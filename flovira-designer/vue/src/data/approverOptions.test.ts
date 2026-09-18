import { expect, test } from 'bun:test'
import { DEMO_CAPABILITIES } from '../../../examples/capabilities'
import { approverOptionError, changeApproverOption, visibleApproverOptions } from './approverOptions'

const strategies = DEMO_CAPABILITIES.approverStrategies
const role = strategies.find(strategy => strategy.code === 'ROLE')!
const options = visibleApproverOptions(role)

test('Vue exposes empty handling for roles and uses backend defaults', () => {
  expect(options.map(option => [option.code, option.defaultValue])).toEqual([
    ['emptyPolicy', 'ERROR'], ['sameAsStarterAction', 'SELF_APPROVE'],
  ])
  expect(visibleApproverOptions(strategies[0]).map(option => option.code)).toEqual(['sameAsStarterAction'])
  expect(approverOptionError({}, options, strategies)).toBeUndefined()
})

test('Vue rejects missing or malformed transfer users and unregistered USER resolver', () => {
  for (const [key, subjectKey] of [['emptyPolicy', 'emptyPolicySubjects'], ['sameAsStarterAction', 'sameAsStarterSubjects']]) {
    const config = { [key]: 'TRANSFER_TO_USER' }
    for (const subjects of [undefined, [], [{ id: 'role', type: 'ROLE' }], [{ id: ' ', type: 'USER' }]]) {
      expect(approverOptionError({ ...config, [subjectKey]: subjects }, options, strategies)).toBeTruthy()
    }
    const valid = { ...config, [subjectKey]: [{ id: 'backup', type: 'USER' }] }
    expect(approverOptionError(valid, options, strategies)).toBeUndefined()
    expect(approverOptionError(valid, options, [role])).toBeTruthy()
  }
  expect(approverOptionError({ emptyPolicy: 'TO_ADMIN' }, options, strategies)).toBeTruthy()
})

test('Vue switching a policy clears only its transfer targets', () => {
  const config = { sameAsStarterAction: 'TRANSFER_TO_USER', sameAsStarterSubjects: [{ id: 'backup', type: 'USER' }],
    emptyPolicy: 'TRANSFER_TO_USER', emptyPolicySubjects: [{ id: 'other', type: 'USER' }], custom: 'preserved' }
  const next = changeApproverOption(config, options[1], 'SELF_APPROVE')
  expect(next.sameAsStarterSubjects).toBeUndefined()
  expect(next.emptyPolicySubjects).toEqual(config.emptyPolicySubjects)
  expect(next.custom).toBe('preserved')
  expect(config.sameAsStarterSubjects).toHaveLength(1)
})

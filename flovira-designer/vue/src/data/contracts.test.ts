import { describe, expect, test } from 'bun:test'
import {
  DEFAULT_DESIGNER_CAPABILITIES,
  filterPaletteNodes,
  unwrapData,
} from './contracts'

describe('designer integration contract', () => {
  test('filters only built-in nodes disabled by the host', () => {
    const capabilities = {
      ...DEFAULT_DESIGNER_CAPABILITIES,
      nodeTypes: ['0', '1', '2', '6'] as typeof DEFAULT_DESIGNER_CAPABILITIES.nodeTypes,
    }
    const result = filterPaletteNodes([
      { type: 'between' },
      { type: 'wait' },
      { type: 'subProcess' },
      { type: 'hostCustomNode' },
    ], capabilities)

    expect(result.map((item) => item.type)).toEqual(['between', 'subProcess', 'hostCustomNode'])
  })

  test('unwraps REST envelopes and direct provider results', () => {
    expect(unwrapData({ data: DEFAULT_DESIGNER_CAPABILITIES })).toBe(DEFAULT_DESIGNER_CAPABILITIES)
    expect(unwrapData(DEFAULT_DESIGNER_CAPABILITIES)).toBe(DEFAULT_DESIGNER_CAPABILITIES)
  })
})

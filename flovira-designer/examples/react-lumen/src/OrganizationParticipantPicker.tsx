import { useMemo, useState } from 'react'
import { Checkbox, Input } from '@luokuiai/lumen-ui'
import type { ApproverEditorRenderContext, ApproverSubject } from '@luokuiai/flovira-react-designer'

const departments = [
  {
    id: 'product',
    name: '产品研发中心',
    users: [
      { id: 'zhangsan', name: '张三', code: 'zhangsan' },
      { id: 'lisi', name: '李四', code: 'lisi' },
    ],
  },
  {
    id: 'finance',
    name: '财务管理部',
    users: [
      { id: 'wangwu', name: '王五', code: 'wangwu' },
      { id: 'zhaoliu', name: '赵六', code: 'zhaoliu' },
    ],
  },
  {
    id: 'operations',
    name: '运营管理部',
    users: [
      { id: 'sunqi', name: '孙七', code: 'sunqi' },
    ],
  },
]

export function OrganizationParticipantPicker({
  selected,
  multiple,
  onChange,
}: ApproverEditorRenderContext) {
  const [departmentId, setDepartmentId] = useState(departments[0].id)
  const [keyword, setKeyword] = useState('')
  const department = departments.find((item) => item.id === departmentId) || departments[0]
  const users = useMemo(() => {
    const normalized = keyword.trim().toLowerCase()
    if (!normalized) return department.users
    return department.users.filter((user) =>
      user.name.toLowerCase().includes(normalized) || user.code.toLowerCase().includes(normalized))
  }, [department, keyword])

  const toggle = (subject: ApproverSubject, checked: boolean) => {
    if (!checked) {
      onChange(selected.filter((item) => item.id !== subject.id))
      return
    }
    onChange(multiple
      ? [...selected.filter((item) => item.id !== subject.id), subject]
      : [subject])
  }

  return (
    <div className="organization-picker">
      <nav className="organization-picker__departments" aria-label="部门">
        <div className="organization-picker__heading">组织架构</div>
        {departments.map((item) => (
          <button
            type="button"
            key={item.id}
            className={item.id === department.id ? 'is-active' : ''}
            onClick={() => setDepartmentId(item.id)}
          >
            <span>{item.name}</span>
            <small>{item.users.length}</small>
          </button>
        ))}
      </nav>
      <div className="organization-picker__people">
        <Input
          value={keyword}
          size="md"
          placeholder="搜索姓名或账号"
          aria-label="搜索人员"
          onChange={(event) => setKeyword(event.target.value)}
        />
        <div className="organization-picker__list">
          {users.map((user) => {
            const checked = selected.some((item) => item.id === user.id)
            return (
              <Checkbox
                key={user.id}
                checked={checked}
                label={(
                  <span className="organization-picker__person">
                    <strong>{user.name}</strong>
                    <small>{user.code}</small>
                  </span>
                )}
                onChange={(nextChecked) => toggle({ id: user.id, type: 'USER', name: user.name }, nextChecked)}
              />
            )
          })}
        </div>
      </div>
    </div>
  )
}

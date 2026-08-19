import type { DataProvider } from './provider'
import { DEFAULT_DESIGNER_CAPABILITIES } from './contracts'

/**
 * 内存 mock 数据源：让设计器脱离后端独立运行 / 联调 / 测试。
 *
 * 仅在显式开启时启用（构建期 VITE_USE_MOCK=true，或运行期 URL 携带 ?mock=true），
 * 默认不启用，因此不影响线上 HTTP 数据源。
 *
 * 返回结构对齐统一能力、资源和关系契约，提供最小可用示例数据，
 * 使办理人选择、监听器、表单等功能在脱后端下可正常体验。
 *
 * @author warm
 */

/** 构造一个成功响应（对齐后端 {code, msg, data} 结构）。 */
function ok(data?: any, msg?: string): Promise<any> {
  return Promise.resolve({ code: 200, msg: msg || 'ok(mock)', data: data })
}

// 办理人示例数据（按 角色 / 部门 / 用户 三类 tab；对齐 HandlerAuth 结构）
const HANDLER_ROWS: Record<string, any[]> = {
  角色: [
    { storageId: 'role:1', handlerCode: 'admin', handlerName: '系统管理员', groupName: '角色', createTime: '2024-01-01 10:00:00' },
    { storageId: 'role:2', handlerCode: 'leader', handlerName: '部门主管', groupName: '角色', createTime: '2024-01-02 10:00:00' },
    { storageId: 'role:3', handlerCode: 'finance', handlerName: '财务专员', groupName: '角色', createTime: '2024-01-03 10:00:00' },
    { storageId: 'role:4', handlerCode: 'hr', handlerName: '人事专员', groupName: '角色', createTime: '2024-01-04 10:00:00' }
  ],
  部门: [
    { storageId: 'dept:1', handlerCode: 'tech', handlerName: '研发部', groupName: '部门', createTime: '2024-01-01 10:00:00' },
    { storageId: 'dept:2', handlerCode: 'hr', handlerName: '人事部', groupName: '部门', createTime: '2024-01-02 10:00:00' },
    { storageId: 'dept:3', handlerCode: 'finance', handlerName: '财务部', groupName: '部门', createTime: '2024-01-03 10:00:00' },
    { storageId: 'dept:4', handlerCode: 'market', handlerName: '市场部', groupName: '部门', createTime: '2024-01-04 10:00:00' }
  ],
  用户: [
    { storageId: 'user:1', handlerCode: 'zhangsan', handlerName: '张三', groupName: '用户', createTime: '2024-01-01 10:00:00' },
    { storageId: 'user:2', handlerCode: 'lisi', handlerName: '李四', groupName: '用户', createTime: '2024-01-02 10:00:00' },
    { storageId: 'user:3', handlerCode: 'wangwu', handlerName: '王五', groupName: '用户', createTime: '2024-01-03 10:00:00' },
    { storageId: 'user:4', handlerCode: 'zhaoliu', handlerName: '赵六', groupName: '用户', createTime: '2024-01-04 10:00:00' },
    { storageId: 'user:5', handlerCode: 'qianqi', handlerName: '钱七', groupName: '用户', createTime: '2024-01-05 10:00:00' },
    { storageId: 'user:6', handlerCode: 'sunba', handlerName: '孙八', groupName: '用户', createTime: '2024-01-06 10:00:00' }
  ]
}

// 左侧组织架构树（对齐 Tree 结构，配合办理人列表；两级部门，体验更真实）
const HANDLER_TREE: any[] = [
  {
    id: '1', name: '总公司', parentId: '0', children: [
      {
        id: '2', name: '研发中心', parentId: '1', children: [
          { id: '21', name: '前端组', parentId: '2', children: [] },
          { id: '22', name: '后端组', parentId: '2', children: [] }
        ]
      },
      {
        id: '3', name: '职能中心', parentId: '1', children: [
          { id: '31', name: '人事部', parentId: '3', children: [] },
          { id: '32', name: '财务部', parentId: '3', children: [] }
        ]
      }
    ]
  }
]

// 流程类别（baseInfo「流程类别」tree-select，结构 { id, name, children }）
const CATEGORY_LIST: any[] = [
  {
    id: 'oa', name: 'OA办公', children: [
      { id: 'oa-leave', name: '请假流程', children: [] },
      { id: 'oa-reimburse', name: '报销流程', children: [] }
    ]
  },
  {
    id: 'hr', name: '人事流程', children: [
      { id: 'hr-entry', name: '入职流程', children: [] },
      { id: 'hr-quit', name: '离职流程', children: [] }
    ]
  },
  { id: 'finance', name: '财务流程', children: [] }
]

// 已发布表单唯一标识（baseInfo 自定义表单=是 时的 tree-select，结构 { id, name, children }）
const FORM_PATH_LIST: any[] = [
  { id: 'form-leave', name: '请假申请表', children: [] },
  { id: 'form-reimburse', name: '报销申请表', children: [] },
  { id: 'form-purchase', name: '采购审批表', children: [] }
]

// 节点扩展属性（between 节点「扩展属性」面板，分组 type:1→平铺 / type:2→标签页；childs 覆盖全部控件类型 1-7）
const NODE_EXT: any[] = [
  {
    type: 1, name: '扩展属性', code: 'baseExt', childs: [
      { label: '业务单号', code: 'bizNo', type: 1, must: false, desc: '请输入业务单号' },
      { label: '备注说明', code: 'remark', type: 2, must: false, desc: '请输入备注（多行）' },
      {
        label: '优先级', code: 'priority', type: 3, must: false, desc: '请选择优先级', dict: [
          { label: '低', value: '1' }, { label: '中', value: '2', selected: true }, { label: '高', value: '3' }
        ]
      },
      {
        label: '关注标签', code: 'tags', type: 3, multiple: true, must: false, desc: '可多选', dict: [
          { label: '紧急', value: 'urgent' }, { label: '重要', value: 'important' }, { label: '常规', value: 'normal' }
        ]
      },
      {
        label: '是否抄送', code: 'cc', type: 4, must: false, desc: '请选择', dict: [
          { label: '是', value: '1', selected: true }, { label: '否', value: '0' }
        ]
      },
      {
        label: '通知方式', code: 'notify', type: 4, multiple: true, must: false, desc: '可多选', dict: [
          { label: '站内信', value: 'site' }, { label: '邮件', value: 'email' }, { label: '短信', value: 'sms' }
        ]
      },
      { label: '抄送人', code: 'ccUsers', type: 5, must: false, desc: '选择抄送人' },
      { label: '限时(小时)', code: 'limitHours', type: 6, must: false, desc: '审批限时', precision: 0, step: 1, min: 0 },
      { label: '截止时间', code: 'deadline', type: 7, must: false, desc: '请选择截止时间', dateType: 'datetime', dateFormat: 'YYYY-MM-DD HH:mm:ss' }
    ]
  },
  {
    type: 2, name: '高级设置', code: 'advanced', childs: [
      {
        label: '允许加签', code: 'allowAddSign', type: 4, must: false, desc: '是否允许加签', dict: [
          { label: '允许', value: '1', selected: true }, { label: '禁止', value: '0' }
        ]
      },
      {
        label: '允许转办', code: 'allowTransfer', type: 4, must: false, desc: '是否允许转办', dict: [
          { label: '允许', value: '1', selected: true }, { label: '禁止', value: '0' }
        ]
      },
      { label: '审批意见模板', code: 'commentTpl', type: 2, must: false, desc: '默认审批意见' }
    ]
  }
]

// 全量办理人（用于名称回显匹配）
const ALL_HANDLERS: any[] = [].concat(HANDLER_ROWS.角色, HANDLER_ROWS.部门, HANDLER_ROWS.用户)

function flattenTreeResources(nodes: any[], resourceType: string): any[] {
  return nodes.flatMap((node) => [
    { id: String(node.id), name: node.name, parentId: node.parentId ? String(node.parentId) : undefined, resourceType },
    ...flattenTreeResources(node.children || [], resourceType)
  ])
}

const LISTENER_RESOURCES = [
  { id: 'com.example.flow.StartListener', code: 'start', name: '开始监听器(mock)', resourceType: 'LISTENER', metadata: { type: 'start', path: 'com.example.flow.StartListener' } },
  { id: 'com.example.flow.AssignmentListener', code: 'assignment', name: '指派监听器(mock)', resourceType: 'LISTENER', metadata: { type: 'assignment', path: 'com.example.flow.AssignmentListener' } },
  { id: 'com.example.flow.FinishListener', code: 'finish', name: '完成监听器(mock)', resourceType: 'LISTENER', metadata: { type: 'finish', path: 'com.example.flow.FinishListener' } }
]

export function createMockProvider(): DataProvider {
  return {
    capabilities() {
      return ok(DEFAULT_DESIGNER_CAPABILITIES)
    },
    queryResources(query) {
      const handlerTypes: Record<string, string> = { USER: '用户', ROLE: '角色' }
      let rows: any[] = []
      if (handlerTypes[query.resourceType]) {
        rows = HANDLER_ROWS[handlerTypes[query.resourceType]].map((item) => ({
          id: item.storageId,
          code: item.handlerCode,
          name: item.handlerName,
          resourceType: query.resourceType,
          metadata: { groupName: item.groupName, createTime: item.createTime }
        }))
      } else if (query.resourceType === 'ORGANIZATION') {
        rows = flattenTreeResources(HANDLER_TREE, 'ORGANIZATION')
      } else if (query.resourceType === 'SUBJECT') {
        const ids = Array.isArray(query.parameters?.ids) ? query.parameters.ids : []
        rows = ALL_HANDLERS.filter((item) => ids.includes(item.storageId)).map((item) => ({
          id: item.storageId, code: item.handlerCode, name: item.handlerName, resourceType: 'SUBJECT'
        }))
      } else if (query.resourceType === 'CATEGORY') {
        rows = flattenTreeResources(CATEGORY_LIST, 'CATEGORY')
      } else if (query.resourceType === 'FORM_PATH') {
        rows = flattenTreeResources(FORM_PATH_LIST, 'FORM_PATH')
      } else if (query.resourceType === 'SUBPROCESS') {
        rows = [
          { id: '2001', code: 'expense-review', name: '报销审批', resourceType: 'SUBPROCESS', metadata: { version: '1.0.0' } },
          { id: '2002', code: 'purchase-review', name: '采购审批', resourceType: 'SUBPROCESS', metadata: { version: '1.0.0' } }
        ]
      } else if (query.resourceType === 'NODE_EXTENSION') {
        rows = NODE_EXT.map((item) => ({ id: item.code, code: item.code, name: item.name, resourceType: 'NODE_EXTENSION', metadata: item }))
      } else if (query.resourceType === 'LISTENER') {
        rows = LISTENER_RESOURCES
      }
      if (query.keyword) {
        const keyword = query.keyword.toLowerCase()
        rows = rows.filter((item) => `${item.name} ${item.code || ''}`.toLowerCase().includes(keyword))
      }
      const pageNum = query.pageNum || 1
      const pageSize = query.pageSize || 20
      const start = (pageNum - 1) * pageSize
      return ok({ items: rows.slice(start, start + pageSize), total: rows.length })
    },
    resolveRelationship() {
      return ok([])
    },
    // ===== 流程定义 =====
    saveJson() {
      return ok(true, '保存成功(mock)')
    },
    queryDef() {
      return ok({})
    },
    queryFlowChart() {
      return ok({})
    },
    subprocessSummary() {
      return ok(null)
    },
    subprocessChildren() {
      return ok({ list: [], total: 0, pageNum: 1, pageSize: 20 })
    },
    subprocessEvents() {
      return ok([])
    },
    subprocessHistory() {
      return ok([])
    },

    // ===== 配置 =====
    // 设计器启动即调用，返回安全默认（无 token、默认框架 springboot），保证 mock 下可正常初始化
    config() {
      return ok({ tokenNameList: [], framework: 'springboot' })
    }
  }
}

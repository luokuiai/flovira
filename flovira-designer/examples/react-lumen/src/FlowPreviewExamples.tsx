import { useState } from 'react'
import { FlowPreview, type FloviraDefinition, type FloviraNode, type FloviraNodeType } from '@luokuiai/flovira-react-designer'
import { lumenDesignerUi } from '@luokuiai/flovira-react-adapter-lumen'
import './preview-examples.css'

const node = (nodeCode: string, nodeName: string, nodeType: FloviraNodeType, targets: string[] = []): FloviraNode => ({
  nodeCode,
  nodeName,
  nodeType,
  skipList: targets.map((targetNodeCode) => ({ sourceNodeCode: nodeCode, targetNodeCode, skipType: 'PASS' })),
})

// 仅用于演示；运行数据由场景明确给出，不从节点位置推断。
const definition: FloviraDefinition = {
  flowCode: 'purchase_preview',
  flowName: '采购申请流程',
  nodeList: [
    node('start', '提交采购申请', '0', ['amount']),
    node('amount', '按采购金额与紧急程度分流', '3', ['department', 'director', 'urgent', 'other']),
    node('department', '按申请部门分流', '3', ['technology', 'operations', 'otherDepartment']),
    node('technology', '研发类采购负责人审批', '1', ['parallel']),
    node('operations', '运营类采购负责人审批', '1', ['parallel']),
    node('otherDepartment', '其他部门采购审批', '1', ['parallel']),
    node('urgent', '紧急采购确认', '1', ['purchasing']),
    node('purchasing', '采购负责人复核', '1', ['parallel']),
    node('director', '大额采购总监审批', '1', ['compliance']),
    node('compliance', '大额采购合规复核', '1', ['parallel']),
    node('other', '其他采购审批', '1', ['parallel']),
    node('parallel', '财务与合同并行审核', '4', ['finance', 'contract']),
    node('finance', '财务预算审核', '1', ['inclusive']),
    node('contract', '合同审查子流程', '6', ['inclusive']),
    node('inclusive', '按需通知与等待', '5', ['copy', 'wait']),
    node('copy', '抄送采购负责人', '8', ['end']),
    node('wait', '等待供应商确认', '7', ['end']),
    node('end', '采购申请完成', '2'),
  ],
}

// 条件名称用于演示分支含义；最后一项是其他条件的兜底分支。
const branchNames: Record<string, string[]> = {
  amount: ['普通采购 · 1–5 万元', '普通采购 · 5 万元及以上', '紧急采购', '其他条件'],
  department: ['研发部门', '运营部门', '其他条件'],
}
definition.nodeList.forEach((item) => {
  item.skipList.forEach((skip, index) => {
    if (branchNames[item.nodeCode]) skip.skipName = branchNames[item.nodeCode][index]
  })
})

const reviewed = ['start', 'amount', 'other', 'parallel']
const audited = [...reviewed, 'finance', 'contract', 'inclusive']
const stages = [
  { name: '其他条件', current: ['other'], completed: ['start', 'amount'] },
  { name: '并行审核', current: ['finance', 'contract'], completed: reviewed },
  { name: '等待回执', current: ['wait'], completed: [...audited, 'copy'] },
  { name: '流程完成', current: [], completed: [...audited, 'copy', 'wait', 'end'] },
  { name: '流程全貌', current: [], completed: undefined },
  { name: '部门条件分支', current: ['technology'], completed: ['start', 'amount', 'department'] },
  { name: '大额采购分支', current: ['compliance'], completed: ['start', 'amount', 'director'] },
  { name: '紧急采购分支', current: ['purchasing'], completed: ['start', 'amount', 'urgent'] },
]

const handlers = {
  start: ['王小明'],
  technology: ['周主管'],
  operations: ['郑主管'],
  otherDepartment: ['周主管'],
  director: ['王总监'],
  compliance: ['孙合规'],
  other: ['张经理'],
  urgent: ['陈经理'],
  purchasing: ['刘采购'],
  finance: ['李会计', '陈主管'],
  copy: ['刘采购'],
}

export function FlowPreviewExamples() {
  const [stageIndex, setStageIndex] = useState(5)
  const stage = stages[stageIndex]
  return (
    <section className="preview-examples" aria-label="流程预览示例">
      <header className="preview-examples-heading">
        <div>
          <p className="preview-examples-eyebrow">采购申请 · 示例</p>
          <h2>办公设备采购</h2>
        </div>
        <span className="preview-examples-status">{stageIndex === 3 ? '已完成' : stageIndex === 4 ? '流程定义' : '审批中'}</span>
      </header>
      <div className="preview-examples-section-heading">
        <h3>流程进度</h3>
        <select aria-label="切换预览场景" value={stageIndex} onChange={(event) => setStageIndex(Number(event.target.value))}>
          {stages.map((item, index) => <option key={item.name} value={index}>{item.name}</option>)}
        </select>
      </div>
      <FlowPreview
        value={definition}
        height="100%"
        currentNodeCodes={stage.current}
        completedNodeCodes={stage.completed}
        nodeHandlers={handlers}
        ui={lumenDesignerUi}
      />
    </section>
  )
}

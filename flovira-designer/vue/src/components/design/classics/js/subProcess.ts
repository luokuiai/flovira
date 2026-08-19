import { RectNode, RectNodeModel } from '@logicflow/core'
import { applyClassicDesignColor, setCommonStyle } from '@/components/design/common/js/tool'

class SubProcessModel extends RectNodeModel {
  initNodeData(data) {
    super.initNodeData(data)
    this.width = 120
    this.height = 80
    this.radius = 8
  }

  getNodeStyle() {
    const style = applyClassicDesignColor(
      setCommonStyle(super.getNodeStyle(), this.properties, 'node', undefined),
      this.properties,
      '146,84,222',
    )
    if (typeof window !== 'undefined' && window.__WF_FLOW_DESIGN_MODE__) {
      style.fill = 'rgba(146,84,222,0.86)'
    }
    return style
  }

  getTextStyle() {
    const style = super.getTextStyle()
    if (typeof window !== 'undefined' && window.__WF_FLOW_DESIGN_MODE__) {
      style.color = '#fff'
      style.fill = '#fff'
    }
    return style
  }
}

export default {
  type: 'subProcess',
  model: SubProcessModel,
  view: RectNode,
}

import { BaseNodeModel } from '@/components/design/mimic/js/baseNodeModel'
import { BaseNodeView } from '@/components/design/mimic/js/baseNodeView'

class WaitModel extends BaseNodeModel {}

class WaitView extends BaseNodeView {}

export default {
  type: 'wait',
  model: WaitModel,
  view: WaitView,
}

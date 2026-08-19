import { BaseNodeModel } from '@/components/design/mimic/js/baseNodeModel'
import { BaseNodeView } from '@/components/design/mimic/js/baseNodeView'

class SubProcessModel extends BaseNodeModel {}

class SubProcessView extends BaseNodeView {}

export default {
  type: 'subProcess',
  model: SubProcessModel,
  view: SubProcessView,
}

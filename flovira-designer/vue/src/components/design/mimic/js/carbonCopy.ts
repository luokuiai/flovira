import { BaseNodeModel } from '@/components/design/mimic/js/baseNodeModel'
import { BaseNodeView } from '@/components/design/mimic/js/baseNodeView'

class CarbonCopyModel extends BaseNodeModel {}

class CarbonCopyView extends BaseNodeView {}

export default {
  type: 'carbonCopy',
  model: CarbonCopyModel,
  view: CarbonCopyView,
}

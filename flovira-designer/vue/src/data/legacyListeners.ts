/** 拒绝已移除的持久化监听配置。 */
export function validateLegacyListeners(definition: { listenerType?: string; listenerPath?: string; nodeList?: any[] }) {
  const validate = (item: { listenerType?: string; listenerPath?: string }) => {
    if (item.listenerType || item.listenerPath) {
      throw new Error('旧监听配置已移除，请清空 listenerType 和 listenerPath')
    }
  }
  validate(definition)
  for (const node of definition.nodeList || []) validate(node)
}

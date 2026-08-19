export type DesignerNodeType = '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7'

export interface DesignerCapabilities {
  schemaVersion: 1
  nodeTypes: DesignerNodeType[]
  approverStrategies: string[]
  approvalModes: string[]
  returnPolicies: string[]
  timeoutNodeTypes: DesignerNodeType[]
  operations: string[]
  resourceTypes: string[]
}

export interface DesignerResourceQuery {
  resourceType: string
  keyword?: string
  scopeId?: string
  pageNum?: number
  pageSize?: number
  parameters?: Record<string, unknown>
}

export interface DesignerResourceItem {
  id: string
  code?: string
  name: string
  parentId?: string
  resourceType: string
  disabled?: boolean
  metadata?: Record<string, unknown>
}

export interface DesignerResourcePage {
  items: DesignerResourceItem[]
  total: number
}

export interface DesignerRelationshipQuery {
  relationType: string
  subjectId?: string
  organizationId?: string
  context?: Record<string, unknown>
}

export interface DesignerSubject {
  id: string
  type: string
  name?: string
  metadata?: Record<string, unknown>
}

export type ApiResponse<T> = T | { data?: T }

export const DEFAULT_DESIGNER_CAPABILITIES: DesignerCapabilities = {
  schemaVersion: 1,
  nodeTypes: ['0', '1', '2', '3', '4', '5', '6', '7'],
  approverStrategies: ['USER', 'ROLE', 'ORGANIZATION', 'EXPRESSION'],
  approvalModes: ['OR', 'VOTE', 'COUNTERSIGN'],
  returnPolicies: ['PREVIOUS', 'ANY', 'REJECT'],
  timeoutNodeTypes: ['1', '7'],
  operations: ['SAVE', 'PUBLISH', 'VALIDATE', 'IMPORT', 'EXPORT'],
  resourceTypes: [
    'USER', 'ROLE', 'ORGANIZATION', 'SUBJECT', 'CATEGORY', 'FORM_PATH',
    'FORM_FIELD', 'DICTIONARY', 'SUBPROCESS', 'NODE_EXTENSION', 'LISTENER',
  ],
}

export function unwrapData<T>(response: ApiResponse<T>): T | undefined {
  if (response && typeof response === 'object' && !Array.isArray(response) && 'data' in response) {
    return (response as { data?: T }).data
  }
  return response as T
}

const PALETTE_NODE_TYPES: Record<string, DesignerNodeType> = {
  start: '0',
  between: '1',
  end: '2',
  serial: '3',
  parallel: '4',
  inclusive: '5',
  subProcess: '6',
  wait: '7',
}

export function filterPaletteNodes<T extends { type: string }>(
  nodes: T[],
  capabilities: DesignerCapabilities,
): T[] {
  const available = new Set(capabilities.nodeTypes)
  return nodes.filter((node) => {
    const nodeType = PALETTE_NODE_TYPES[node.type]
    return !nodeType || available.has(nodeType)
  })
}

export interface DesignerResourceTreeNode {
  id: string
  name: string
  parentId?: string
  children: DesignerResourceTreeNode[]
}

export function resourcesToTree(items: DesignerResourceItem[]): DesignerResourceTreeNode[] {
  const nodes = new Map<string, DesignerResourceTreeNode>()
  items.forEach((item) => nodes.set(item.id, {
    id: item.id,
    name: item.name,
    parentId: item.parentId,
    children: [],
  }))
  const roots: DesignerResourceTreeNode[] = []
  nodes.forEach((node) => {
    const parent = node.parentId ? nodes.get(node.parentId) : undefined
    if (parent) parent.children.push(node)
    else roots.push(node)
  })
  return roots
}

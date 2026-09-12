import { useEffect, useMemo, useState } from 'react'
import {
  ReactFlowDesigner,
  createInitialDefinition,
  type DesignerCapabilities,
  type DesignerResourceLoader,
  type DesignerUiAdapter,
  type FloviraDefinition,
} from '@luokuiai/flovira-react-designer'
import {
  exampleApi,
  type DefinitionSummary,
  type DemoIdentity,
  type InstanceProgress,
  type PurchaseRequest,
  type TaskSummary,
} from '@flovira-example/common'

export interface ExampleAppProps {
  title: string
  ui: DesignerUiAdapter
}

const freshDefinition = (): FloviraDefinition => {
  const definition = createInitialDefinition()
  definition.flowCode = `purchase_${Date.now()}`
  definition.flowName = 'Purchase approval'
  definition.formId = 'purchase-request-v1'
  definition.nodeList[1].nodeName = 'Manager approval'
  definition.nodeList[1].permissionFlag = 'manager'
  definition.nodeList[1].skipList.push({
    id: `reject_${Date.now()}`,
    sourceNodeCode: definition.nodeList[1].nodeCode,
    targetNodeCode: definition.nodeList[definition.nodeList.length - 1].nodeCode,
    skipName: 'Reject request',
    skipType: 'REJECT',
  })
  return definition
}

export function ExampleApp({ title, ui }: ExampleAppProps) {
  const [identities, setIdentities] = useState<DemoIdentity[]>([])
  const [user, setUser] = useState('alice')
  const [definitions, setDefinitions] = useState<DefinitionSummary[]>([])
  const [purchases, setPurchases] = useState<PurchaseRequest[]>([])
  const [tasks, setTasks] = useState<TaskSummary[]>([])
  const [definition, setDefinition] = useState<FloviraDefinition>(freshDefinition)
  const [definitionKey, setDefinitionKey] = useState(0)
  const [selectedId, setSelectedId] = useState<number>()
  const [instanceId, setInstanceId] = useState<number>()
  const [progress, setProgress] = useState<InstanceProgress>()
  const [capabilities, setCapabilities] = useState<DesignerCapabilities>()
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const run = async (operation: () => Promise<void>) => {
    setError('')
    setNotice('')
    try {
      await operation()
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : String(cause))
    }
  }

  const refresh = async () => {
    const [nextDefinitions, nextPurchases, nextTasks] = await Promise.all([
      exampleApi.definitions(), exampleApi.purchases(), exampleApi.tasks(user),
    ])
    setDefinitions(nextDefinitions)
    setPurchases(nextPurchases)
    setTasks(nextTasks)
  }

  useEffect(() => {
    run(async () => {
      const [nextIdentities, nextCapabilities] = await Promise.all([
        exampleApi.identities(), exampleApi.capabilities(),
      ])
      setIdentities(nextIdentities)
      setCapabilities(nextCapabilities as unknown as DesignerCapabilities)
      await refresh()
    })
  }, [user])

  const queryResources = useMemo<DesignerResourceLoader>(() => async (query) =>
    exampleApi.resources(query), [])

  const loadDefinition = (id: number) => run(async () => {
    const loaded = await exampleApi.definition(id) as unknown as FloviraDefinition
    setDefinition(loaded)
    setSelectedId(id)
    setDefinitionKey((value) => value + 1)
    setNotice(`Loaded definition ${id}`)
  })

  const publish = () => selectedId && run(async () => {
    await exampleApi.publishDefinition(selectedId, user)
    await refresh()
    setNotice(`Published definition ${selectedId}`)
  })

  const start = () => selectedId && purchases[0] && run(async () => {
    const instance = await exampleApi.start(purchases[0].id, selectedId, user)
    const id = Number(instance.id)
    setInstanceId(id)
    await refresh()
    setProgress(await exampleApi.progress(id, user))
    setNotice(`Started process ${id}`)
  })

  const handle = (task: TaskSummary, action: 'approve' | 'reject') => run(async () => {
    const instance = await exampleApi.handle(task.id, action, `${action} from ${user}`, user)
    const id = Number(instance.id || task.instanceId)
    setInstanceId(id)
    await refresh()
    setProgress(await exampleApi.progress(id, user))
    setNotice(`${action === 'approve' ? 'Approved' : 'Rejected'} task ${task.id}`)
  })

  return (
    <main className="example-shell">
      <header className="example-header">
        <div><span className="eyebrow">Composable Flovira example</span><h1>{title}</h1></div>
        <label>Demo identity (not authentication)
          <select value={user} onChange={(event) => setUser(event.target.value)}>
            {identities.map((identity) => <option key={identity.id} value={identity.id}>{identity.name}</option>)}
          </select>
        </label>
      </header>
      {error && <p className="message error" role="alert">{error}</p>}
      {notice && <p className="message success" role="status">{notice}</p>}

      <section className="card toolbar">
        <label>Definition
          <select value={selectedId ?? ''} onChange={(event) => loadDefinition(Number(event.target.value))}>
            <option value="">Choose a definition</option>
            {definitions.map((item) => <option key={item.id} value={item.id}>{item.flowName} · v{item.version}</option>)}
          </select>
        </label>
        <button type="button" onClick={() => { setDefinition(freshDefinition()); setSelectedId(undefined); setDefinitionKey((v) => v + 1) }}>New</button>
        <button type="button" disabled={!selectedId} onClick={publish}>Publish</button>
        <button type="button" disabled={!selectedId || !purchases[0]} onClick={start}>Start sample request</button>
        <button type="button" onClick={() => run(refresh)}>Refresh</button>
      </section>

      <section className="card designer-card">
        <ReactFlowDesigner
          key={definitionKey}
          defaultValue={definition}
          ui={ui}
          capabilities={capabilities}
          queryResources={queryResources}
          queryConditionFields={async () => [
            { code: 'amount', label: 'Purchase amount', type: 'NUMBER' },
            { code: 'department', label: 'Department', type: 'STRING' },
          ]}
          onSave={async (value) => {
            await run(async () => {
              const saved = await exampleApi.saveDefinition(value, user)
              setSelectedId(saved.id)
              await refresh()
              setNotice(`Saved definition ${saved.id}`)
            })
          }}
        />
      </section>

      <div className="example-grid">
        <section className="card"><h2>Host-owned purchase request</h2>
          {purchases.map((purchase) => <dl key={purchase.id}>
            <dt>{purchase.title}</dt><dd>{purchase.department} · {purchase.amount} · {purchase.status}</dd>
          </dl>)}
        </section>
        <section className="card"><h2>My tasks</h2>
          {tasks.length === 0 && <p>No pending tasks for {user}.</p>}
          {tasks.map((task) => <div className="task" key={task.id}>
            <span>{task.nodeName} · process {task.instanceId}</span>
            <button type="button" onClick={() => handle(task, 'approve')}>Approve</button>
            <button type="button" onClick={() => handle(task, 'reject')}>Reject</button>
          </div>)}
        </section>
      </div>

      <section className="card"><h2>Process progress {instanceId ? `#${instanceId}` : ''}</h2>
        {instanceId && <button type="button" onClick={() => run(async () => setProgress(await exampleApi.progress(instanceId, user)))}>Refresh progress</button>}
        <pre>{progress ? JSON.stringify(progress, null, 2) : 'Start or handle a process to inspect database-backed progress.'}</pre>
      </section>
    </main>
  )
}

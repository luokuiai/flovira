import { describe, expect, test } from 'bun:test'
import { API_PREFIX, DESIGNER_PREFIX, DEMO_USER_HEADER, backendTarget, exampleApi } from './index'

describe('shared frontend contract', () => {
  test('submits the complete design without a partial-save header', async () => {
    const originalFetch = globalThis.fetch
    const definition = { id: 1, flowCode: 'purchase', businessType: 'PURCHASE', nodeList: [] }
    let captured: RequestInit | undefined
    globalThis.fetch = (async (_url: unknown, init: RequestInit) => {
      captured = init
      return new Response(JSON.stringify({ code: 200, data: null }))
    }) as typeof fetch
    try {
      await exampleApi.designerSave(definition, 'bob')
      expect(captured?.body).toBe(JSON.stringify(definition))
      expect(new Headers(captured?.headers).get(DEMO_USER_HEADER)).toBe('bob')
      expect(Array.from(new Headers(captured?.headers).keys()).sort()).toEqual(
        ['accept', 'content-type', DEMO_USER_HEADER.toLowerCase()].sort())
    } finally { globalThis.fetch = originalFetch }
  })
  test('uses stable prefixes', () => {
    expect(API_PREFIX).toBe('/api/example/v1')
    expect(DESIGNER_PREFIX).toBe('/flovira')
  })

  test('selects either backend without source changes', () => {
    expect(backendTarget({})).toBe('http://localhost:8081')
    expect(backendTarget({ EXAMPLE_BACKEND_URL: 'http://localhost:8082' })).toBe('http://localhost:8082')
  })
})

import { describe, expect, test } from 'bun:test'
import { API_PREFIX, DESIGNER_PREFIX, backendTarget } from './index'

describe('shared frontend contract', () => {
  test('uses stable prefixes', () => {
    expect(API_PREFIX).toBe('/api/example/v1')
    expect(DESIGNER_PREFIX).toBe('/flovira')
  })

  test('selects either backend without source changes', () => {
    expect(backendTarget({})).toBe('http://localhost:8081')
    expect(backendTarget({ EXAMPLE_BACKEND_URL: 'http://localhost:8082' })).toBe('http://localhost:8082')
  })
})

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { apiFetch } from './api'

describe('apiFetch', () => {
  beforeEach(() => {
    document.cookie = 'XSRF-TOKEN=test-token-123'
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true }))
  })

  it('adds the X-XSRF-TOKEN header from the cookie on POST', async () => {
    await apiFetch('/api/briefings/trigger', { method: 'POST' })
    const [, init] = (fetch as ReturnType<typeof vi.fn>).mock.calls[0]
    expect(new Headers(init.headers).get('X-XSRF-TOKEN')).toBe('test-token-123')
  })

  it('does not add the header on GET', async () => {
    await apiFetch('/api/news', { method: 'GET' })
    const [, init] = (fetch as ReturnType<typeof vi.fn>).mock.calls[0]
    expect(new Headers(init.headers).get('X-XSRF-TOKEN')).toBeNull()
  })
})

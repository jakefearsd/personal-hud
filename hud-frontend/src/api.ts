const MUTATING = new Set(['POST', 'PUT', 'DELETE', 'PATCH'])

function readCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp('(^|;\\s*)' + name + '=([^;]*)'))
  return match ? decodeURIComponent(match[2]) : null
}

/**
 * fetch wrapper that attaches the CSRF token header on mutating requests.
 * Use for every POST/PUT/DELETE call to the HUD API.
 */
export function apiFetch(url: string, init: RequestInit = {}): Promise<Response> {
  const method = (init.method || 'GET').toUpperCase()
  const headers = new Headers(init.headers)
  if (MUTATING.has(method)) {
    const token = readCookie('XSRF-TOKEN')
    if (token) headers.set('X-XSRF-TOKEN', token)
  }
  return fetch(url, { ...init, headers })
}

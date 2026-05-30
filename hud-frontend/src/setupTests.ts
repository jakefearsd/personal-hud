import '@testing-library/jest-dom'
import { beforeAll, afterEach, afterAll, vi } from 'vitest'
import { server } from './mocks/server'
import { resetAuth } from './mocks/handlers'

beforeAll(() => server.listen())
afterEach(() => {
  server.resetHandlers()
  resetAuth()
})
afterAll(() => server.close())

// Mock ResizeObserver which is needed by Radix UI components used in shadcn
class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
}
window.ResizeObserver = ResizeObserver

// Support Radix UI Select and other pointer-based components
HTMLElement.prototype.hasPointerCapture = vi.fn()
HTMLElement.prototype.setPointerCapture = vi.fn()
HTMLElement.prototype.releasePointerCapture = vi.fn()
HTMLElement.prototype.scrollIntoView = vi.fn()

window.scrollTo = vi.fn()
window.alert = vi.fn()

// Mock canvas for Recharts/Charts
HTMLCanvasElement.prototype.getContext = vi.fn()



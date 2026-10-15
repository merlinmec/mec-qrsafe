import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useServerWake, WAKE_NOTICE_AFTER_MS } from './useServerWake'

let up: boolean
let healthCalls: number

beforeEach(() => {
  vi.useFakeTimers()
  up = false
  healthCalls = 0
  vi.stubGlobal(
    'fetch',
    vi.fn(async () => {
      healthCalls++
      // dormindo, o Render não responde com o JSON da aplicação
      if (!up) throw new TypeError('Failed to fetch')
      return new Response(JSON.stringify({ status: 'ok' }), { status: 200, headers: { 'Content-Type': 'application/json' } })
    }),
  )
})

afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllGlobals()
})

const advance = (ms: number) => act(async () => { await vi.advanceTimersByTimeAsync(ms) })

describe('useServerWake', () => {
  it('servidor acordado: fica pronto sem nunca avisar', async () => {
    up = true
    const { result } = renderHook(() => useServerWake())
    await advance(0)
    expect(result.current).toEqual({ ready: true, wakingSeconds: null })
    await advance(WAKE_NOTICE_AFTER_MS + 1000)
    expect(result.current.wakingSeconds).toBeNull()
  })

  it('servidor dormindo: avisa com o tempo, tenta de novo e libera quando ele responde', async () => {
    const { result } = renderHook(() => useServerWake())
    await advance(WAKE_NOTICE_AFTER_MS - 100)
    expect(result.current).toEqual({ ready: false, wakingSeconds: null })

    await advance(10_000)
    expect(result.current.ready).toBe(false)
    expect(result.current.wakingSeconds).toBeGreaterThanOrEqual(10)
    expect(healthCalls).toBeGreaterThan(2)

    up = true
    await advance(2_500)
    expect(result.current).toEqual({ ready: true, wakingSeconds: null })

    const callsWhenReady = healthCalls
    await advance(30_000)
    expect(healthCalls).toBe(callsWhenReady)
  })

  it('responder 200 com outra coisa (a página de espera do Render) não conta como acordado', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => new Response('<html>waking up</html>', { status: 200 })))
    const { result } = renderHook(() => useServerWake())
    await advance(5_000)
    expect(result.current.ready).toBe(false)
  })
})

import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { PreviewResponse, ValidationResponse } from '../api/types'
import { initialDesign, type QrDesign } from '../design/state'
import { DEBOUNCE_MS, useScannabilityValidation } from './useScannabilityValidation'

const validation: ValidationResponse = { score: 1, passed: true, violations: [], errorCorrectionLevel: 'M', version: 1 }
const preview: PreviewResponse = {
  validation,
  matrix: { size: 1, quietZone: 0, rows: ['D'] },
  logoBox: null,
  engineering: {
    mask: 0,
    symbolSize: 1,
    blockCount: 1,
    correctableErrorsPerBlock: 1,
    destroyedPerBlock: [0],
    codewordBlocks: [0],
    destroyedCodewords: [],
    codewordRows: [[-1]],
  },
}

let calls: { path: string; signal: AbortSignal }[]

beforeEach(() => {
  vi.useFakeTimers()
  calls = []
  vi.stubGlobal(
    'fetch',
    vi.fn(async (url: string, init: RequestInit) => {
      calls.push({ path: new URL(url, 'http://x').pathname, signal: init.signal! })
      const body = url.endsWith('/preview') ? preview : validation
      return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })
    }),
  )
})

afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllGlobals()
})

async function settle() {
  await act(async () => {
    await vi.advanceTimersByTimeAsync(DEBOUNCE_MS + 10)
  })
}

describe('useScannabilityValidation', () => {
  it('primeira carga pede /preview e não pede /validate de sobra depois', async () => {
    const { result } = renderHook(({ design }) => useScannabilityValidation(design), { initialProps: { design: initialDesign } })
    await settle()
    await settle()

    expect(calls.map((c) => c.path)).toEqual(['/api/v1/qr-codes/preview'])
    expect(result.current.status).toBe('ready')
    expect(result.current.stale).toBe(false)
  })

  it('mudar só a cor pede /validate; mudar o conteúdo pede /preview', async () => {
    const { rerender } = renderHook(({ design }) => useScannabilityValidation(design), { initialProps: { design: initialDesign } })
    await settle()

    rerender({ design: { ...initialDesign, darkColor: '#FF0000' } })
    await settle()
    rerender({ design: { ...initialDesign, darkColor: '#FF0000', content: 'outro conteúdo' } })
    await settle()

    expect(calls.map((c) => c.path.split('/').pop())).toEqual(['preview', 'validate', 'preview'])
  })

  it('mudanças em sequência rápida viram uma requisição só (debounce)', async () => {
    const { rerender } = renderHook(({ design }) => useScannabilityValidation(design), { initialProps: { design: initialDesign } })
    for (const content of ['a', 'ab', 'abc', 'abcd']) {
      rerender({ design: { ...initialDesign, content } satisfies QrDesign })
      await act(async () => {
        await vi.advanceTimersByTimeAsync(DEBOUNCE_MS / 3)
      })
    }
    await settle()

    expect(calls).toHaveLength(1)
  })

  it('uma requisição nova cancela a anterior que ainda não respondeu', async () => {
    let release: () => void = () => {}
    vi.stubGlobal(
      'fetch',
      vi.fn(async (url: string, init: RequestInit) => {
        calls.push({ path: url, signal: init.signal! })
        if (calls.length === 1) await new Promise<void>((resolve) => (release = resolve))
        return new Response(JSON.stringify(preview), { status: 200 })
      }),
    )
    const { rerender } = renderHook(({ design }) => useScannabilityValidation(design), { initialProps: { design: initialDesign } })
    await settle()
    rerender({ design: { ...initialDesign, content: 'novo' } })
    await settle()
    release()

    expect(calls[0].signal.aborted).toBe(true)
    expect(calls[1].signal.aborted).toBe(false)
  })

  it('API fora do ar vira erro legível, e conteúdo vazio nem chega a chamar', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => Promise.reject(new TypeError('Failed to fetch'))))
    const { result, rerender } = renderHook(({ design }) => useScannabilityValidation(design), { initialProps: { design: initialDesign } })
    await settle()
    expect(result.current.status).toBe('error')
    expect(result.current.error).toMatch(/API não respondeu/)

    rerender({ design: { ...initialDesign, content: '   ' } })
    await settle()
    expect(result.current.error).toMatch(/Escreva o conteúdo/)
  })
})

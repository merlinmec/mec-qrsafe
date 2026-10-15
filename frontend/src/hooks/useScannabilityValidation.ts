import { useCallback, useEffect, useRef, useState } from 'react'
import { ApiError, fetchPreview, fetchValidation } from '../api/client'
import type { PreviewResponse, ValidationResponse } from '../api/types'
import { structuralKey, toRequest, type QrDesign } from '../design/state'

export type ValidationStatus = 'checking' | 'ready' | 'error'

export interface ScannabilityState {
  /** Último preview que chegou — a matriz desenhada no canvas. */
  preview: PreviewResponse | null
  /** Validação mais recente: do /preview ou, se só a cor mudou, do /validate. */
  validation: ValidationResponse | null
  status: ValidationStatus
  error: string | null
  /** A matriz na tela é de um design anterior ao atual (ainda conferindo, ou deu erro). */
  stale: boolean
  retry: () => void
}

export const DEBOUNCE_MS = 180

/** O que a última requisição concluída respondeu, e pra qual design. */
interface Settled {
  design: QrDesign
  attempt: number
  error: string | null
}

/**
 * Validação ao vivo (seção 9): espera o usuário parar de mexer por
 * [DEBOUNCE_MS], cancela a requisição anterior com AbortController (uma
 * resposta velha nunca sobrescreve uma nova) e escolhe o endpoint:
 * mudança na matriz (conteúdo, logo, margem, nível) pede /preview; só cor
 * pede /validate, mais leve, e o canvas repinta local.
 *
 * Com `serverReady` falso (a API da demo ainda acordando, useServerWake),
 * nada é pedido e o status fica "conferindo"; começa sozinho quando vira true.
 *
 * O status não é guardado: "conferindo" é derivado na renderização — o
 * design atual ainda não é o da última resposta. O efeito só escreve
 * estado quando uma resposta chega.
 */
export function useScannabilityValidation(design: QrDesign, serverReady = true): ScannabilityState {
  const [preview, setPreview] = useState<PreviewResponse | null>(null)
  const [previewKey, setPreviewKey] = useState<string | null>(null)
  const [validation, setValidation] = useState<ValidationResponse | null>(null)
  const [settled, setSettled] = useState<Settled | null>(null)
  const [attempt, setAttempt] = useState(0)
  // A chave do último preview é lida no efeito por um ref: como
  // dependência, cada preview novo dispararia um /validate a mais.
  const previewKeyRef = useRef<string | null>(null)
  const controller = useRef<AbortController | null>(null)

  const key = structuralKey(design)
  const contentMissing = design.content.trim() === ''

  useEffect(() => {
    controller.current?.abort()
    if (contentMissing || !serverReady) return
    const timer = window.setTimeout(async () => {
      const current = new AbortController()
      controller.current = current
      const request = toRequest(design)
      try {
        if (key !== previewKeyRef.current) {
          const response = await fetchPreview(request, current.signal)
          previewKeyRef.current = key
          setPreview(response)
          setPreviewKey(key)
          setValidation(response.validation)
        } else {
          setValidation(await fetchValidation(request, current.signal))
        }
        setSettled({ design, attempt, error: null })
      } catch (cause) {
        if (cause instanceof DOMException && cause.name === 'AbortError') return
        setSettled({ design, attempt, error: cause instanceof ApiError ? cause.message : 'Algo deu errado ao conferir o QR.' })
      }
    }, DEBOUNCE_MS)
    return () => window.clearTimeout(timer)
  }, [design, key, contentMissing, attempt, serverReady])

  useEffect(() => () => controller.current?.abort(), [])

  const retry = useCallback(() => setAttempt((n) => n + 1), [])

  const upToDate = settled !== null && settled.design === design && settled.attempt === attempt
  const error = contentMissing ? 'Escreva o conteúdo do QR — um link, um texto, um contato.' : upToDate ? settled.error : null
  const status: ValidationStatus = error ? 'error' : upToDate ? 'ready' : 'checking'

  return { preview, validation, status, error, stale: key !== previewKey, retry }
}

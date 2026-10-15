import type { ApiErrorBody, PreviewResponse, QrRequest, ValidationResponse } from './types'

// Vazio = mesma origem (Vite em dev, imagem Docker). Na demo, o editor é um
// site estático que não dorme e chama a API pelo endereço dela (docs/DEMO.md).
const API_ORIGIN = import.meta.env.VITE_API_ORIGIN ?? ''
const BASE = `${API_ORIGIN}/api/v1/qr-codes`

/** Erro com a resposta da API, quando houve uma (400, 422...). */
export class ApiError extends Error {
  readonly status: number
  readonly body?: ApiErrorBody

  constructor(status: number, message: string, body?: ApiErrorBody) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.body = body
  }
}

async function post(path: string, body: QrRequest, signal?: AbortSignal): Promise<Response> {
  let response: Response
  try {
    response = await fetch(`${BASE}${path}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
      signal,
    })
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error
    throw new ApiError(0, API_ORIGIN ? 'A API não respondeu.' : 'A API não respondeu. Ela está rodando na porta 8080?')
  }
  if (!response.ok) {
    const parsed = (await response.json().catch(() => undefined)) as ApiErrorBody | undefined
    throw new ApiError(response.status, parsed?.message ?? `A API respondeu ${response.status}.`, parsed)
  }
  return response
}

export async function fetchPreview(body: QrRequest, signal?: AbortSignal): Promise<PreviewResponse> {
  return (await post('/preview', body, signal)).json()
}

export async function fetchValidation(body: QrRequest, signal?: AbortSignal): Promise<ValidationResponse> {
  return (await post('/validate', body, signal)).json()
}

export type ExportFormat = 'png' | 'svg'

export async function fetchExport(body: QrRequest, format: ExportFormat, size: number): Promise<Blob> {
  const response = await post(`/export?format=${format}&size=${size}`, body)
  return response.blob()
}

/** O servidor já subiu? Responde rápido e não codifica nada (HealthController). */
export async function isApiUp(signal?: AbortSignal): Promise<boolean> {
  try {
    const response = await fetch(`${API_ORIGIN}/api/v1/health`, { signal })
    if (!response.ok) return false
    const body = (await response.json()) as { status?: string }
    return body.status === 'ok'
  } catch {
    return false
  }
}

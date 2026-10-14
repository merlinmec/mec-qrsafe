import type { ApiErrorBody, PreviewResponse, QrRequest, ValidationResponse } from './types'

const BASE = '/api/v1/qr-codes'

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
    throw new ApiError(0, 'A API não respondeu. Ela está rodando na porta 8080?')
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

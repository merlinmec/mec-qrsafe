import { useEffect, useState } from 'react'
import { isApiUp } from '../api/client'

/** Só avisa depois disso: com o servidor acordado, o health responde antes e nada pisca. */
export const WAKE_NOTICE_AFTER_MS = 1500
const RETRY_EVERY_MS = 2000
const ATTEMPT_TIMEOUT_MS = 8000

export interface ServerWake {
  ready: boolean
  /** Segundos esperando, quando já vale avisar; null se não há o que avisar. */
  wakingSeconds: number | null
}

/**
 * A demo roda no plano gratuito do Render, que dorme depois de 15 minutos
 * parado e leva cerca de um minuto para acordar (docs/DEMO.md). O editor é
 * estático e abre na hora; este hook bate no /health até o servidor
 * responder, e a validação só começa depois disso — em vez de uma
 * requisição pendurada por um minuto ou de um erro que não explica nada.
 */
export function useServerWake(): ServerWake {
  const [ready, setReady] = useState(false)
  const [elapsedMs, setElapsedMs] = useState(0)

  useEffect(() => {
    if (ready) return
    const started = Date.now()
    let cancelled = false
    let retry: number | undefined
    const tick = window.setInterval(() => setElapsedMs(Date.now() - started), 1000)
    const notice = window.setTimeout(() => setElapsedMs(Date.now() - started), WAKE_NOTICE_AFTER_MS)

    const attempt = async () => {
      const up = await isApiUp(AbortSignal.timeout(ATTEMPT_TIMEOUT_MS))
      if (cancelled) return
      if (up) setReady(true)
      else retry = window.setTimeout(attempt, RETRY_EVERY_MS)
    }
    void attempt()

    return () => {
      cancelled = true
      window.clearInterval(tick)
      window.clearTimeout(notice)
      window.clearTimeout(retry)
    }
  }, [ready])

  return { ready, wakingSeconds: !ready && elapsedMs >= WAKE_NOTICE_AFTER_MS ? Math.floor(elapsedMs / 1000) : null }
}

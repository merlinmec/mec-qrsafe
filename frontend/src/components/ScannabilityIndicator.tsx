import { CircleAlert, CircleCheck, CircleX, LoaderCircle, WifiOff } from 'lucide-react'
import type { ValidationResponse } from '../api/types'
import type { ValidationStatus } from '../hooks/useScannabilityValidation'
import { SECTION_LABELS, sectionFor, verdictOf, type SectionId } from '../lib/violations'
import './ScannabilityIndicator.css'

const VERDICT_COPY = {
  pass: { title: 'Escaneia', detail: 'Nenhum problema encontrado.' },
  warn: { title: 'Escaneia, com ressalvas', detail: 'Dá pra exportar, mas vale ajustar.' },
  block: { title: 'Não escaneia', detail: 'Exportar fica bloqueado até corrigir.' },
} as const

export function ScannabilityIndicator(props: {
  validation: ValidationResponse | null
  status: ValidationStatus
  error: string | null
  /** Segundos esperando o servidor da demo acordar (useServerWake); null se ele já respondeu. */
  wakingSeconds?: number | null
  onGoToSection: (section: SectionId) => void
}) {
  const { validation, status, error, wakingSeconds = null, onGoToSection } = props

  if (status === 'error' && error) {
    return (
      <div className="verdict verdict--offline" role="status">
        <WifiOff size={20} aria-hidden className="verdict__icon" />
        <div className="verdict__text">
          <strong>Não foi possível conferir</strong>
          <span>{error}</span>
        </div>
      </div>
    )
  }

  if (!validation && wakingSeconds !== null) {
    return (
      <div className="verdict verdict--checking" role="status">
        <LoaderCircle size={20} aria-hidden className="verdict__icon spin" />
        <div className="verdict__text">
          <strong>Acordando o servidor… {formatElapsed(wakingSeconds)}</strong>
          <span>
            A demo roda num plano gratuito que dorme depois de 15 minutos parado e leva cerca de um minuto para voltar.
            Pode ir ajustando: a validação começa sozinha.
          </span>
        </div>
      </div>
    )
  }

  if (!validation) {
    return (
      <div className="verdict verdict--checking" role="status">
        <LoaderCircle size={20} aria-hidden className="verdict__icon spin" />
        <div className="verdict__text">
          <strong>Conferindo…</strong>
          <span>Codificando e validando no servidor.</span>
        </div>
      </div>
    )
  }

  const verdict = verdictOf(validation.violations)
  const copy = VERDICT_COPY[verdict]
  const Icon = verdict === 'pass' ? CircleCheck : verdict === 'warn' ? CircleAlert : CircleX
  const checking = status === 'checking'

  return (
    <div className="scannability">
      <div className={`verdict verdict--${verdict}`} role="status" aria-live="polite" aria-busy={checking}>
        <Icon size={22} aria-hidden className="verdict__icon" />
        <div className="verdict__text">
          <strong>{copy.title}</strong>
          <span>{copy.detail}</span>
        </div>
        <div className="verdict__score" title="Nota da validação: bloqueio tira 0,5; aviso tira 0,15">
          <span className="verdict__score-value">{Math.round(validation.score * 100)}</span>
          <span className="verdict__score-label">de 100</span>
        </div>
        {checking && <LoaderCircle size={14} aria-label="Conferindo de novo" className="verdict__checking spin" />}
      </div>

      {validation.violations.length > 0 && (
        <ul className="violations">
          {validation.violations.map((violation, index) => {
            const section = sectionFor(violation)
            return (
              <li key={`${violation.rule}-${index}`} className={`violation violation--${violation.severity.toLowerCase()}`}>
                <span className="violation__badge">{violation.severity === 'BLOCKING' ? 'Bloqueia' : 'Aviso'}</span>
                <p className="violation__message">{violation.message}</p>
                <button type="button" className="violation__link" onClick={() => onGoToSection(section)}>
                  Ajustar em {SECTION_LABELS[section]}
                </button>
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}

function formatElapsed(seconds: number): string {
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`
}

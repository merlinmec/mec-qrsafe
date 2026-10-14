import { Download, LoaderCircle } from 'lucide-react'
import { useId, useState } from 'react'
import { ApiError, fetchExport, type ExportFormat } from '../api/client'
import type { ValidationResponse } from '../api/types'
import { useQrDesign } from '../design/useQrDesign'
import { toRequest } from '../design/state'
import type { ValidationStatus } from '../hooks/useScannabilityValidation'
import { downloadBlob } from '../lib/download'
import { Segmented } from './ui/controls'
import './ExportBar.css'

const SIZES = [512, 1024, 2048]

/**
 * Bloqueio em duas camadas (seção 9): aqui o botão fica desabilitado
 * quando a validação reprova; o servidor recusa de novo (422) se o pedido
 * chegar mesmo assim.
 */
export function ExportBar(props: { validation: ValidationResponse | null; status: ValidationStatus }) {
  const { validation, status } = props
  const { design } = useQrDesign()
  const [format, setFormat] = useState<ExportFormat>('png')
  const [size, setSize] = useState(1024)
  const [exporting, setExporting] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const reasonId = useId()

  const reason = !validation
    ? 'Esperando a validação terminar.'
    : status === 'checking'
      ? 'Conferindo a última mudança…'
      : status === 'error'
        ? 'Sem validação, sem exportar.'
        : !validation.passed
          ? 'Corrija os itens que bloqueiam para exportar.'
          : null
  const disabled = reason !== null || exporting

  async function onExport() {
    setExporting(true)
    setError(null)
    try {
      const blob = await fetchExport(toRequest(design, true), format, size)
      downloadBlob(blob, `qrcode.${format}`)
    } catch (cause) {
      setError(
        cause instanceof ApiError && cause.status === 422
          ? 'O servidor recusou: o QR não passa na validação.'
          : cause instanceof ApiError
            ? cause.message
            : 'Não foi possível exportar.',
      )
    } finally {
      setExporting(false)
    }
  }

  return (
    <div className="export">
      <div className="export__controls">
        <Segmented
          label="Formato"
          hideLabel
          size="sm"
          value={format}
          options={[
            { value: 'png', label: 'PNG', title: 'Imagem, pronta pra usar' },
            { value: 'svg', label: 'SVG', title: 'Vetor, ideal pra impressão' },
          ]}
          onChange={setFormat}
        />
        <label className="export__size">
          <span className="visually-hidden">Tamanho</span>
          <select className="input export__select" value={size} onChange={(event) => setSize(Number(event.target.value))}>
            {SIZES.map((value) => (
              <option key={value} value={value}>
                {value} px
              </option>
            ))}
          </select>
        </label>
        <button
          type="button"
          className="button button--primary export__button"
          disabled={disabled}
          aria-describedby={reason ? reasonId : undefined}
          onClick={onExport}
        >
          {exporting ? <LoaderCircle size={16} className="spin" aria-hidden /> : <Download size={16} aria-hidden />}
          Baixar {format.toUpperCase()}
        </button>
      </div>
      {(reason || error) && (
        <p id={reasonId} className={`export__note${error ? ' export__note--error' : ''}`} role={error ? 'alert' : undefined}>
          {error ?? reason}
        </p>
      )}
    </div>
  )
}

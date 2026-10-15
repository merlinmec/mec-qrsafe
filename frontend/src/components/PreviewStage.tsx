import { RefreshCw, TriangleAlert } from 'lucide-react'
import { useEffect, useLayoutEffect, useMemo, useRef, useState, type PointerEvent } from 'react'
import type { PreviewResponse } from '../api/types'
import { useQrDesign } from '../design/useQrDesign'
import type { ValidationStatus } from '../hooks/useScannabilityValidation'
import { drawQr, moduleAt, modulePxFor, type XrayMode } from '../render/drawQr'
import { localLogoBox } from '../render/logoBox'
import { parseMatrix, TYPE_LABELS } from '../render/matrix'
import { Segmented } from './ui/controls'
import './PreviewStage.css'

const MAX_CSS_SIDE = 520

const XRAY_OPTIONS: { value: XrayMode; label: string; title: string }[] = [
  { value: 'off', label: 'QR', title: 'Como vai ser exportado' },
  { value: 'types', label: 'Raio-X: estrutura', title: 'Os padrões fixos que o leitor usa antes de ler os dados' },
  { value: 'blocks', label: 'Raio-X: blocos', title: 'Cada codeword na cor do seu bloco de correção; em vermelho, os que o logo destrói' },
]

interface HoverInfo {
  x: number
  y: number
  lines: string[]
}

export function PreviewStage(props: {
  preview: PreviewResponse | null
  stale: boolean
  status: ValidationStatus
  error: string | null
  onRetry: () => void
  xray: XrayMode
  onXrayChange: (mode: XrayMode) => void
  highlightBlock: number | null
}) {
  const { preview, stale, status, error, onRetry, xray, onXrayChange, highlightBlock } = props
  const { design, dispatch } = useQrDesign()
  const frame = useRef<HTMLDivElement>(null)
  const canvas = useRef<HTMLCanvasElement>(null)
  const [cssSide, setCssSide] = useState(MAX_CSS_SIDE)
  const [hover, setHover] = useState<HoverInfo | null>(null)
  const [dragging, setDragging] = useState(false)
  const logoImage = useLogoImage(design.logo?.imageDataUrl ?? null)

  const modules = useMemo(() => (preview ? parseMatrix(preview.matrix) : null), [preview])
  const size = preview?.matrix.size ?? 0
  const quietZone = preview?.matrix.quietZone ?? 0
  const dpr = typeof window === 'undefined' ? 1 : window.devicePixelRatio || 1
  const modulePx = size ? modulePxFor(cssSide * dpr, size) : 1
  const logoBox = preview && (stale || dragging) ? localLogoBox(design, size, quietZone) : (preview?.logoBox ?? null)

  useLayoutEffect(() => {
    const element = frame.current
    if (!element) return
    const observer = new ResizeObserver(([entry]) => setCssSide(Math.min(MAX_CSS_SIDE, Math.floor(entry.contentRect.width))))
    observer.observe(element)
    return () => observer.disconnect()
  }, [])

  useEffect(() => {
    const element = canvas.current
    const context = element?.getContext('2d')
    if (!element || !context || !modules || !preview) return
    element.width = element.height = modulePx * size
    drawQr(context, {
      modules,
      darkColor: design.darkColor,
      lightColor: design.lightColor,
      moduleShape: design.moduleShape,
      eyeShape: design.eyeShape,
      gradient: design.gradientEnabled ? design.gradient : null,
      logoBox,
      logoImage,
      modulePx,
      xray,
      engineering: stale ? null : preview.engineering,
      highlightBlock,
    })
  }, [
    modules,
    preview,
    size,
    modulePx,
    design.darkColor,
    design.lightColor,
    design.moduleShape,
    design.eyeShape,
    design.gradientEnabled,
    design.gradient,
    logoBox,
    logoImage,
    xray,
    stale,
    highlightBlock,
  ])

  function toModules(event: PointerEvent<HTMLCanvasElement>) {
    const rect = event.currentTarget.getBoundingClientRect()
    const scale = (modulePx * size) / rect.width
    return {
      x: ((event.clientX - rect.left) * scale) / modulePx,
      y: ((event.clientY - rect.top) * scale) / modulePx,
      local: { x: event.clientX - rect.left, y: event.clientY - rect.top },
    }
  }

  const insideLogo = (x: number, y: number) =>
    !!logoBox && x >= logoBox.left && x <= logoBox.left + logoBox.side && y >= logoBox.top && y <= logoBox.top + logoBox.side

  function moveLogoTo(x: number, y: number) {
    const symbol = size - 2 * quietZone
    dispatch({
      type: 'setLogoGeometry',
      centerXPercent: ((x - quietZone) / symbol) * 100,
      centerYPercent: ((y - quietZone) / symbol) * 100,
    })
  }

  function onPointerDown(event: PointerEvent<HTMLCanvasElement>) {
    const point = toModules(event)
    if (!insideLogo(point.x, point.y)) return
    event.currentTarget.setPointerCapture(event.pointerId)
    setDragging(true)
    setHover(null)
  }

  function onPointerMove(event: PointerEvent<HTMLCanvasElement>) {
    if (!preview || !modules) return
    const point = toModules(event)
    if (dragging) {
      moveLogoTo(point.x, point.y)
      return
    }
    const cell = moduleAt(point.x, point.y, 1, size)
    if (!cell) return setHover(null)
    const module = modules[cell.row][cell.column]
    const lines = [TYPE_LABELS[module.type]]
    if (module.type !== 'quietZone') lines.push(`linha ${cell.row - quietZone}, coluna ${cell.column - quietZone}`)
    const codeword = stale ? -1 : preview.engineering.codewordRows[cell.row][cell.column]
    if (codeword >= 0) {
      const block = preview.engineering.codewordBlocks[codeword]
      const destroyed = preview.engineering.destroyedCodewords.includes(codeword)
      lines.push(`codeword ${codeword} · bloco ${block + 1}${destroyed ? ' · destruído pelo logo' : ''}`)
    }
    setHover({ x: point.local.x, y: point.local.y, lines })
    event.currentTarget.style.cursor = insideLogo(point.x, point.y) ? 'grab' : 'crosshair'
  }

  function onPointerUp(event: PointerEvent<HTMLCanvasElement>) {
    if (!dragging) return
    event.currentTarget.releasePointerCapture(event.pointerId)
    setDragging(false)
  }

  const showSpinner = status === 'checking' && (stale || !preview)

  return (
    <div className="stage">
      <div className="stage__toolbar">
        <Segmented label="Visualização" hideLabel size="sm" value={xray} options={XRAY_OPTIONS} onChange={onXrayChange} />
      </div>

      <div className="stage__frame" ref={frame}>
        {preview && modules ? (
          <div className={`stage__canvas-wrap${dragging ? ' is-dragging' : ''}`}>
            <canvas
              ref={canvas}
              className="stage__canvas"
              style={{ width: (modulePx * size) / dpr, height: (modulePx * size) / dpr }}
              role="img"
              aria-label={
                xray === 'off'
                  ? `Prévia do QR code: versão ${preview.validation.version}, ${size}×${size} módulos com a margem`
                  : 'Raio-X do QR code'
              }
              onPointerDown={onPointerDown}
              onPointerMove={onPointerMove}
              onPointerUp={onPointerUp}
              onPointerCancel={onPointerUp}
              onPointerLeave={() => setHover(null)}
            />
            {hover && (
              <div
                className={`stage__tooltip${hover.x > (modulePx * size) / dpr / 2 ? ' is-flipped' : ''}`}
                style={{ left: hover.x, top: hover.y }}
                aria-hidden
              >
                {hover.lines.map((line, index) => (
                  <span key={index} className={index === 0 ? 'stage__tooltip-title' : undefined}>
                    {line}
                  </span>
                ))}
              </div>
            )}
          </div>
        ) : (
          <div className="stage__placeholder" style={{ width: cssSide, height: cssSide }}>
            {status === 'error' ? (
              <StageError message={error} onRetry={onRetry} />
            ) : (
              <span className="stage__placeholder-text">Gerando o QR…</span>
            )}
          </div>
        )}
        {preview && status === 'error' && (
          <div className="stage__notice stage__notice--error" role="status">
            <TriangleAlert size={14} aria-hidden /> {error}
            <button type="button" className="stage__notice-action" onClick={onRetry}>
              Tentar de novo
            </button>
          </div>
        )}
        {showSpinner && preview && (
          <div className="stage__notice" role="status">
            <RefreshCw size={13} className="spin" aria-hidden /> Recalculando a matriz…
          </div>
        )}
      </div>
    </div>
  )
}

function StageError({ message, onRetry }: { message: string | null; onRetry: () => void }) {
  return (
    <div className="stage__error">
      <TriangleAlert size={20} aria-hidden />
      <p>{message ?? 'Não foi possível gerar o QR.'}</p>
      <button type="button" className="button" onClick={onRetry}>
        <RefreshCw size={15} aria-hidden /> Tentar de novo
      </button>
    </div>
  )
}

/** A imagem do logo carregada, só quando corresponde ao data URL atual. */
function useLogoImage(dataUrl: string | null): HTMLImageElement | null {
  const [loaded, setLoaded] = useState<{ src: string; image: HTMLImageElement } | null>(null)
  useEffect(() => {
    if (!dataUrl) return
    let cancelled = false
    const element = new Image()
    element.onload = () => !cancelled && setLoaded({ src: dataUrl, image: element })
    element.src = dataUrl
    return () => {
      cancelled = true
    }
  }, [dataUrl])
  return loaded && loaded.src === dataUrl ? loaded.image : null
}

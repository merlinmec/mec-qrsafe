import { useCallback, useState } from 'react'
import { useQrDesign } from '../design/useQrDesign'
import { useScannabilityValidation } from '../hooks/useScannabilityValidation'
import { useServerWake } from '../hooks/useServerWake'
import type { SectionId } from '../lib/violations'
import type { XrayMode } from '../render/drawQr'
import { CustomizationPanel } from './CustomizationPanel'
import { EngineeringPanel } from './EngineeringPanel'
import { ExportBar } from './ExportBar'
import { PreviewStage } from './PreviewStage'
import { ScannabilityIndicator } from './ScannabilityIndicator'
import './EditorPage.css'

export function EditorPage() {
  const { design } = useQrDesign()
  const server = useServerWake()
  const { preview, validation, status, error, stale, retry } = useScannabilityValidation(design, server.ready)
  const [xray, setXray] = useState<XrayMode>('off')
  const [highlightBlock, setHighlightBlock] = useState<number | null>(null)

  // Passar o mouse num bloco "espia" o raio-X de blocos mesmo com a visualização normal.
  const effectiveXray: XrayMode = highlightBlock !== null && !stale ? 'blocks' : xray

  const goToSection = useCallback((section: SectionId) => {
    const element = document.getElementById(`secao-${section}`)
    if (!element) return
    element.scrollIntoView({ behavior: matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth', block: 'start' })
    document.getElementById(`titulo-${section}`)?.focus({ preventScroll: true })
    element.classList.remove('panel-section--flash')
    void element.offsetWidth // reinicia a animação se clicar duas vezes seguidas
    element.classList.add('panel-section--flash')
  }, [])

  return (
    <div className="app">
      <header className="topbar">
        <a className="brand" href="/" aria-label="mec-qrsafe, início">
          <BrandMark />
          <span className="brand__name">mec-qrsafe</span>
        </a>
        <p className="topbar__tagline">Gerador de QR code que confere se ele ainda lê antes de deixar exportar.</p>
        <a className="topbar__link" href="https://github.com/merlinmec/mec-qrsafe" target="_blank" rel="noreferrer">
          <GithubIcon />
          Código no GitHub
        </a>
      </header>

      <main className="workspace">
        <aside className="workspace__controls" aria-label="Personalização">
          <CustomizationPanel validation={validation} />
        </aside>

        <section className="workspace__preview" aria-label="Prévia e validação">
          <PreviewStage
            preview={preview}
            stale={stale}
            status={status}
            error={error}
            onRetry={retry}
            xray={effectiveXray}
            onXrayChange={setXray}
            highlightBlock={highlightBlock}
          />
          <div className="workspace__checks">
            <ScannabilityIndicator
              validation={validation}
              status={status}
              error={error}
              wakingSeconds={server.wakingSeconds}
              onGoToSection={goToSection}
            />
            <ExportBar validation={validation} status={status} />
          </div>
        </section>

        <div className="workspace__inside">
          <EngineeringPanel
            engineering={preview?.engineering ?? null}
            matrixRows={preview?.matrix.rows ?? null}
            validation={validation}
            autoLevel={design.errorCorrection === 'AUTO'}
            hasLogo={design.logo !== null}
            stale={stale}
            xray={effectiveXray}
            highlightBlock={highlightBlock}
            onHighlightBlock={setHighlightBlock}
          />
        </div>
      </main>
    </div>
  )
}

/** Marca: três cantos de QR, o mesmo desenho dos padrões de localização. */
function BrandMark() {
  return (
    <svg className="brand__mark" width="24" height="24" viewBox="0 0 26 26" aria-hidden>
      <rect width="26" height="26" rx="5" fill="var(--accent)" />
      <g fill="none" stroke="var(--accent-ink)" strokeWidth="2">
        <rect x="5" y="5" width="6" height="6" rx="1" />
        <rect x="15" y="5" width="6" height="6" rx="1" />
        <rect x="5" y="15" width="6" height="6" rx="1" />
      </g>
      <rect x="16.5" y="16.5" width="3" height="3" fill="var(--accent-ink)" />
    </svg>
  )
}

/** Ícone do GitHub (o lucide-react removeu os ícones de marca). */
function GithubIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" aria-hidden>
      <path d="M12 .5C5.65.5.5 5.65.5 12a11.5 11.5 0 0 0 7.86 10.92c.58.1.79-.25.79-.56v-2c-3.2.7-3.87-1.37-3.87-1.37-.53-1.33-1.28-1.69-1.28-1.69-1.05-.72.08-.7.08-.7 1.16.08 1.77 1.19 1.77 1.19 1.03 1.77 2.7 1.26 3.36.96.1-.75.4-1.26.73-1.55-2.55-.29-5.24-1.28-5.24-5.69 0-1.26.45-2.29 1.19-3.1-.12-.29-.52-1.46.11-3.05 0 0 .97-.31 3.18 1.18a11 11 0 0 1 5.78 0c2.2-1.49 3.17-1.18 3.17-1.18.63 1.59.23 2.76.12 3.05.74.81 1.18 1.84 1.18 3.1 0 4.42-2.69 5.39-5.26 5.68.41.36.78 1.06.78 2.14v3.17c0 .31.21.67.8.56A11.5 11.5 0 0 0 23.5 12C23.5 5.65 18.35.5 12 .5Z" />
    </svg>
  )
}

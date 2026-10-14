import { ArrowLeftRight, Crosshair, ImagePlus, Sparkles, Trash2 } from 'lucide-react'
import { useRef, useState } from 'react'
import type { ErrorCorrection, ValidationResponse } from '../api/types'
import { useQrDesign } from '../design/useQrDesign'
import { LOGO_ACCEPT, LogoFileError, readLogoFile, sampleLogoDataUrl } from '../lib/logo'
import { ColorField, Section, Segmented, Slider } from './ui/controls'
import './CustomizationPanel.css'

const EXAMPLES = [
  { label: 'Link', value: 'https://github.com/merlinmec/mec-qrsafe' },
  { label: 'Wi-Fi', value: 'WIFI:T:WPA;S:Cafe da Esquina;P:cafezinho2026;;' },
  { label: 'Texto', value: 'Mesa 12 — peça pelo cardápio no celular. Obrigado pela visita!' },
]

const LEVEL_OPTIONS: { value: ErrorCorrection; label: string; title: string }[] = [
  { value: 'AUTO', label: 'Auto', title: 'O servidor escolhe o menor nível em que o logo cabe' },
  { value: 'L', label: 'L', title: 'Corrige ~7% dos codewords' },
  { value: 'M', label: 'M', title: 'Corrige ~15% dos codewords' },
  { value: 'Q', label: 'Q', title: 'Corrige ~25% dos codewords' },
  { value: 'H', label: 'H', title: 'Corrige ~30% dos codewords' },
]

export function CustomizationPanel({ validation }: { validation: ValidationResponse | null }) {
  const { design, dispatch } = useQrDesign()
  const bytes = new TextEncoder().encode(design.content).length

  return (
    <div className="customization">
      <Section id="conteudo" title="Conteúdo">
        <div className="field">
          <label className="visually-hidden" htmlFor="conteudo-qr">
            Conteúdo do QR
          </label>
          <textarea
            id="conteudo-qr"
            className="input content-input"
            rows={3}
            value={design.content}
            placeholder="https://seu-site.com.br"
            onChange={(event) => dispatch({ type: 'setContent', value: event.target.value })}
          />
          <div className="content-meta">
            <span>
              {bytes} {bytes === 1 ? 'byte' : 'bytes'} em UTF-8
            </span>
            {validation && (
              <span>
                versão {validation.version} · {validation.version * 4 + 17}×{validation.version * 4 + 17} módulos
              </span>
            )}
          </div>
        </div>
        <div className="chips" role="group" aria-label="Exemplos de conteúdo">
          {EXAMPLES.map((example) => (
            <button
              key={example.label}
              type="button"
              className="chip"
              aria-pressed={design.content === example.value}
              onClick={() => dispatch({ type: 'setContent', value: example.value })}
            >
              {example.label}
            </button>
          ))}
        </div>
      </Section>

      <Section
        id="cores"
        title="Cores"
        aside={
          <button
            type="button"
            className="button button--ghost"
            onClick={() => dispatch({ type: 'swapColors' })}
            title="Inverter as cores escura e clara"
          >
            <ArrowLeftRight size={15} aria-hidden /> Inverter
          </button>
        }
      >
        <div className="color-pair">
          <ColorField label="Módulos" value={design.darkColor} onChange={(value) => dispatch({ type: 'setDarkColor', value })} />
          <ColorField label="Fundo" value={design.lightColor} onChange={(value) => dispatch({ type: 'setLightColor', value })} />
        </div>
      </Section>

      <LogoSection />

      <Section id="margem" title="Margem">
        <Slider
          label="Margem clara em volta"
          value={design.quietZoneModules}
          min={0}
          max={10}
          unit=" módulos"
          hint="A especificação pede pelo menos 4: é o contraste com a margem que mostra ao leitor onde o QR começa."
          onChange={(value) => dispatch({ type: 'setQuietZone', value })}
        />
      </Section>

      <Section id="correcao" title="Correção de erro">
        <Segmented
          label="Nível de correção"
          hideLabel
          value={design.errorCorrection}
          options={LEVEL_OPTIONS}
          onChange={(value) => dispatch({ type: 'setErrorCorrection', value })}
        />
        <p className="field__hint">
          {design.errorCorrection === 'AUTO'
            ? `No automático, o servidor sobe de M para Q e H até o logo caber${validation ? ` — agora está em ${validation.errorCorrectionLevel}.` : '.'}`
            : 'Nível fixo: a validação diz se o logo cabe nele. Mais correção ocupa mais espaço e pode aumentar a versão do QR.'}
        </p>
      </Section>
    </div>
  )
}

function LogoSection() {
  const { design, dispatch } = useQrDesign()
  const input = useRef<HTMLInputElement>(null)
  const [error, setError] = useState<string | null>(null)
  const logo = design.logo

  async function onFile(file: File | undefined) {
    if (!file) return
    try {
      const dataUrl = await readLogoFile(file)
      setError(null)
      dispatch({ type: 'setLogoImage', imageDataUrl: dataUrl, fileName: file.name })
    } catch (cause) {
      setError(cause instanceof LogoFileError ? cause.message : 'Não foi possível usar essa imagem.')
    } finally {
      if (input.current) input.current.value = ''
    }
  }

  return (
    <Section
      id="logo"
      title="Logo"
      aside={
        logo && (
          <button type="button" className="button button--ghost" onClick={() => dispatch({ type: 'removeLogo' })}>
            <Trash2 size={15} aria-hidden /> Remover
          </button>
        )
      }
    >
      <input
        ref={input}
        type="file"
        accept={LOGO_ACCEPT}
        className="visually-hidden"
        tabIndex={-1}
        aria-hidden
        onChange={(event) => onFile(event.target.files?.[0])}
      />
      {logo ? (
        <div className="logo-file">
          <img className="logo-file__thumb" src={logo.imageDataUrl ?? undefined} alt="" />
          <span className="logo-file__name" title={logo.fileName ?? undefined}>
            {logo.fileName}
          </span>
          <button type="button" className="button" onClick={() => input.current?.click()}>
            Trocar
          </button>
        </div>
      ) : (
        <div className="logo-empty">
          <button type="button" className="button" onClick={() => input.current?.click()}>
            <ImagePlus size={16} aria-hidden /> Enviar imagem
          </button>
          <button
            type="button"
            className="button button--ghost"
            onClick={() => dispatch({ type: 'setLogoImage', imageDataUrl: sampleLogoDataUrl(), fileName: 'logo-de-exemplo.png' })}
          >
            <Sparkles size={15} aria-hidden /> Usar um de exemplo
          </button>
        </div>
      )}
      {error && (
        <p className="field-error" role="alert">
          {error}
        </p>
      )}
      {!logo && <p className="field__hint">PNG ou JPEG, até 1 MB. Ele vai sobre uma placa na cor do fundo.</p>}

      {logo && (
        <>
          <Slider
            label="Tamanho"
            value={logo.widthPercent}
            min={5}
            max={60}
            unit="% da largura"
            onChange={(value) => dispatch({ type: 'setLogoGeometry', widthPercent: value })}
          />
          <div className="logo-position">
            <Slider
              label="Horizontal"
              value={Math.round(logo.centerXPercent)}
              min={0}
              max={100}
              unit="%"
              onChange={(value) => dispatch({ type: 'setLogoGeometry', centerXPercent: value })}
            />
            <Slider
              label="Vertical"
              value={Math.round(logo.centerYPercent)}
              min={0}
              max={100}
              unit="%"
              onChange={(value) => dispatch({ type: 'setLogoGeometry', centerYPercent: value })}
            />
          </div>
          <div className="logo-actions">
            <button type="button" className="button button--ghost" onClick={() => dispatch({ type: 'centerLogo' })}>
              <Crosshair size={15} aria-hidden /> Centralizar
            </button>
            <span className="field__hint">ou arraste o logo no QR</span>
          </div>
        </>
      )}
    </Section>
  )
}

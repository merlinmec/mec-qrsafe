import type { EngineeringResponse, ValidationResponse } from '../api/types'
import { blockColor, TYPE_COLORS, type XrayMode } from '../render/drawQr'
import { TYPE_LABELS, TYPE_LETTERS, type ModuleType } from '../render/matrix'
import './EngineeringPanel.css'

/** O mesmo limiar de aviso da LogoCoverageRule (75% da correção). */
const WARNING_SHARE = 0.75

const LEGEND_TYPES: ModuleType[] = ['finder', 'separator', 'timing', 'alignment', 'format', 'version', 'darkModule']

export function EngineeringPanel(props: {
  engineering: EngineeringResponse | null
  /** Linhas da matriz do /preview — a legenda só lista os tipos que existem nela. */
  matrixRows: string[] | null
  validation: ValidationResponse | null
  autoLevel: boolean
  hasLogo: boolean
  stale: boolean
  xray: XrayMode
  onHighlightBlock: (block: number | null) => void
  highlightBlock: number | null
}) {
  const { engineering, matrixRows, validation, autoLevel, hasLogo, stale, xray, onHighlightBlock, highlightBlock } = props

  if (!engineering || !validation) {
    return (
      <aside className="engineering" aria-labelledby="engenharia-titulo">
        <h2 id="engenharia-titulo" className="engineering__title">
          Por dentro do QR
        </h2>
        <p className="engineering__lede">Os números aparecem assim que o servidor codificar o primeiro QR.</p>
      </aside>
    )
  }

  const capacity = engineering.correctableErrorsPerBlock
  const worst = Math.max(0, ...engineering.destroyedPerBlock)
  const symbol = engineering.symbolSize

  return (
    <aside className={`engineering${stale ? ' is-stale' : ''}`} aria-labelledby="engenharia-titulo">
      <div className="engineering__head">
        <h2 id="engenharia-titulo" className="engineering__title">
          Por dentro do QR
        </h2>
        <p className="engineering__lede">
          Codificado pelo encoder próprio, em Kotlin, sem biblioteca de QR. Estes são os números com que a validação decide.
        </p>
      </div>

      <dl className="facts">
        <div className="fact">
          <dt>Versão</dt>
          <dd>
            <strong>{validation.version}</strong>
            <span>
              {symbol}×{symbol} módulos
            </span>
          </dd>
        </div>
        <div className="fact">
          <dt>Correção</dt>
          <dd>
            <strong>{validation.errorCorrectionLevel}</strong>
            <span>{autoLevel ? 'no automático' : 'fixado por você'}</span>
          </dd>
        </div>
        <div className="fact">
          <dt>Máscara</dt>
          <dd>
            <strong>{engineering.mask}</strong>
            <span>menor penalidade das 8</span>
          </dd>
        </div>
        <div className="fact">
          <dt>Blocos RS</dt>
          <dd>
            <strong>{engineering.blockCount}</strong>
            <span>{capacity} erros cada</span>
          </dd>
        </div>
      </dl>

      <section className="blocks" aria-labelledby="blocos-titulo">
        <div className="blocks__head">
          <h3 id="blocos-titulo">Correção de erro por bloco</h3>
          <span className="blocks__summary">
            {hasLogo ? `pior bloco: ${worst} de ${capacity}` : 'sem logo, nada destruído'}
          </span>
        </div>
        <p className="blocks__explain">
          {hasLogo
            ? 'Cada codeword que o logo encosta vira um erro. Se algum bloco passar da linha, o QR não lê.'
            : 'Com um logo, cada codeword que ele encosta vira um erro no seu bloco.'}
        </p>
        <ol className="blocks__list" onMouseLeave={() => onHighlightBlock(null)}>
          {engineering.destroyedPerBlock.map((destroyed, block) => {
            const share = capacity > 0 ? destroyed / capacity : 0
            const tone = share > 1 ? 'block' : share > WARNING_SHARE ? 'warn' : 'ok'
            return (
              <li key={block}>
                <button
                  type="button"
                  className={`block-row block-row--${tone}${highlightBlock === block ? ' is-active' : ''}`}
                  onMouseEnter={() => onHighlightBlock(block)}
                  onFocus={() => onHighlightBlock(block)}
                  onBlur={() => onHighlightBlock(null)}
                  aria-label={`Bloco ${block + 1}: ${destroyed} codewords destruídos de ${capacity} corrigíveis`}
                >
                  <span className="block-row__swatch" style={{ background: blockColor(block, true) }} aria-hidden />
                  <span className="block-row__name">Bloco {block + 1}</span>
                  <span className="block-row__bar" aria-hidden>
                    <span className="block-row__fill" style={{ transform: `scaleX(${Math.min(share, 1)})` }} />
                    <span className="block-row__warn" style={{ left: `${WARNING_SHARE * 100}%` }} />
                  </span>
                  <span className="block-row__count">
                    {destroyed}/{capacity}
                  </span>
                </button>
              </li>
            )
          })}
        </ol>
        {xray !== 'blocks' && <p className="blocks__tip">Passe o mouse num bloco para ver os codewords dele no QR.</p>}
      </section>

      {xray === 'types' && (
        <section className="legend" aria-label="Legenda do raio-X de estrutura">
          {LEGEND_TYPES.filter((type) => matrixRows?.some((row) => row.toUpperCase().includes(TYPE_LETTERS[type]))).map((type) => (
            <span key={type} className="legend__item">
              <span className="legend__swatch" style={{ background: TYPE_COLORS[type] }} aria-hidden />
              {TYPE_LABELS[type]}
            </span>
          ))}
          <span className="legend__item">
            <span className="legend__swatch legend__swatch--data" aria-hidden />
            {TYPE_LABELS.data}
          </span>
        </section>
      )}
    </aside>
  )
}

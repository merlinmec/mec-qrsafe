import { useId, useState, type ReactNode } from 'react'
import './controls.css'

export function Slider(props: {
  label: string
  value: number
  min: number
  max: number
  step?: number
  unit?: string
  hint?: ReactNode
  onChange: (value: number) => void
}) {
  const id = useId()
  const { label, value, min, max, step = 1, unit = '', hint, onChange } = props
  const fill = ((value - min) / (max - min)) * 100
  return (
    <div className="field">
      <div className="field__row">
        <label className="field__label" htmlFor={id}>
          {label}
        </label>
        <output className="field__value" htmlFor={id}>
          {formatNumber(value, step)}
          {unit}
        </output>
      </div>
      <input
        id={id}
        className="slider"
        type="range"
        min={min}
        max={max}
        step={step}
        value={value}
        style={{ '--fill': `${fill}%` } as React.CSSProperties}
        onChange={(event) => onChange(Number(event.target.value))}
      />
      {hint && <p className="field__hint">{hint}</p>}
    </div>
  )
}

const formatNumber = (value: number, step: number) =>
  step < 1 ? value.toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 }) : String(value)

export function Segmented<T extends string>(props: {
  label: string
  value: T
  options: { value: T; label: string; title?: string }[]
  onChange: (value: T) => void
  hideLabel?: boolean
  size?: 'sm' | 'md'
}) {
  const name = useId()
  const { label, value, options, onChange, hideLabel, size = 'md' } = props
  return (
    <fieldset className={`segmented segmented--${size}`}>
      <legend className={hideLabel ? 'visually-hidden' : 'field__label'}>{label}</legend>
      <div className="segmented__track">
        {options.map((option) => (
          <label key={option.value} className="segmented__option" title={option.title}>
            <input
              type="radio"
              name={name}
              value={option.value}
              checked={option.value === value}
              onChange={() => onChange(option.value)}
            />
            <span>{option.label}</span>
          </label>
        ))}
      </div>
    </fieldset>
  )
}

const HEX = /^#[0-9a-f]{6}$/i

/** Seletor de cor nativo + campo hex que só confirma quando o valor é válido. */
export function ColorField(props: { label: string; value: string; onChange: (value: string) => void }) {
  const id = useId()
  const { label, value, onChange } = props
  const [draft, setDraft] = useState(value)
  const [lastValue, setLastValue] = useState(value)
  if (value !== lastValue) {
    // Sincroniza o rascunho quando a cor muda por fora (seletor, trocar cores).
    setLastValue(value)
    setDraft(value)
  }
  const invalid = !HEX.test(draft)
  return (
    <div className="field color-field">
      <label className="field__label" htmlFor={id}>
        {label}
      </label>
      <div className="color-field__row">
        <input
          className="color-field__swatch"
          type="color"
          aria-label={`${label}: escolher no seletor`}
          value={value.toLowerCase()}
          onChange={(event) => onChange(event.target.value.toUpperCase())}
        />
        <input
          id={id}
          className="input color-field__hex"
          value={draft}
          spellCheck={false}
          maxLength={7}
          aria-invalid={invalid}
          onChange={(event) => {
            const next = event.target.value.startsWith('#') ? event.target.value : `#${event.target.value}`
            setDraft(next.toUpperCase())
            if (HEX.test(next)) onChange(next.toUpperCase())
          }}
          onBlur={() => setDraft(value)}
        />
      </div>
    </div>
  )
}

export function Section(props: { id: string; title: string; children: ReactNode; aside?: ReactNode }) {
  return (
    <section className="panel-section" id={`secao-${props.id}`} aria-labelledby={`titulo-${props.id}`}>
      <div className="panel-section__head">
        <h2 className="panel-section__title" id={`titulo-${props.id}`} tabIndex={-1}>
          {props.title}
        </h2>
        {props.aside}
      </div>
      {props.children}
    </section>
  )
}

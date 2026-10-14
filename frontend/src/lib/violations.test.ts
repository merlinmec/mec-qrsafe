import { describe, expect, it } from 'vitest'
import { sectionFor, verdictOf } from './violations'

describe('violações', () => {
  it('cada regra leva à seção que resolve', () => {
    expect(sectionFor({ rule: 'contrast', severity: 'BLOCKING', message: '' })).toBe('cores')
    expect(sectionFor({ rule: 'logo_coverage', severity: 'WARNING', message: '' })).toBe('logo')
    expect(sectionFor({ rule: 'function_pattern_overlap', severity: 'BLOCKING', message: '' })).toBe('logo')
  })

  it('margem tem dois motivos: margem pequena ou logo vazando', () => {
    expect(sectionFor({ rule: 'quiet_zone', severity: 'BLOCKING', message: 'A margem clara tem 1 módulo(s)' })).toBe('margem')
    expect(sectionFor({ rule: 'quiet_zone', severity: 'BLOCKING', message: 'O logo sai do QR e invade a margem' })).toBe('logo')
  })

  it('veredito: bloqueio vence aviso, sem nada passa', () => {
    expect(verdictOf([])).toBe('pass')
    expect(verdictOf([{ rule: 'logo_coverage', severity: 'WARNING', message: '' }])).toBe('warn')
    expect(
      verdictOf([
        { rule: 'logo_coverage', severity: 'WARNING', message: '' },
        { rule: 'contrast', severity: 'BLOCKING', message: '' },
      ]),
    ).toBe('block')
  })
})

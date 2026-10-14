import type { Violation } from '../api/types'

export type SectionId = 'conteudo' | 'cores' | 'logo' | 'margem' | 'correcao'

export const SECTION_LABELS: Record<SectionId, string> = {
  conteudo: 'Conteúdo',
  cores: 'Cores',
  logo: 'Logo',
  margem: 'Margem',
  correcao: 'Correção de erro',
}

/**
 * Pra onde cada violação manda o usuário (seção 9: mensagens "acionáveis,
 * apontando pra seção do painel responsável"). A margem tem dois motivos
 * possíveis: margem pequena (seção Margem) ou logo vazando pra fora (Logo).
 */
export function sectionFor(violation: Violation): SectionId {
  switch (violation.rule) {
    case 'contrast':
      return 'cores'
    case 'function_pattern_overlap':
    case 'logo_coverage':
      return 'logo'
    case 'quiet_zone':
      return violation.message.toLowerCase().includes('logo') ? 'logo' : 'margem'
    default:
      return 'conteudo'
  }
}

export type Verdict = 'pass' | 'warn' | 'block'

export function verdictOf(violations: Violation[]): Verdict {
  if (violations.some((v) => v.severity === 'BLOCKING')) return 'block'
  if (violations.length > 0) return 'warn'
  return 'pass'
}

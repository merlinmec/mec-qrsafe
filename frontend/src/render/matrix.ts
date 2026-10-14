import type { MatrixResponse } from '../api/types'

export type ModuleType =
  | 'finder'
  | 'separator'
  | 'alignment'
  | 'timing'
  | 'format'
  | 'version'
  | 'darkModule'
  | 'data'
  | 'quietZone'

export interface Module {
  dark: boolean
  type: ModuleType
}

// Legenda do /preview (MatrixResponse no qr-api): letra = tipo, caixa = cor.
const TYPES: Record<string, ModuleType> = {
  F: 'finder',
  S: 'separator',
  A: 'alignment',
  T: 'timing',
  I: 'format',
  V: 'version',
  K: 'darkModule',
  D: 'data',
  Q: 'quietZone',
}

/** A letra (maiúscula) de cada tipo na matriz do /preview. */
export const TYPE_LETTERS = Object.fromEntries(Object.entries(TYPES).map(([letter, type]) => [type, letter])) as Record<
  ModuleType,
  string
>

export function decodeModule(letter: string): Module {
  const type = TYPES[letter.toUpperCase()]
  if (!type) throw new Error(`letra de módulo desconhecida: "${letter}"`)
  return { dark: letter !== letter.toLowerCase(), type }
}

/** A matriz do /preview como grade de módulos. */
export function parseMatrix(matrix: MatrixResponse): Module[][] {
  if (matrix.rows.length !== matrix.size) throw new Error('matriz com número de linhas errado')
  return matrix.rows.map((row) => {
    if (row.length !== matrix.size) throw new Error('matriz com linha de tamanho errado')
    return Array.from(row, decodeModule)
  })
}

export const TYPE_LABELS: Record<ModuleType, string> = {
  finder: 'Padrão de localização',
  separator: 'Separador',
  alignment: 'Padrão de alinhamento',
  timing: 'Linha de sincronismo',
  format: 'Informação de formato',
  version: 'Informação de versão',
  darkModule: 'Módulo escuro fixo',
  data: 'Dados e correção',
  quietZone: 'Margem',
}

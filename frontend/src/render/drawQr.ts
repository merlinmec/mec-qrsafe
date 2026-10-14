import type { EngineeringResponse, LogoBox } from '../api/types'
import type { Module, ModuleType } from './matrix'

export type XrayMode = 'off' | 'types' | 'blocks'

export interface DrawParams {
  modules: Module[][]
  darkColor: string
  lightColor: string
  logoBox: LogoBox | null
  logoImage: CanvasImageSource | null
  /** Pixels por módulo no canvas (inteiro: módulo fracionário borra a borda). */
  modulePx: number
  xray: XrayMode
  engineering: EngineeringResponse | null
  /** Bloco em destaque no modo "blocos" (os outros esmaecem). */
  highlightBlock: number | null
}

/** Folga entre a placa e a imagem do logo — a mesma do export (LogoLayout no qr-render). */
export const LOGO_PADDING_SHARE = 0.08

/** Pixels inteiros por módulo que cabem em [sidePx]. */
export function modulePxFor(sidePx: number, matrixSize: number): number {
  return Math.max(1, Math.floor(sidePx / matrixSize))
}

/** O módulo sob o ponto (em px do canvas), ou null fora da matriz. */
export function moduleAt(x: number, y: number, modulePx: number, matrixSize: number): { row: number; column: number } | null {
  const row = Math.floor(y / modulePx)
  const column = Math.floor(x / modulePx)
  if (row < 0 || column < 0 || row >= matrixSize || column >= matrixSize) return null
  return { row, column }
}

// Cores do raio-X por tipo: os padrões fixos que o leitor usa antes de ler
// dado. Dados ficam sem tinta, pra o olho ver o que é estrutura.
export const TYPE_COLORS: Partial<Record<ModuleType, string>> = {
  finder: '#7C3AED',
  separator: '#C4B5FD',
  alignment: '#2563EB',
  timing: '#0D9488',
  format: '#D97706',
  version: '#EA580C',
  darkModule: '#DB2777',
}

export const DESTROYED_COLOR = '#DC2626'

/**
 * Matiz do bloco: ângulo áureo (blocos vizinhos nunca ficam parecidos),
 * dentro de 30°–330° — a faixa dos vermelhos fica reservada para os
 * codewords destruídos pelo logo.
 */
export function blockHue(block: number): number {
  return 30 + ((block * 137.508) % 300)
}

export function blockColor(block: number, dark: boolean): string {
  const hue = blockHue(block)
  return dark ? `hsl(${hue} 62% 38%)` : `hsl(${hue} 70% 88%)`
}

export function drawQr(ctx: CanvasRenderingContext2D, p: DrawParams): void {
  const n = p.modules.length
  const px = p.modulePx
  const side = n * px

  ctx.fillStyle = p.lightColor
  ctx.fillRect(0, 0, side, side)

  const destroyed = p.engineering ? new Set(p.engineering.destroyedCodewords) : new Set<number>()

  for (let row = 0; row < n; row++) {
    for (let column = 0; column < n; column++) {
      const module = p.modules[row][column]
      const fill = moduleFill(p, module, row, column, destroyed)
      if (fill) {
        ctx.fillStyle = fill
        ctx.fillRect(column * px, row * px, px, px)
      }
    }
  }

  if (p.logoBox) drawLogo(ctx, p, p.logoBox)
}

function moduleFill(p: DrawParams, module: Module, row: number, column: number, destroyed: Set<number>): string | null {
  if (p.xray === 'off') return module.dark ? p.darkColor : null

  if (p.xray === 'types') {
    const tint = TYPE_COLORS[module.type]
    if (tint) return module.dark ? tint : `${tint}40`
    return module.dark ? '#3F3F46' : null
  }

  // Modo "blocos": cada módulo de dado na cor do bloco do seu codeword.
  const codeword = p.engineering?.codewordRows[row]?.[column] ?? -1
  if (codeword < 0) return module.dark ? '#D4D4D8' : null
  const block = p.engineering!.codewordBlocks[codeword]
  if (destroyed.has(codeword)) return module.dark ? DESTROYED_COLOR : '#FCA5A5'
  if (p.highlightBlock !== null && p.highlightBlock !== block) return module.dark ? '#E4E4E7' : null
  return blockColor(block, module.dark)
}

function drawLogo(ctx: CanvasRenderingContext2D, p: DrawParams, box: LogoBox): void {
  const px = p.modulePx
  const x = box.left * px
  const y = box.top * px
  const size = box.side * px

  if (p.xray !== 'off') {
    // No raio-X a placa fica transparente: dá pra ver o que o logo cobre.
    ctx.save()
    ctx.setLineDash([Math.max(2, px * 0.6), Math.max(2, px * 0.4)])
    ctx.lineWidth = Math.max(1.5, px * 0.18)
    ctx.strokeStyle = '#18181B'
    ctx.strokeRect(x, y, size, size)
    ctx.restore()
    return
  }

  ctx.fillStyle = p.lightColor
  ctx.fillRect(x, y, size, size)
  const image = p.logoImage
  if (!image) return
  const width = sourceSize(image, 'width')
  const height = sourceSize(image, 'height')
  if (!width || !height) return
  const padding = size * LOGO_PADDING_SHARE
  const inner = size - 2 * padding
  const scale = Math.min(inner / width, inner / height)
  const drawWidth = width * scale
  const drawHeight = height * scale
  ctx.imageSmoothingQuality = 'high'
  ctx.drawImage(image, x + padding + (inner - drawWidth) / 2, y + padding + (inner - drawHeight) / 2, drawWidth, drawHeight)
}

function sourceSize(image: CanvasImageSource, axis: 'width' | 'height'): number {
  if ('naturalWidth' in image) return axis === 'width' ? image.naturalWidth : image.naturalHeight
  const value = (image as { width?: unknown; height?: unknown })[axis]
  return typeof value === 'number' ? value : 0
}

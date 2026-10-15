import type { EngineeringResponse, EyeShape, GradientRequest, LogoBox, ModuleShape } from '../api/types'
import {
  addModuleShape,
  EYE_CENTER_CORNER,
  EYE_HOLE_CORNER,
  EYE_OUTER_CORNER,
  eyes,
  linearGradientLine,
  quietZoneOf,
  radialGradientRadius,
} from './geometry'
import type { Module, ModuleType } from './matrix'

export type XrayMode = 'off' | 'types' | 'blocks'

export interface DrawParams {
  modules: Module[][]
  darkColor: string
  lightColor: string
  /** Só no modo normal: o raio-X mostra a estrutura, sempre em módulos quadrados. */
  moduleShape?: ModuleShape
  eyeShape?: EyeShape
  gradient?: GradientRequest | null
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

  if (p.xray === 'off') {
    drawStyled(ctx, p)
    if (p.logoBox) drawLogo(ctx, p, p.logoBox)
    return
  }

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

/**
 * O QR como vai ser exportado: módulos na forma escolhida, os três olhos
 * desenhados inteiros (moldura com furo + miolo) e o degradê, com as
 * medidas de geometry.ts — as mesmas do PNG e do SVG do servidor.
 */
function drawStyled(ctx: CanvasRenderingContext2D, p: DrawParams): void {
  const n = p.modules.length
  const px = p.modulePx
  const moduleShape = p.moduleShape ?? 'SQUARE'
  const eyeShape = p.eyeShape ?? 'SQUARE'
  const quietZone = quietZoneOf(p.modules)

  ctx.fillStyle = darkPaint(ctx, p, n, quietZone)
  ctx.beginPath()
  for (let row = 0; row < n; row++) {
    for (let column = 0; column < n; column++) {
      const module = p.modules[row][column]
      // Os finders saem inteiros como olhos, logo abaixo.
      if (module.dark && module.type !== 'finder') addModuleShape(ctx, moduleShape, column, row, px)
    }
  }
  ctx.fill()

  ctx.beginPath()
  for (const eye of eyes(n, quietZone)) {
    const x = eye.left * px
    const y = eye.top * px
    addEyePart(ctx, eyeShape, x, y, 7 * px, EYE_OUTER_CORNER * px)
    addEyePart(ctx, eyeShape, x + px, y + px, 5 * px, EYE_HOLE_CORNER * px)
    addEyePart(ctx, eyeShape, x + 2 * px, y + 2 * px, 3 * px, EYE_CENTER_CORNER * px)
  }
  // evenodd: a moldura de 7 menos o furo de 5, e o miolo de 3 de volta.
  ctx.fill('evenodd')
}

function addEyePart(ctx: CanvasRenderingContext2D, shape: EyeShape, x: number, y: number, size: number, corner: number) {
  if (shape === 'CIRCLE') {
    ctx.moveTo(x + size, y + size / 2)
    ctx.arc(x + size / 2, y + size / 2, size / 2, 0, Math.PI * 2)
  } else if (shape === 'ROUNDED') {
    ctx.roundRect(x, y, size, size, corner)
  } else {
    ctx.rect(x, y, size, size)
  }
}

function darkPaint(ctx: CanvasRenderingContext2D, p: DrawParams, n: number, quietZone: number): string | CanvasGradient {
  const gradient = p.gradient
  if (!gradient) return p.darkColor
  const px = p.modulePx
  let paint: CanvasGradient
  if (gradient.type === 'RADIAL') {
    const center = (n / 2) * px
    paint = ctx.createRadialGradient(center, center, 0, center, center, radialGradientRadius(n, quietZone) * px)
  } else {
    const line = linearGradientLine(n, quietZone, gradient.angleDegrees)
    paint = ctx.createLinearGradient(line.x1 * px, line.y1 * px, line.x2 * px, line.y2 * px)
  }
  paint.addColorStop(0, p.darkColor)
  paint.addColorStop(1, gradient.endColor)
  return paint
}

function moduleFill(p: DrawParams, module: Module, row: number, column: number, destroyed: Set<number>): string | null {
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

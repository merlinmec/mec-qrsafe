// As mesmas medidas do QrGeometry do qr-render (Kotlin), em unidades de
// módulo e nas coordenadas da matriz. O preview e o arquivo exportado têm
// de sair iguais — mudou aqui, muda lá. Toda combinação de forma é lida
// pelo ZXing no ShapesAgainstReaderTest do backend.

import type { ModuleShape } from '../api/types'
import type { Module } from './matrix'

export const DOT_DIAMETER = 0.9
export const MODULE_CORNER = 0.3
export const EYE_OUTER_CORNER = 2.0
export const EYE_HOLE_CORNER = 1.2
export const EYE_CENTER_CORNER = 0.9

/** Largura da quiet zone, lida da matriz: primeira coluna da linha do meio que não é margem. */
export function quietZoneOf(modules: Module[][]): number {
  const middle = modules[Math.floor(modules.length / 2)]
  return middle.findIndex((module) => module.type !== 'quietZone')
}

/** Canto superior esquerdo dos três olhos (7x7): cima-esquerda, cima-direita, baixo-esquerda. */
export function eyes(matrixSize: number, quietZone: number): { left: number; top: number }[] {
  const near = quietZone
  const far = matrixSize - quietZone - 7
  return [
    { left: near, top: near },
    { left: far, top: near },
    { left: near, top: far },
  ]
}

/** Linha do degradê linear: atravessa o símbolo inteiro na direção do ângulo. */
export function linearGradientLine(matrixSize: number, quietZone: number, angleDegrees: number) {
  const symbol = matrixSize - 2 * quietZone
  const center = matrixSize / 2
  const radians = (angleDegrees * Math.PI) / 180
  const dx = Math.cos(radians)
  const dy = Math.sin(radians)
  const half = (symbol / 2) * (Math.abs(dx) + Math.abs(dy))
  return { x1: center - dx * half, y1: center - dy * half, x2: center + dx * half, y2: center + dy * half }
}

/** Raio do degradê radial: do centro até o canto do símbolo. */
export function radialGradientRadius(matrixSize: number, quietZone: number): number {
  return ((matrixSize - 2 * quietZone) / 2) * Math.SQRT2
}

/** Adiciona ao caminho atual a forma de um módulo na posição (coluna, linha), em px. */
export function addModuleShape(ctx: CanvasRenderingContext2D, shape: ModuleShape, column: number, row: number, px: number) {
  const x = column * px
  const y = row * px
  if (shape === 'DOTS') {
    const radius = (DOT_DIAMETER * px) / 2
    ctx.moveTo(x + px / 2 + radius, y + px / 2)
    ctx.arc(x + px / 2, y + px / 2, radius, 0, Math.PI * 2)
  } else if (shape === 'ROUNDED') {
    ctx.roundRect(x, y, px, px, MODULE_CORNER * px)
  } else {
    ctx.rect(x, y, px, px)
  }
}

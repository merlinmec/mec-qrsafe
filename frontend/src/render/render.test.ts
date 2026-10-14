import { describe, expect, it } from 'vitest'
import type { EngineeringResponse } from '../api/types'
import { localLogoBox } from './logoBox'
import { initialDesign } from '../design/state'
import { blockHue, DESTROYED_COLOR, drawQr, moduleAt, modulePxFor } from './drawQr'
import { decodeModule, parseMatrix } from './matrix'

describe('matriz do /preview', () => {
  it('letra é o tipo e a caixa é a cor', () => {
    expect(decodeModule('F')).toEqual({ dark: true, type: 'finder' })
    expect(decodeModule('s')).toEqual({ dark: false, type: 'separator' })
    expect(decodeModule('K')).toEqual({ dark: true, type: 'darkModule' })
    expect(decodeModule('q')).toEqual({ dark: false, type: 'quietZone' })
  })

  it('recusa letra desconhecida e matriz torta', () => {
    expect(() => decodeModule('X')).toThrow()
    expect(() => parseMatrix({ size: 2, quietZone: 0, rows: ['Dd'] })).toThrow()
    expect(() => parseMatrix({ size: 2, quietZone: 0, rows: ['Dd', 'd'] })).toThrow()
  })
})

describe('geometria do canvas', () => {
  it('módulo em pixels inteiros que cabem no lado', () => {
    expect(modulePxFor(520, 41)).toBe(12)
    expect(modulePxFor(10, 41)).toBe(1)
  })

  it('acha o módulo sob o ponteiro e nada fora da matriz', () => {
    expect(moduleAt(25, 13, 12, 41)).toEqual({ row: 1, column: 2 })
    expect(moduleAt(-1, 5, 12, 41)).toBeNull()
    expect(moduleAt(12 * 41, 5, 12, 41)).toBeNull()
  })

  it('caixa local do logo é a mesma conta do LogoFootprint no servidor', () => {
    // Versão 4: símbolo de 33 módulos + margem de 4 = matriz de 41.
    const design = { ...initialDesign, logo: { imageDataUrl: null, fileName: null, widthPercent: 20, centerXPercent: 50, centerYPercent: 25 } }
    const box = localLogoBox(design, 41, 4)!
    expect(box.side).toBeCloseTo(6.6)
    expect(box.left).toBeCloseTo(4 + 16.5 - 3.3)
    expect(box.top).toBeCloseTo(4 + 8.25 - 3.3)
  })
})

describe('cores dos blocos', () => {
  it('nenhum bloco usa a faixa dos vermelhos, reservada aos codewords destruídos', () => {
    for (let block = 0; block < 81; block++) {
      const hue = blockHue(block)
      expect(hue).toBeGreaterThanOrEqual(30)
      expect(hue).toBeLessThanOrEqual(330)
    }
  })
})

/** Contexto 2D falso que guarda a cor de cada fillRect. */
function recorder() {
  const fills: { style: string; x: number; y: number }[] = []
  const ctx = {
    fillStyle: '',
    fillRect(x: number, y: number) {
      fills.push({ style: String(this.fillStyle), x, y })
    },
    save() {},
    restore() {},
    setLineDash() {},
    strokeRect() {},
    drawImage() {},
  }
  return { ctx: ctx as unknown as CanvasRenderingContext2D, fills }
}

describe('drawQr', () => {
  // 2x2: um escuro de dado, um claro de dado, um finder escuro, margem.
  const modules = parseMatrix({ size: 2, quietZone: 0, rows: ['Dd', 'Fq'] })
  const engineering: EngineeringResponse = {
    mask: 0,
    symbolSize: 2,
    blockCount: 2,
    correctableErrorsPerBlock: 1,
    destroyedPerBlock: [1, 0],
    codewordBlocks: [0, 1],
    destroyedCodewords: [0],
    codewordRows: [
      [0, 1],
      [-1, -1],
    ],
  }
  const base = {
    modules,
    darkColor: '#123456',
    lightColor: '#FFFFFF',
    logoBox: null,
    logoImage: null,
    modulePx: 10,
    engineering,
    highlightBlock: null,
  }

  it('no modo normal pinta o fundo e só os módulos escuros, na cor escolhida', () => {
    const { ctx, fills } = recorder()
    drawQr(ctx, { ...base, xray: 'off' })
    expect(fills[0]).toMatchObject({ style: '#FFFFFF' })
    expect(fills.slice(1).map((f) => [f.x, f.y, f.style])).toEqual([
      [0, 0, '#123456'],
      [0, 10, '#123456'],
    ])
  })

  it('no raio-X de blocos o codeword destruído sai em vermelho', () => {
    const { ctx, fills } = recorder()
    drawQr(ctx, { ...base, xray: 'blocks' })
    expect(fills.slice(1).find((f) => f.x === 0 && f.y === 0)?.style).toBe(DESTROYED_COLOR)
  })

  it('a placa do logo é desenhada na cor clara', () => {
    const { ctx, fills } = recorder()
    drawQr(ctx, { ...base, xray: 'off', logoBox: { left: 0.5, top: 0.5, side: 1 } })
    expect(fills.at(-1)).toMatchObject({ style: '#FFFFFF', x: 5, y: 5 })
  })
})

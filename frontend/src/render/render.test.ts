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
  const shapes: { kind: string; args: number[] }[] = []
  const pathFills: { style: unknown; rule?: string; shapes: { kind: string; args: number[] }[] }[] = []
  let current: { kind: string; args: number[] }[] = []
  const gradients: { kind: string; args: number[]; stops: [number, string][] }[] = []
  const ctx = {
    fillStyle: '' as unknown,
    fillRect(x: number, y: number) {
      fills.push({ style: String(this.fillStyle), x, y })
    },
    beginPath() {
      current = []
    },
    rect: (...args: number[]) => current.push({ kind: 'rect', args }),
    roundRect: (...args: number[]) => current.push({ kind: 'roundRect', args }),
    arc: (...args: number[]) => current.push({ kind: 'arc', args }),
    moveTo() {},
    fill(rule?: string) {
      pathFills.push({ style: this.fillStyle, rule, shapes: current })
      shapes.push(...current)
    },
    createLinearGradient(...args: number[]) {
      const gradient = { kind: 'linear', args, stops: [] as [number, string][], addColorStop: (o: number, c: string) => gradient.stops.push([o, c]) }
      gradients.push(gradient)
      return gradient
    },
    createRadialGradient(...args: number[]) {
      const gradient = { kind: 'radial', args, stops: [] as [number, string][], addColorStop: (o: number, c: string) => gradient.stops.push([o, c]) }
      gradients.push(gradient)
      return gradient
    },
    save() {},
    restore() {},
    setLineDash() {},
    strokeRect() {},
    drawImage() {},
  }
  return { ctx: ctx as unknown as CanvasRenderingContext2D, fills, pathFills, gradients }
}

/**
 * Matriz sintética 15x15, sem margem: os três finders de 7x7 nos cantos
 * (escuros, para o teste só contar posição) e três módulos de dado
 * escuros no canto de baixo-direita.
 */
function fakeSymbol() {
  const rows = Array.from({ length: 15 }, (_, row) =>
    Array.from({ length: 15 }, (_, column) => {
      const inFinder = (row < 7 && column < 7) || (row < 7 && column >= 8) || (row >= 8 && column < 7)
      if (inFinder) return 'F'
      return row >= 12 && column >= 12 && (row + column) % 2 === 0 ? 'D' : 'd'
    }).join(''),
  )
  return parseMatrix({ size: 15, quietZone: 0, rows })
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

  const styled = { ...base, modules: fakeSymbol(), engineering: null }
  const darkDataModules = 5 // (12,12), (12,14), (13,13), (14,12), (14,14)

  it('no modo normal: fundo claro, um caminho com os módulos de dado e outro com os três olhos', () => {
    const { ctx, fills, pathFills } = recorder()
    drawQr(ctx, { ...styled, xray: 'off' })

    expect(fills[0]).toMatchObject({ style: '#FFFFFF' })
    const [modulesPath, eyesPath] = pathFills
    expect(modulesPath.style).toBe('#123456')
    expect(modulesPath.shapes.map((s) => [s.kind, s.args[0], s.args[1]])).toContainEqual(['rect', 120, 120])
    expect(modulesPath.shapes).toHaveLength(darkDataModules)
    expect(eyesPath.rule).toBe('evenodd')
    expect(eyesPath.shapes).toHaveLength(3 * 3) // moldura, furo e miolo de cada olho
  })

  it('pontos têm o diâmetro do export (0,9 do módulo) e olhos em círculo viram arcos', () => {
    const { ctx, pathFills } = recorder()
    drawQr(ctx, { ...styled, xray: 'off', moduleShape: 'DOTS', eyeShape: 'CIRCLE' })

    const [modulesPath, eyesPath] = pathFills
    expect(modulesPath.shapes.every((s) => s.kind === 'arc' && s.args[2] === 4.5)).toBe(true)
    // Olho de cima-esquerda: centro em (35, 35), raios 35, 25 e 15 (7, 5 e 3 módulos).
    expect(eyesPath.shapes.slice(0, 3).map((s) => s.args.slice(0, 3))).toEqual([
      [35, 35, 35],
      [35, 35, 25],
      [35, 35, 15],
    ])
  })

  it('degradê linear atravessa o símbolo na direção do ângulo e pinta módulos e olhos', () => {
    const { ctx, pathFills, gradients } = recorder()
    drawQr(ctx, { ...styled, xray: 'off', gradient: { type: 'LINEAR', endColor: '#6D28D9', angleDegrees: 0 } })

    const [gradient] = gradients
    expect(gradient.args).toEqual([0, 75, 150, 75]) // 0° = da borda esquerda à direita, na altura do meio
    expect(gradient.stops).toEqual([
      [0, '#123456'],
      [1, '#6D28D9'],
    ])
    expect(pathFills.every((p) => p.style === gradient)).toBe(true)
  })

  it('o raio-X desenha a estrutura em módulos quadrados, sem forma nem degradê', () => {
    const { ctx, pathFills, gradients } = recorder()
    drawQr(ctx, { ...base, xray: 'types', moduleShape: 'DOTS', gradient: { type: 'RADIAL', endColor: '#6D28D9', angleDegrees: 0 } })

    expect(pathFills).toHaveLength(0)
    expect(gradients).toHaveLength(0)
  })

  it('no raio-X de blocos o codeword destruído sai em vermelho', () => {
    const { ctx, fills } = recorder()
    drawQr(ctx, { ...base, xray: 'blocks' })
    expect(fills.slice(1).find((f) => f.x === 0 && f.y === 0)?.style).toBe(DESTROYED_COLOR)
  })

  it('a placa do logo é desenhada na cor clara', () => {
    const { ctx, fills } = recorder()
    drawQr(ctx, { ...styled, xray: 'off', logoBox: { left: 0.5, top: 0.5, side: 1 } })
    expect(fills.at(-1)).toMatchObject({ style: '#FFFFFF', x: 5, y: 5 })
  })
})

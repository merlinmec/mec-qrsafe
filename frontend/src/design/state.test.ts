import { describe, expect, it } from 'vitest'
import { designReducer, initialDesign, structuralKey, toRequest, type QrDesign } from './state'

const withLogo: QrDesign = {
  ...initialDesign,
  logo: { imageDataUrl: 'data:image/png;base64,AAAA', fileName: 'logo.png', widthPercent: 20, centerXPercent: 50, centerYPercent: 50 },
}

describe('designReducer', () => {
  it('troca as cores escura e clara', () => {
    const next = designReducer({ ...initialDesign, darkColor: '#111111', lightColor: '#EEEEEE' }, { type: 'swapColors' })
    expect([next.darkColor, next.lightColor]).toEqual(['#EEEEEE', '#111111'])
  })

  it('limita a posição do logo a 0–100% (o arraste pode passar da borda)', () => {
    const next = designReducer(withLogo, { type: 'setLogoGeometry', centerXPercent: 130, centerYPercent: -8 })
    expect([next.logo?.centerXPercent, next.logo?.centerYPercent]).toEqual([100, 0])
  })

  it('trocar a imagem mantém tamanho e posição do logo', () => {
    const moved = designReducer(withLogo, { type: 'setLogoGeometry', widthPercent: 33, centerXPercent: 40 })
    const next = designReducer(moved, { type: 'setLogoImage', imageDataUrl: 'data:image/png;base64,BBBB', fileName: 'outro.png' })
    expect(next.logo).toMatchObject({ widthPercent: 33, centerXPercent: 40, fileName: 'outro.png' })
  })

  it('mexer na geometria sem logo não cria um', () => {
    expect(designReducer(initialDesign, { type: 'setLogoGeometry', widthPercent: 30 }).logo).toBeNull()
  })
})

describe('structuralKey', () => {
  it('cor não muda a matriz; conteúdo, logo, margem e nível mudam', () => {
    const base = structuralKey(withLogo)
    expect(structuralKey({ ...withLogo, darkColor: '#FF0000', lightColor: '#00FF00' })).toBe(base)
    expect(structuralKey({ ...withLogo, content: 'outro' })).not.toBe(base)
    expect(structuralKey({ ...withLogo, quietZoneModules: 2 })).not.toBe(base)
    expect(structuralKey({ ...withLogo, errorCorrection: 'H' })).not.toBe(base)
    expect(structuralKey({ ...withLogo, logo: { ...withLogo.logo!, centerXPercent: 51 } })).not.toBe(base)
  })

  it('forma e degradê são cosméticos: não pedem matriz nova', () => {
    const styled = { ...withLogo, moduleShape: 'DOTS' as const, eyeShape: 'CIRCLE' as const, gradientEnabled: true }
    expect(structuralKey(styled)).toBe(structuralKey(withLogo))
  })

  it('trocar só a imagem do logo não muda a matriz', () => {
    expect(structuralKey({ ...withLogo, logo: { ...withLogo.logo!, imageDataUrl: 'data:x' } })).toBe(structuralKey(withLogo))
  })
})

describe('toRequest', () => {
  it('só manda a imagem do logo quando pedido (no export)', () => {
    expect(toRequest(withLogo).style.logo).not.toHaveProperty('imageDataUrl')
    expect(toRequest(withLogo, true).style.logo?.imageDataUrl).toBe('data:image/png;base64,AAAA')
  })

  it('o degradê só vai quando está ligado, e desligar guarda a escolha', () => {
    expect(toRequest(initialDesign).style).not.toHaveProperty('gradient')
    const on = designReducer(designReducer(initialDesign, { type: 'setGradientEnabled', value: true }), {
      type: 'setGradient',
      value: { endColor: '#FF00AA', type: 'RADIAL' },
    })
    expect(toRequest(on).style.gradient).toEqual({ type: 'RADIAL', endColor: '#FF00AA', angleDegrees: 45 })

    const offAgain = designReducer(designReducer(on, { type: 'setGradientEnabled', value: false }), { type: 'setGradientEnabled', value: true })
    expect(offAgain.gradient.endColor).toBe('#FF00AA')
  })

  it('a forma vai sempre (o export precisa dela)', () => {
    const dots = designReducer(initialDesign, { type: 'setModuleShape', value: 'DOTS' })
    expect(toRequest(dots).style).toMatchObject({ moduleShape: 'DOTS', eyeShape: 'SQUARE' })
  })

  it('sem logo não manda o campo', () => {
    expect(toRequest(initialDesign).style).not.toHaveProperty('logo')
  })
})

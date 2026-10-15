import type { ErrorCorrection, EyeShape, GradientType, ModuleShape, QrRequest } from '../api/types'

export interface LogoDesign {
  /** data URL da imagem (PNG ou JPEG) — desenhada no canvas e enviada só no export. */
  imageDataUrl: string | null
  fileName: string | null
  widthPercent: number
  centerXPercent: number
  centerYPercent: number
}

export interface GradientDesign {
  type: GradientType
  endColor: string
  angleDegrees: number
}

export interface QrDesign {
  content: string
  darkColor: string
  lightColor: string
  logo: LogoDesign | null
  quietZoneModules: number
  errorCorrection: ErrorCorrection
  moduleShape: ModuleShape
  eyeShape: EyeShape
  /** Desligar o degradê guarda a escolha: religar volta com a mesma cor e ângulo. */
  gradientEnabled: boolean
  gradient: GradientDesign
}

export const DEFAULT_LOGO_WIDTH = 20

export const initialDesign: QrDesign = {
  content: 'https://github.com/merlinmec/mec-qrsafe',
  darkColor: '#1A1A2E',
  lightColor: '#FFFFFF',
  logo: null,
  quietZoneModules: 4,
  errorCorrection: 'AUTO',
  moduleShape: 'SQUARE',
  eyeShape: 'SQUARE',
  gradientEnabled: false,
  gradient: { type: 'LINEAR', endColor: '#8A5A00', angleDegrees: 45 },
}

export type DesignAction =
  | { type: 'setContent'; value: string }
  | { type: 'setDarkColor'; value: string }
  | { type: 'setLightColor'; value: string }
  | { type: 'swapColors' }
  | { type: 'setLogoImage'; imageDataUrl: string; fileName: string }
  | { type: 'setLogoGeometry'; widthPercent?: number; centerXPercent?: number; centerYPercent?: number }
  | { type: 'centerLogo' }
  | { type: 'removeLogo' }
  | { type: 'setQuietZone'; value: number }
  | { type: 'setErrorCorrection'; value: ErrorCorrection }
  | { type: 'setModuleShape'; value: ModuleShape }
  | { type: 'setEyeShape'; value: EyeShape }
  | { type: 'setGradientEnabled'; value: boolean }
  | { type: 'setGradient'; value: Partial<GradientDesign> }

const clampPercent = (value: number) => Math.min(100, Math.max(0, value))

export function designReducer(state: QrDesign, action: DesignAction): QrDesign {
  switch (action.type) {
    case 'setContent':
      return { ...state, content: action.value }
    case 'setDarkColor':
      return { ...state, darkColor: action.value }
    case 'setLightColor':
      return { ...state, lightColor: action.value }
    case 'swapColors':
      return { ...state, darkColor: state.lightColor, lightColor: state.darkColor }
    case 'setLogoImage':
      return {
        ...state,
        logo: {
          widthPercent: state.logo?.widthPercent ?? DEFAULT_LOGO_WIDTH,
          centerXPercent: state.logo?.centerXPercent ?? 50,
          centerYPercent: state.logo?.centerYPercent ?? 50,
          imageDataUrl: action.imageDataUrl,
          fileName: action.fileName,
        },
      }
    case 'setLogoGeometry': {
      if (!state.logo) return state
      return {
        ...state,
        logo: {
          ...state.logo,
          widthPercent: action.widthPercent ?? state.logo.widthPercent,
          centerXPercent: clampPercent(action.centerXPercent ?? state.logo.centerXPercent),
          centerYPercent: clampPercent(action.centerYPercent ?? state.logo.centerYPercent),
        },
      }
    }
    case 'centerLogo':
      return state.logo ? { ...state, logo: { ...state.logo, centerXPercent: 50, centerYPercent: 50 } } : state
    case 'removeLogo':
      return { ...state, logo: null }
    case 'setQuietZone':
      return { ...state, quietZoneModules: action.value }
    case 'setErrorCorrection':
      return { ...state, errorCorrection: action.value }
    case 'setModuleShape':
      return { ...state, moduleShape: action.value }
    case 'setEyeShape':
      return { ...state, eyeShape: action.value }
    case 'setGradientEnabled':
      return { ...state, gradientEnabled: action.value }
    case 'setGradient':
      return { ...state, gradient: { ...state.gradient, ...action.value } }
  }
}

/**
 * O que muda a MATRIZ (e por isso precisa de um /preview novo): conteúdo,
 * geometria do logo, margem e nível. Cor é cosmética — repinta no canvas e
 * só pede um /validate, que é mais leve (seção 8).
 */
export function structuralKey(design: QrDesign): string {
  const logo = design.logo
  return JSON.stringify([
    design.content,
    design.quietZoneModules,
    design.errorCorrection,
    logo ? [logo.widthPercent, logo.centerXPercent, logo.centerYPercent] : null,
  ])
}

/** O corpo da API. A imagem do logo só vai quando [withLogoImage] (no export). */
export function toRequest(design: QrDesign, withLogoImage = false): QrRequest {
  const logo = design.logo
  return {
    content: { type: 'STATIC', value: design.content },
    style: {
      darkColor: design.darkColor,
      lightColor: design.lightColor,
      quietZoneModules: design.quietZoneModules,
      moduleShape: design.moduleShape,
      eyeShape: design.eyeShape,
      ...(design.gradientEnabled && { gradient: { ...design.gradient } }),
      ...(logo && {
        logo: {
          widthPercent: logo.widthPercent,
          centerXPercent: round2(logo.centerXPercent),
          centerYPercent: round2(logo.centerYPercent),
          ...(withLogoImage && logo.imageDataUrl ? { imageDataUrl: logo.imageDataUrl } : {}),
        },
      }),
    },
    errorCorrection: design.errorCorrection,
  }
}

const round2 = (value: number) => Math.round(value * 100) / 100

// Contrato da API do qr-api (seções 6 e 16 de DECISOES-E-TRADEOFFS.txt).
// Os três endpoints recebem o mesmo corpo.

export type ErrorCorrection = 'AUTO' | 'L' | 'M' | 'Q' | 'H'
export type Level = Exclude<ErrorCorrection, 'AUTO'>
export type Severity = 'BLOCKING' | 'WARNING'

export interface LogoRequest {
  widthPercent: number
  centerXPercent: number
  centerYPercent: number
  /** Só o /export usa; o preview desenha o logo no próprio browser. */
  imageDataUrl?: string
}

export type ModuleShape = 'SQUARE' | 'ROUNDED' | 'DOTS'
export type EyeShape = 'SQUARE' | 'ROUNDED' | 'CIRCLE'
export type GradientType = 'LINEAR' | 'RADIAL'

export interface GradientRequest {
  type: GradientType
  endColor: string
  /** Só no linear: 0 = da esquerda pra direita, 90 = de cima pra baixo. */
  angleDegrees: number
}

export interface QrRequest {
  content: { type: 'STATIC'; value: string }
  style: {
    darkColor: string
    lightColor: string
    logo?: LogoRequest
    quietZoneModules: number
    moduleShape: ModuleShape
    eyeShape: EyeShape
    gradient?: GradientRequest
  }
  errorCorrection: ErrorCorrection
}

export interface Violation {
  rule: 'contrast' | 'function_pattern_overlap' | 'quiet_zone' | 'logo_coverage' | (string & {})
  severity: Severity
  message: string
}

export interface ValidationResponse {
  score: number
  passed: boolean
  violations: Violation[]
  errorCorrectionLevel: Level
  version: number
}

export interface MatrixResponse {
  size: number
  quietZone: number
  /** Uma string por linha; letra = tipo do módulo, maiúscula = escuro. */
  rows: string[]
}

export interface LogoBox {
  left: number
  top: number
  side: number
}

export interface EngineeringResponse {
  mask: number
  symbolSize: number
  blockCount: number
  correctableErrorsPerBlock: number
  destroyedPerBlock: number[]
  codewordBlocks: number[]
  destroyedCodewords: number[]
  /** Codeword de cada módulo, nas coordenadas de `rows`; -1 = sem codeword. */
  codewordRows: number[][]
}

export interface PreviewResponse {
  validation: ValidationResponse
  matrix: MatrixResponse
  logoBox: LogoBox | null
  engineering: EngineeringResponse
}

export interface ApiErrorBody {
  error: string
  message: string
  validation?: ValidationResponse
}

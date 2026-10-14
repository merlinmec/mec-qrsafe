import type { LogoBox } from '../api/types'
import type { QrDesign } from '../design/state'

/**
 * Caixa do logo em módulos, nas coordenadas da matriz. É a mesma conta do
 * LogoFootprint no qr-validation; aqui serve só pra desenhar na hora em que
 * o usuário arrasta ou redimensiona, antes do /preview responder. Quando a
 * matriz está em dia, vale a caixa que o servidor devolveu.
 */
export function localLogoBox(design: QrDesign, matrixSize: number, quietZone: number): LogoBox | null {
  const logo = design.logo
  if (!logo) return null
  const symbol = matrixSize - 2 * quietZone
  const side = (logo.widthPercent / 100) * symbol
  return {
    left: quietZone + (logo.centerXPercent / 100) * symbol - side / 2,
    top: quietZone + (logo.centerYPercent / 100) * symbol - side / 2,
    side,
  }
}

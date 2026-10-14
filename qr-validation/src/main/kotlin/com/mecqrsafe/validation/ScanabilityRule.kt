package com.mecqrsafe.validation

import com.mecqrsafe.qrengine.QrCode

/**
 * Cada regra é um objeto independente (ver seção 5 de
 * `DECISOES-E-TRADEOFFS.txt`) porque cada uma tem ciclo de calibração
 * próprio — contraste é threshold ajustável empiricamente; overlap de
 * padrão fixo é geometria fixa da spec. Separar evita que ajustar uma
 * regra afete as outras sem querer.
 *
 * Recebe o [QrCode] inteiro, não só a matriz: a regra de logo precisa do
 * nível de correção e do mapa de codewords, a de margem precisa da quiet
 * zone.
 */
fun interface ScanabilityRule {
    fun evaluate(qr: QrCode, style: QrStyle): Violation?
}

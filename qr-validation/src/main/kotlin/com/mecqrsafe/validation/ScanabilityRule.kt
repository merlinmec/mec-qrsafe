package com.mecqrsafe.validation

import com.mecqrsafe.qrengine.matrix.QrMatrix

/**
 * Cada regra é um objeto independente (ver seção 5 de
 * `DECISOES-E-TRADEOFFS.txt`) porque cada uma tem ciclo de calibração
 * próprio — contraste é threshold ajustável empiricamente; overlap de
 * finder pattern é geometria fixa da spec. Separar evita que ajustar uma
 * regra afete as outras sem querer.
 */
fun interface ScanabilityRule {
    fun evaluate(matrix: QrMatrix, style: QrStyle): Violation?
}

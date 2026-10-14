package com.mecqrsafe.validation.rules

import com.mecqrsafe.qrengine.matrix.QrMatrix
import com.mecqrsafe.validation.QrStyle
import com.mecqrsafe.validation.ScanabilityRule
import com.mecqrsafe.validation.Violation

/**
 * Margem mínima de 4 módulos ao redor do QR sem nenhum elemento — hard
 * block, não ajustável por correção de erro (ver seção 5 de
 * `DECISOES-E-TRADEOFFS.txt`).
 *
 * TODO: o `QrEncoder` já entrega a matriz com a quiet zone marcada como
 * `ModuleType.QUIET_ZONE`. Falta o lado do estilo: hoje [QrStyle] não tem
 * nada que possa invadir a margem (margem configurável, moldura, logo
 * fora do centro), então ainda não há o que esta regra reprove.
 */
class QuietZoneRule : ScanabilityRule {
    override fun evaluate(matrix: QrMatrix, style: QrStyle): Violation? {
        TODO(
            "Falta o QrStyle modelar o que pode invadir a quiet zone " +
                "(margem, moldura, logo fora do centro). Ver TODO acima."
        )
    }
}

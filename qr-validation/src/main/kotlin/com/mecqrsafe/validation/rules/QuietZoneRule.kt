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
 * TODO: depende de `QrEncoder` já posicionar módulos `ModuleType.QUIET_ZONE`
 * na matriz (ainda não implementado — ver TODOs em `QrEncoder.kt`). Sem
 * isso, não há o que checar aqui além de "a quiet zone existe".
 */
class QuietZoneRule : ScanabilityRule {
    override fun evaluate(matrix: QrMatrix, style: QrStyle): Violation? {
        TODO(
            "Depende de QrEncoder posicionar ModuleType.QUIET_ZONE na matriz " +
                "primeiro. Ver TODOs em QrEncoder.kt."
        )
    }
}

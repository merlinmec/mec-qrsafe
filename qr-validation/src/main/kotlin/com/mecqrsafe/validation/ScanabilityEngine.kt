package com.mecqrsafe.validation

import com.mecqrsafe.qrengine.QrCode
import com.mecqrsafe.validation.rules.ContrastRule
import com.mecqrsafe.validation.rules.FunctionPatternOverlapRule
import com.mecqrsafe.validation.rules.LogoCoverageRule
import com.mecqrsafe.validation.rules.QuietZoneRule

/**
 * Orquestra as [ScanabilityRule]s e agrega o resultado num
 * [ScanabilityReport]. Chamado de forma SEMPRE síncrona pela API — não
 * há caso de uso pra fila/worker aqui, o cálculo é aritmética pura sobre
 * uma matriz de no máximo ~177×177 módulos (ver seção 5 de
 * `DECISOES-E-TRADEOFFS.txt`).
 */
class ScanabilityEngine(
    private val rules: List<ScanabilityRule> = listOf(
        ContrastRule(),
        FunctionPatternOverlapRule(),
        QuietZoneRule(),
        LogoCoverageRule(),
    ),
) {
    fun evaluate(qr: QrCode, style: QrStyle): ScanabilityReport {
        val violations = rules.mapNotNull { it.evaluate(qr, style) }
        // Score só pra UI mostrar "quase lá" — quem decide se exporta é
        // ScanabilityReport.passed. Bloqueio pesa mais que aviso; os pesos
        // são de partida, ajustar quando houver teste de escaneamento real.
        val penalty = violations.sumOf { if (it.severity == Severity.BLOCKING) BLOCKING_WEIGHT else WARNING_WEIGHT }
        return ScanabilityReport((1.0 - penalty).coerceAtLeast(0.0), violations)
    }

    private companion object {
        const val BLOCKING_WEIGHT = 0.5
        const val WARNING_WEIGHT = 0.15
    }
}

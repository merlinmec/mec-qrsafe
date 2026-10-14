package com.mecqrsafe.validation

import com.mecqrsafe.qrengine.matrix.QrMatrix
import com.mecqrsafe.validation.rules.ContrastRule

/**
 * Orquestra as [ScanabilityRule]s e agrega o resultado num
 * [ScanabilityReport]. Chamado de forma SEMPRE síncrona pela API — não
 * há caso de uso pra fila/worker aqui, o cálculo é aritmética pura sobre
 * uma matriz de no máximo ~177×177 módulos (ver seção 5 de
 * `DECISOES-E-TRADEOFFS.txt`).
 *
 * TODO: só [ContrastRule] está ativa. `FinderPatternOverlapRule` e
 * `QuietZoneRule` ainda lançam `TODO()` internamente (o encoder já marca
 * finder e quiet zone na matriz; falta modelar a geometria do logo e a
 * margem no QrStyle) — NÃO adicione elas na lista `rules` abaixo até
 * estarem implementadas de verdade, ou toda chamada a `evaluate` vai
 * quebrar.
 */
class ScanabilityEngine(
    private val rules: List<ScanabilityRule> = listOf(
        ContrastRule(),
    ),
) {
    fun evaluate(matrix: QrMatrix, style: QrStyle): ScanabilityReport {
        val violations = rules.mapNotNull { it.evaluate(matrix, style) }
        val score = if (violations.isEmpty()) 1.0 else {
            // Placeholder simples: 1 ponto perdido por violação, piso em 0.
            // TODO: ponderar por severidade (BLOCKING deveria pesar mais
            // que WARNING) assim que houver mais de uma regra ativa.
            (1.0 - violations.size * 0.25).coerceAtLeast(0.0)
        }
        return ScanabilityReport(score, violations)
    }
}

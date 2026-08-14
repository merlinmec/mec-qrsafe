package com.mecqrsafe.validation

/**
 * BLOCKING = impede exportação (ver seção 5 de `DECISOES-E-TRADEOFFS.txt`
 * — cobertura de finder pattern e quiet zone são sempre BLOCKING, não
 * dependem de threshold). WARNING = exportável, mas o usuário deveria
 * saber que está arriscando.
 */
enum class Severity { BLOCKING, WARNING }

data class Violation(
    val rule: String,
    val severity: Severity,
    val message: String,
)

/**
 * Resultado agregado do [ScanabilityEngine] — não um booleano seco,
 * porque a UI precisa poder dizer "quase lá, ajuste X" em vez de só
 * sim/não (ver seção 5 de `DECISOES-E-TRADEOFFS.txt`).
 */
data class ScanabilityReport(
    val score: Double,
    val violations: List<Violation>,
) {
    val passed: Boolean
        get() = violations.none { it.severity == Severity.BLOCKING }
}

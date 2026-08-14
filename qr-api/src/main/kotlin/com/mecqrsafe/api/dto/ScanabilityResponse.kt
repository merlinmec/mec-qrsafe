package com.mecqrsafe.api.dto

import com.mecqrsafe.validation.ScanabilityReport
import com.mecqrsafe.validation.Severity

data class ViolationResponse(
    val rule: String,
    val severity: Severity,
    val message: String,
)

data class ScanabilityResponse(
    val score: Double,
    val passed: Boolean,
    val violations: List<ViolationResponse>,
) {
    companion object {
        fun from(report: ScanabilityReport): ScanabilityResponse = ScanabilityResponse(
            score = report.score,
            passed = report.passed,
            violations = report.violations.map { ViolationResponse(it.rule, it.severity, it.message) },
        )
    }
}

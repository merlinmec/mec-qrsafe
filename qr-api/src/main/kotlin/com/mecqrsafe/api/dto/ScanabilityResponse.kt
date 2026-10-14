package com.mecqrsafe.api.dto

import com.mecqrsafe.api.service.ValidationResult
import com.mecqrsafe.qrengine.ErrorCorrectionLevel
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
    /** O nível em que o QR foi validado — no AUTO, o que o advisor escolheu por causa do logo. */
    val errorCorrectionLevel: ErrorCorrectionLevel,
    val version: Int,
) {
    companion object {
        fun from(result: ValidationResult): ScanabilityResponse = ScanabilityResponse(
            score = result.report.score,
            passed = result.report.passed,
            violations = result.report.violations.map { ViolationResponse(it.rule, it.severity, it.message) },
            errorCorrectionLevel = result.qr.errorCorrectionLevel,
            version = result.qr.version,
        )
    }
}

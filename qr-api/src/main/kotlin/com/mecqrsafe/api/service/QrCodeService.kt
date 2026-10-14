package com.mecqrsafe.api.service

import com.mecqrsafe.api.dto.ErrorCorrectionRequest
import com.mecqrsafe.api.dto.QrStyleRequest
import com.mecqrsafe.api.dto.ValidateRequest
import com.mecqrsafe.qrengine.ErrorCorrectionLevel
import com.mecqrsafe.qrengine.QrCode
import com.mecqrsafe.qrengine.QrEncoder
import com.mecqrsafe.validation.Color
import com.mecqrsafe.validation.ErrorCorrectionAdvisor
import com.mecqrsafe.validation.LogoStyle
import com.mecqrsafe.validation.QrStyle
import com.mecqrsafe.validation.ScanabilityEngine
import com.mecqrsafe.validation.ScanabilityReport
import org.springframework.stereotype.Service

/** O QR que foi validado e o resultado — a API devolve os dois (nível e versão escolhidos interessam à UI). */
data class ValidationResult(val qr: QrCode, val report: ScanabilityReport)

/**
 * Orquestra qr-engine + qr-validation — sem regra de negócio de QR aqui
 * (isso mora nos módulos puros), só tradução DTO <-> domínio (ver seção 1
 * de `DECISOES-E-TRADEOFFS.txt`: "qr-api ... não tem regra de negócio de
 * QR, só chama os módulos acima").
 */
@Service
class QrCodeService(
    private val scanabilityEngine: ScanabilityEngine = ScanabilityEngine(),
    private val errorCorrectionAdvisor: ErrorCorrectionAdvisor = ErrorCorrectionAdvisor(),
) {

    fun validate(request: ValidateRequest): ValidationResult {
        require(request.content.type == "STATIC") {
            "v1 só suporta content.type = STATIC"
        }

        val style = toQrStyle(request.style)
        // AUTO: o advisor sobe a correção ANTES da validação (seção 5).
        // Nível fixo: o usuário escolheu, a validação diz se o logo cabe nele.
        val qr = when (val level = request.errorCorrection) {
            ErrorCorrectionRequest.AUTO -> errorCorrectionAdvisor.encode(request.content.value, style)
            else -> QrEncoder.encode(request.content.value, ErrorCorrectionLevel.valueOf(level.name), style.quietZoneModules)
        }

        return ValidationResult(qr, scanabilityEngine.evaluate(qr, style))
    }

    private fun toQrStyle(style: QrStyleRequest): QrStyle = QrStyle(
        darkModuleColor = parseHexColor(style.darkColor),
        lightModuleColor = parseHexColor(style.lightColor),
        logo = style.logo?.let { LogoStyle(it.widthPercent, it.centerXPercent, it.centerYPercent) },
        quietZoneModules = style.quietZoneModules,
    )

    private fun parseHexColor(hex: String): Color {
        val clean = hex.removePrefix("#")
        return Color(
            red = clean.substring(0, 2).toInt(16),
            green = clean.substring(2, 4).toInt(16),
            blue = clean.substring(4, 6).toInt(16),
        )
    }
}

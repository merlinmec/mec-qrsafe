package com.mecqrsafe.api.service

import com.mecqrsafe.api.dto.QrStyleRequest
import com.mecqrsafe.api.dto.ValidateRequest
import com.mecqrsafe.qrengine.ErrorCorrectionLevel
import com.mecqrsafe.qrengine.QrEncoder
import com.mecqrsafe.validation.Color
import com.mecqrsafe.validation.QrStyle
import com.mecqrsafe.validation.LogoStyle
import com.mecqrsafe.validation.ScanabilityEngine
import com.mecqrsafe.validation.ScanabilityReport
import org.springframework.stereotype.Service

/**
 * Orquestra qr-engine + qr-validation — sem regra de negócio de QR aqui
 * (isso mora nos módulos puros), só tradução DTO <-> domínio (ver seção 1
 * de `DECISOES-E-TRADEOFFS.txt`: "qr-api ... não tem regra de negócio de
 * QR, só chama os módulos acima").
 */
@Service
class QrCodeService(
    private val scanabilityEngine: ScanabilityEngine = ScanabilityEngine(),
) {

    fun validate(request: ValidateRequest): ScanabilityReport {
        require(request.content.type == "STATIC") {
            "v1 só suporta content.type = STATIC"
        }

        val level = when (request.errorCorrection.name) {
            "AUTO" -> ErrorCorrectionLevel.M // TODO: usar ErrorCorrectionAdvisor com o logo real
            else -> ErrorCorrectionLevel.valueOf(request.errorCorrection.name)
        }

        val matrix = QrEncoder.encode(request.content.value, level).matrix

        return scanabilityEngine.evaluate(matrix, toQrStyle(request.style))
    }

    private fun toQrStyle(style: QrStyleRequest): QrStyle = QrStyle(
        darkModuleColor = parseHexColor(style.darkColor),
        lightModuleColor = parseHexColor(style.lightColor),
        logo = style.logoSizePercent?.let { LogoStyle(sizePercentOfMatrix = it) },
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

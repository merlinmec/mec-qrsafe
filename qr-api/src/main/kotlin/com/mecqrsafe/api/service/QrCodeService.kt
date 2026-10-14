package com.mecqrsafe.api.service

import com.mecqrsafe.api.dto.ErrorCorrectionRequest
import com.mecqrsafe.api.dto.QrStyleRequest
import com.mecqrsafe.api.dto.ValidateRequest
import com.mecqrsafe.qrengine.ErrorCorrectionLevel
import com.mecqrsafe.qrengine.QrCode
import com.mecqrsafe.qrengine.QrEncoder
import com.mecqrsafe.render.LogoBox
import com.mecqrsafe.render.LogoImage
import com.mecqrsafe.render.PngRenderer
import com.mecqrsafe.render.RenderOptions
import com.mecqrsafe.render.SvgRenderer
import com.mecqrsafe.validation.Color
import com.mecqrsafe.validation.ErrorCorrectionAdvisor
import com.mecqrsafe.validation.LogoFootprint
import com.mecqrsafe.validation.LogoStyle
import com.mecqrsafe.validation.QrStyle
import com.mecqrsafe.validation.ScanabilityEngine
import com.mecqrsafe.validation.ScanabilityReport
import org.springframework.stereotype.Service

/** O QR que foi validado, o resultado e onde o logo caiu nele. */
data class ValidationResult(
    val qr: QrCode,
    val report: ScanabilityReport,
    val logoFootprint: LogoFootprint?,
)

enum class ExportFormat(val contentType: String, val extension: String) {
    SVG("image/svg+xml", "svg"),
    PNG("image/png", "png"),
    ;

    companion object {
        fun parse(value: String): ExportFormat = entries.firstOrNull { it.extension == value.lowercase() }
            ?: throw IllegalArgumentException("format precisa ser svg ou png, veio \"$value\"")
    }
}

class ExportedFile(val bytes: ByteArray, val format: ExportFormat)

/** O export foi recusado porque o QR não passou na validação — leva o resultado pra API mostrar o motivo. */
class NotScannableException(val result: ValidationResult) : RuntimeException("o QR não passou na validação")

/**
 * Orquestra qr-engine + qr-validation + qr-render — sem regra de negócio
 * de QR aqui (isso mora nos módulos puros), só tradução DTO <-> domínio
 * (ver seção 1 de `DECISOES-E-TRADEOFFS.txt`: "qr-api ... não tem regra
 * de negócio de QR, só chama os módulos acima").
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

        return ValidationResult(qr, scanabilityEngine.evaluate(qr, style), style.logo?.let { LogoFootprint(qr, it) })
    }

    /**
     * Gera o arquivo final, revalidando aqui mesmo — o servidor RECUSA
     * exportar o que não passa, independente do que a UI mostrou (seções 5
     * e 9). O arquivo sai da mesma matriz que acabou de ser validada.
     *
     * @param sizePx lado desejado do arquivo. No PNG o módulo é arredondado
     *   pra pixels inteiros, então o lado final pode ser um pouco menor.
     */
    fun export(request: ValidateRequest, format: ExportFormat, sizePx: Int): ExportedFile {
        val result = validate(request)
        if (!result.report.passed) throw NotScannableException(result)

        val options = RenderOptions(
            darkColor = request.style.darkColor.uppercase(),
            lightColor = request.style.lightColor.uppercase(),
            logoBox = result.logoFootprint?.let { LogoBox(it.left, it.top, it.side) },
            logoImage = request.style.logo?.imageDataUrl?.let { LogoImage.fromDataUrl(it) },
        )
        val matrix = result.qr.matrix
        val bytes = when (format) {
            ExportFormat.SVG -> SvgRenderer.render(matrix, options, sizePx).toByteArray(Charsets.UTF_8)
            ExportFormat.PNG -> PngRenderer.render(matrix, options, PngRenderer.modulePxFor(matrix, sizePx))
        }
        return ExportedFile(bytes, format)
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

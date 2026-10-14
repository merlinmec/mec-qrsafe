package com.mecqrsafe.validation.rules

import com.mecqrsafe.qrengine.QrCode
import com.mecqrsafe.validation.Color
import com.mecqrsafe.validation.QrStyle
import com.mecqrsafe.validation.ScanabilityRule
import com.mecqrsafe.validation.Severity
import com.mecqrsafe.validation.Violation
import kotlin.math.pow

/**
 * Contraste de luminância entre a cor "escura" e "clara" do QR — falha
 * abaixo do limiar configurado. Ver seção 5 de
 * `DECISOES-E-TRADEOFFS.txt`: "começar com limiar permissivo (~4.5:1) e
 * apertar depois com dados reais de teste de escaneamento, em vez de
 * travar num número especulativo desde já".
 *
 * TODO: hoje só avalia a cor sólida configurada em [QrStyle]. Quando o
 * front suportar gradiente de verdade, isso precisa avaliar o PONTO DE
 * MENOR CONTRASTE do gradiente, não a cor base — ver nota na doc de
 * QrStyle.Color.
 */
class ContrastRule(
    private val minimumContrastRatio: Double = 4.5,
) : ScanabilityRule {

    override fun evaluate(qr: QrCode, style: QrStyle): Violation? {
        val ratio = contrastRatio(style.darkModuleColor, style.lightModuleColor)
        if (ratio >= minimumContrastRatio) return null

        return Violation(
            rule = "contrast",
            severity = Severity.BLOCKING,
            message = "Contraste entre módulo escuro e claro está em " +
                "%.2f:1, abaixo do mínimo de %.1f:1 — a câmera pode não ".format(ratio, minimumContrastRatio) +
                "conseguir distinguir os módulos em pouca luz.",
        )
    }

    /** Fórmula de contraste de luminância relativa (mesma base do WCAG). */
    private fun contrastRatio(a: Color, b: Color): Double {
        val luminanceA = relativeLuminance(a)
        val luminanceB = relativeLuminance(b)
        val lighter = maxOf(luminanceA, luminanceB)
        val darker = minOf(luminanceA, luminanceB)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(color: Color): Double {
        val r = linearize(color.red / 255.0)
        val g = linearize(color.green / 255.0)
        val b = linearize(color.blue / 255.0)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun linearize(channel: Double): Double =
        if (channel <= 0.03928) channel / 12.92 else ((channel + 0.055) / 1.055).pow(2.4)
}

package com.mecqrsafe.validation.rules

import com.mecqrsafe.qrengine.QrCode
import com.mecqrsafe.validation.Color
import com.mecqrsafe.validation.QrStyle
import com.mecqrsafe.validation.ScanabilityRule
import com.mecqrsafe.validation.Severity
import com.mecqrsafe.validation.Violation
import java.util.Locale
import kotlin.math.pow

/**
 * Contraste de luminância entre a cor "escura" e "clara" do QR — falha
 * abaixo do limiar configurado. Ver seção 5 de
 * `DECISOES-E-TRADEOFFS.txt`: "começar com limiar permissivo (~4.5:1) e
 * apertar depois com dados reais de teste de escaneamento, em vez de
 * travar num número especulativo desde já".
 *
 * Com degradê, vale o PONTO DE MENOR CONTRASTE do caminho entre as duas
 * cores, nunca a média nem só as pontas: a luminância é soma de três
 * canais que podem andar em sentidos opostos, então o pior ponto pode
 * cair no meio. O caminho é amostrado em [GRADIENT_SAMPLES] pontos — com
 * canais de 8 bits, a mistura muda no máximo 255 vezes, e a variação de
 * contraste entre amostras vizinhas fica muito abaixo da margem do limiar.
 */
class ContrastRule(
    private val minimumContrastRatio: Double = 4.5,
) : ScanabilityRule {

    override fun evaluate(qr: QrCode, style: QrStyle): Violation? {
        val (ratio, worst) = worstContrast(style)
        if (ratio >= minimumContrastRatio) return null

        val where = if (style.darkGradientEnd == null) "" else " no ponto mais claro do degradê (${hex(worst)})"
        return Violation(
            rule = "contrast",
            severity = Severity.BLOCKING,
            message = "Contraste entre módulo escuro e claro está em " +
                String.format(PT_BR, "%.2f:1%s, abaixo do mínimo de %.1f:1 — a câmera pode não ", ratio, where, minimumContrastRatio) +
                "conseguir distinguir os módulos em pouca luz.",
        )
    }

    /** O menor contraste ao longo dos módulos (cor sólida ou o caminho do degradê) e a cor onde ele ocorre. */
    private fun worstContrast(style: QrStyle): Pair<Double, Color> {
        val end = style.darkGradientEnd ?: return contrastRatio(style.darkModuleColor, style.lightModuleColor) to style.darkModuleColor
        return (0..GRADIENT_SAMPLES)
            .map { step -> style.darkModuleColor.mix(end, step.toDouble() / GRADIENT_SAMPLES) }
            .map { color -> contrastRatio(color, style.lightModuleColor) to color }
            .minBy { it.first }
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

    private fun hex(color: Color) = "#%02X%02X%02X".format(color.red, color.green, color.blue)

    private companion object {
        const val GRADIENT_SAMPLES = 256

        /** A mensagem é em português: "4,50:1" em qualquer servidor (o container da demo roda em inglês). */
        val PT_BR: Locale = Locale.of("pt", "BR")
    }
}

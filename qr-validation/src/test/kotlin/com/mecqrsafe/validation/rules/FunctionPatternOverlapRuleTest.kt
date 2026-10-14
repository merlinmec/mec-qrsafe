package com.mecqrsafe.validation.rules

import com.mecqrsafe.qrengine.ErrorCorrectionLevel
import com.mecqrsafe.qrengine.QrEncoder
import com.mecqrsafe.validation.Color
import com.mecqrsafe.validation.LogoStyle
import com.mecqrsafe.validation.QrStyle
import com.mecqrsafe.validation.Severity
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FunctionPatternOverlapRuleTest {

    /** Versão 7: tem version info, alignment no meio e símbolo de 45 módulos. */
    private val qr = QrEncoder.encode("a".repeat(120), ErrorCorrectionLevel.M)
    private val rule = FunctionPatternOverlapRule()

    private fun evaluate(logo: LogoStyle?) = rule.evaluate(qr, QrStyle(Color(0, 0, 0), Color(255, 255, 255), logo))

    @Test
    fun `sem logo nao ha o que reprovar`() {
        assertNull(evaluate(null))
    }

    @Test
    fun `logo centralizado em cima do alignment do meio passa`() {
        assertEquals(7, qr.version)
        assertNull(evaluate(LogoStyle(widthPercent = 25.0)))
    }

    @Test
    fun `logo no canto em cima do finder bloqueia`() {
        val violation = requireNotNull(evaluate(LogoStyle(widthPercent = 12.0, centerXPercent = 8.0, centerYPercent = 8.0)))

        assertEquals(Severity.BLOCKING, violation.severity)
        assertContains(violation.message, "quadrados dos cantos")
    }

    @Test
    fun `logo encostando na linha de sincronismo bloqueia mesmo longe dos cantos`() {
        // Timing horizontal na linha 6 do símbolo (≈14% de 45); logo de 6% centrado em 12% vai de 9% a 15%.
        // Em x = 35% (colunas 14-17), longe do alignment que a versão 7 tem na linha 6, coluna 22.
        val violation = requireNotNull(evaluate(LogoStyle(widthPercent = 6.0, centerXPercent = 35.0, centerYPercent = 12.0)))

        assertContains(violation.message, "linhas de sincronismo")
    }

    @Test
    fun `basta encostar numa beirada pra bloquear`() {
        // Finder de cima-esquerda + separador vão até a coluna 8 do símbolo (8/45 ≈ 17.8%).
        val beirada = LogoStyle(widthPercent = 10.0, centerXPercent = 17.8 + 5.0 - 0.1, centerYPercent = 8.0)
        val folga = beirada.copy(centerXPercent = 17.8 + 5.0 + 0.5)

        assertEquals(Severity.BLOCKING, evaluate(beirada)?.severity)
        assertNull(evaluate(folga)?.takeIf { "quadrados dos cantos" in it.message })
    }
}

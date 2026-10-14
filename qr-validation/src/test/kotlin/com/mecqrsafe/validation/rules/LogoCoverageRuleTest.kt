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

/**
 * Os casos de borda da regra. Que o limite dela é o de um decodificador
 * de verdade está em [com.mecqrsafe.validation.LogoRulesAgainstReaderTest].
 */
class LogoCoverageRuleTest {

    private val content = "https://exemplo.com.br/cardapio/restaurante-do-joao?mesa=12"
    private val rule = LogoCoverageRule()

    private fun evaluate(level: ErrorCorrectionLevel, width: Double?) = rule.evaluate(
        QrEncoder.encode(content, level),
        QrStyle(Color(0, 0, 0), Color(255, 255, 255), width?.let { LogoStyle(widthPercent = it) }),
    )

    /** Menor largura (em passos de 1%) em que a regra devolve [severity] no nível dado. */
    private fun firstWidthWith(level: ErrorCorrectionLevel, severity: Severity): Double =
        (1..60).map { it.toDouble() }.first { evaluate(level, it)?.severity == severity }

    @Test
    fun `sem logo nao ha o que reprovar`() {
        assertNull(evaluate(ErrorCorrectionLevel.L, null))
    }

    @Test
    fun `logo pequeno passa em qualquer nivel`() {
        for (level in ErrorCorrectionLevel.entries) assertNull(evaluate(level, 5.0), "nível $level")
    }

    @Test
    fun `logo grande demais bloqueia dizendo o nivel e a capacidade`() {
        val violation = requireNotNull(evaluate(ErrorCorrectionLevel.M, 40.0))

        assertEquals(Severity.BLOCKING, violation.severity)
        assertContains(violation.message, "nível M")
    }

    @Test
    fun `antes de bloquear avisa que a margem acabou`() {
        val warning = firstWidthWith(ErrorCorrectionLevel.H, Severity.WARNING)
        val blocking = firstWidthWith(ErrorCorrectionLevel.H, Severity.BLOCKING)

        assert(warning < blocking) { "aviso em $warning% deveria vir antes do bloqueio em $blocking%" }
    }

    @Test
    fun `mais correcao aguenta logo maior`() {
        val limits = ErrorCorrectionLevel.entries.map { firstWidthWith(it, Severity.BLOCKING) }

        assertEquals(limits.sorted(), limits, "limites por nível L, M, Q, H: $limits")
        assert(limits.first() < limits.last()) { "H deveria aguentar mais que L: $limits" }
    }
}

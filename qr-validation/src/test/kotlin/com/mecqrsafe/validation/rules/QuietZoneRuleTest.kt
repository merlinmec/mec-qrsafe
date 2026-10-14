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

class QuietZoneRuleTest {

    private val rule = QuietZoneRule()

    private fun evaluate(quietZone: Int, logo: LogoStyle? = null) = rule.evaluate(
        QrEncoder.encode("margem", ErrorCorrectionLevel.M, quietZone),
        QrStyle(Color(0, 0, 0), Color(255, 255, 255), logo, quietZoneModules = quietZone),
    )

    @Test
    fun `margem de 4 modulos ou mais passa`() {
        assertNull(evaluate(4))
        assertNull(evaluate(10))
    }

    @Test
    fun `margem menor que 4 bloqueia dizendo quanto tem`() {
        val violation = requireNotNull(evaluate(2))

        assertEquals(Severity.BLOCKING, violation.severity)
        assertContains(violation.message, "2 módulo(s)")
    }

    @Test
    fun `logo que sai do simbolo invade a margem e bloqueia`() {
        val violation = requireNotNull(evaluate(4, LogoStyle(widthPercent = 20.0, centerXPercent = 95.0)))

        assertContains(violation.message, "invade a margem")
    }

    @Test
    fun `logo inteiro dentro do simbolo nao mexe na margem`() {
        assertNull(evaluate(4, LogoStyle(widthPercent = 20.0, centerXPercent = 89.9)))
    }
}

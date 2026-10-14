package com.mecqrsafe.validation.rules

import com.mecqrsafe.qrengine.ErrorCorrectionLevel
import com.mecqrsafe.qrengine.QrEncoder
import com.mecqrsafe.validation.Color
import com.mecqrsafe.validation.QrStyle
import com.mecqrsafe.validation.Severity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ContrastRuleTest {

    private val qr = QrEncoder.encode("contraste", ErrorCorrectionLevel.M)

    @Test
    fun `preto sobre branco passa facil`() {
        val style = QrStyle(
            darkModuleColor = Color(0, 0, 0),
            lightModuleColor = Color(255, 255, 255),
        )
        assertNull(ContrastRule().evaluate(qr, style))
    }

    @Test
    fun `cinza sobre cinza falha`() {
        val style = QrStyle(
            darkModuleColor = Color(150, 150, 150),
            lightModuleColor = Color(200, 200, 200),
        )
        val violation = ContrastRule().evaluate(qr, style)
        requireNotNull(violation)
        assertEquals(Severity.BLOCKING, violation.severity)
    }
}

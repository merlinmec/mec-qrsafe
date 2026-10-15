package com.mecqrsafe.validation.rules

import com.mecqrsafe.qrengine.ErrorCorrectionLevel
import com.mecqrsafe.qrengine.QrEncoder
import com.mecqrsafe.validation.Color
import com.mecqrsafe.validation.QrStyle
import com.mecqrsafe.validation.Severity
import kotlin.test.Test
import kotlin.test.assertContains
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

    @Test
    fun `mensagem usa virgula decimal mesmo num servidor em ingles`() {
        val original = java.util.Locale.getDefault()
        java.util.Locale.setDefault(java.util.Locale.US) // o container da demo no Render roda assim
        try {
            val violation = requireNotNull(ContrastRule().evaluate(qr, QrStyle(Color(150, 150, 150), Color(200, 200, 200))))
            assertContains(violation.message, "1,77:1")
            assertContains(violation.message, "mínimo de 4,5:1")
        } finally {
            java.util.Locale.setDefault(original)
        }
    }

    @Test
    fun `degrade escuro sobre branco passa`() {
        val style = QrStyle(Color(0x1A, 0x1A, 0x2E), Color(255, 255, 255), darkGradientEnd = Color(0x6D, 0x28, 0xD9))
        assertNull(ContrastRule().evaluate(qr, style))
    }

    @Test
    fun `degrade que clareia demais reprova pela ponta clara, nao pela media`() {
        // Preto passa fácil, mas o degradê termina num cinza claro: a média daria ok, a ponta não.
        val style = QrStyle(Color(0, 0, 0), Color(255, 255, 255), darkGradientEnd = Color(200, 200, 200))
        val violation = requireNotNull(ContrastRule().evaluate(qr, style))

        assertContains(violation.message, "degradê")
        assertContains(violation.message, "#C8C8C8")
    }

    @Test
    fun `o pior ponto pode ficar no meio do degrade, e e ele que vale`() {
        // QR invertido (módulos claros sobre fundo preto). Vermelho e verde passam sozinhos
        // (5,3:1 e 6,8:1), mas a mistura no meio, um marrom, fica em ~3,2:1.
        val style = QrStyle(Color(255, 0, 0), Color(0, 0, 0), darkGradientEnd = Color(0, 170, 0))
        assertNull(ContrastRule().evaluate(qr, QrStyle(Color(255, 0, 0), Color(0, 0, 0))), "vermelho sozinho passa")
        assertNull(ContrastRule().evaluate(qr, QrStyle(Color(0, 170, 0), Color(0, 0, 0))), "verde sozinho passa")

        assertEquals(Severity.BLOCKING, ContrastRule().evaluate(qr, style)?.severity)
    }
}

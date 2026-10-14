package com.mecqrsafe.validation

import com.mecqrsafe.qrengine.ErrorCorrectionLevel
import com.mecqrsafe.qrengine.QrEncoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScanabilityEngineTest {

    private val black = Color(0, 0, 0)
    private val white = Color(255, 255, 255)
    private val engine = ScanabilityEngine()

    @Test
    fun `QR limpo passa com score cheio`() {
        val report = engine.evaluate(QrEncoder.encode("limpo", ErrorCorrectionLevel.M), QrStyle(black, white))

        assertTrue(report.passed)
        assertEquals(1.0, report.score)
    }

    @Test
    fun `logo valido no nivel escolhido pelo advisor passa`() {
        val style = QrStyle(black, white, LogoStyle(widthPercent = 20.0))
        val report = engine.evaluate(ErrorCorrectionAdvisor().encode("https://exemplo.com", style), style)

        assertTrue(report.passed, "violações: ${report.violations}")
    }

    @Test
    fun `junta as violacoes de todas as regras e bloqueio pesa mais que aviso`() {
        val style = QrStyle(Color(150, 150, 150), Color(200, 200, 200), LogoStyle(widthPercent = 12.0, centerXPercent = 8.0, centerYPercent = 8.0), quietZoneModules = 1)
        val report = engine.evaluate(QrEncoder.encode("tudo errado", ErrorCorrectionLevel.M, 1), style)

        assertFalse(report.passed)
        assertEquals(setOf("contrast", "function_pattern_overlap", "quiet_zone"), report.violations.map { it.rule }.toSet())
        assertEquals(0.0, report.score)
    }

    @Test
    fun `um aviso sozinho nao bloqueia e tira pouco do score`() {
        val warningOnly = ScanabilityEngine(listOf(ScanabilityRule { _, _ -> Violation("teste", Severity.WARNING, "aviso") }))
        val report = warningOnly.evaluate(QrEncoder.encode("aviso", ErrorCorrectionLevel.M), QrStyle(black, white))

        assertTrue(report.passed)
        assertEquals(0.85, report.score, 1e-9)
    }
}

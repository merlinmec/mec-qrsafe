package com.mecqrsafe.validation.rules

import com.mecqrsafe.qrengine.matrix.ModuleType
import com.mecqrsafe.qrengine.matrix.QrMatrixBuilder
import com.mecqrsafe.validation.Color
import com.mecqrsafe.validation.QrStyle
import com.mecqrsafe.validation.Severity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ContrastRuleTest {

    private fun tinyMatrix() = QrMatrixBuilder(1).apply {
        set(0, 0, dark = true, type = ModuleType.DATA)
    }.build()

    @Test
    fun `preto sobre branco passa facil`() {
        val style = QrStyle(
            darkModuleColor = Color(0, 0, 0),
            lightModuleColor = Color(255, 255, 255),
        )
        assertNull(ContrastRule().evaluate(tinyMatrix(), style))
    }

    @Test
    fun `cinza sobre cinza falha`() {
        val style = QrStyle(
            darkModuleColor = Color(150, 150, 150),
            lightModuleColor = Color(200, 200, 200),
        )
        val violation = ContrastRule().evaluate(tinyMatrix(), style)
        requireNotNull(violation)
        assertEquals(Severity.BLOCKING, violation.severity)
    }
}

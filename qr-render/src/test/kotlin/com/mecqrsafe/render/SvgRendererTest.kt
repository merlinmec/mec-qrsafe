package com.mecqrsafe.render

import com.mecqrsafe.qrengine.matrix.ModuleType
import com.mecqrsafe.qrengine.matrix.QrMatrixBuilder
import kotlin.test.Test
import kotlin.test.assertTrue

class SvgRendererTest {

    @Test
    fun `gera svg valido com dimensoes corretas`() {
        val matrix = QrMatrixBuilder(2).apply {
            set(0, 0, dark = true, type = ModuleType.DATA)
            set(0, 1, dark = false, type = ModuleType.DATA)
            set(1, 0, dark = false, type = ModuleType.DATA)
            set(1, 1, dark = true, type = ModuleType.DATA)
        }.build()

        val svg = SvgRenderer.render(matrix, moduleSizePx = 10)

        assertTrue(svg.contains("width=\"20\""))
        assertTrue(svg.contains("height=\"20\""))
        assertTrue(svg.startsWith("<svg"))
        assertTrue(svg.trim().endsWith("</svg>"))
        // 2 módulos escuros (0,0) e (1,1) => 2 <rect> de módulo + 1 de fundo
        assertTrue(svg.split("<rect").size - 1 == 3)
    }
}

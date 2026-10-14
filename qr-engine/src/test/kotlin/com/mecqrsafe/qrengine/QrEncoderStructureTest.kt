package com.mecqrsafe.qrengine

import com.mecqrsafe.qrengine.encoding.DataEncoder
import com.mecqrsafe.qrengine.matrix.ModuleType
import com.mecqrsafe.qrengine.version.SymbolCapacity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * O que os outros módulos esperam da matriz além dos bits: os
 * [ModuleType] certos em cada lugar (o qr-validation decide o que um logo
 * pode cobrir consultando o tipo) e a quiet zone em volta.
 */
class QrEncoderStructureTest {

    private val quietZone = QrEncoder.DEFAULT_QUIET_ZONE

    @Test
    fun `quiet zone de 4 modulos claros em volta do simbolo`() {
        val qr = QrEncoder.encode("quiet zone", ErrorCorrectionLevel.M)
        val symbolSize = SymbolCapacity.symbolSize(qr.version)
        val matrix = qr.matrix

        assertEquals(symbolSize + 2 * quietZone, matrix.size)
        for (row in 0 until matrix.size) {
            for (column in 0 until matrix.size) {
                val inside = row - quietZone in 0 until symbolSize && column - quietZone in 0 until symbolSize
                val module = matrix[row, column]
                if (inside) {
                    assertTrue(module.type != ModuleType.QUIET_ZONE, "($row, $column) é símbolo, não quiet zone")
                } else {
                    assertEquals(ModuleType.QUIET_ZONE, module.type, "($row, $column)")
                    assertEquals(false, module.dark, "quiet zone tem que ser clara em ($row, $column)")
                }
            }
        }
    }

    @Test
    fun `quantidade de modulos de cada tipo bate com a geometria da versao`() {
        for (version in listOf(1, 2, 7, 40)) {
            val qr = QrEncoder.encode(QrEncoderAgainstZxingTest.texto(maxBytesM(version)), ErrorCorrectionLevel.M, quietZone = 0)
            fun count(type: ModuleType) = qr.matrix.modulesOfType(type).size

            assertEquals(version, qr.version)
            assertEquals(3 * 7 * 7, count(ModuleType.FINDER_PATTERN), "finders na versão $version")
            assertEquals(3 * 15, count(ModuleType.SEPARATOR), "separadores na versão $version")
            assertEquals(2 * 15, count(ModuleType.FORMAT_INFO), "format info na versão $version")
            assertEquals(1, count(ModuleType.DARK_MODULE), "dark module na versão $version")
            assertEquals(if (version >= 7) 2 * 18 else 0, count(ModuleType.VERSION_INFO), "version info na versão $version")
            assertEquals(SymbolCapacity.rawDataModules(version), count(ModuleType.DATA), "módulos de dado na versão $version")
        }
    }

    @Test
    fun `dark module fica escuro ao lado do finder de baixo-esquerda`() {
        val qr = QrEncoder.encode("dark module", ErrorCorrectionLevel.L, quietZone = 0)
        val size = qr.matrix.size

        val module = qr.matrix[size - 8, 8]
        assertEquals(ModuleType.DARK_MODULE, module.type)
        assertTrue(module.dark)
    }

    @Test
    fun `conteudo grande demais e recusado`() {
        assertFailsWith<IllegalArgumentException> {
            QrEncoder.encode("a".repeat(2954), ErrorCorrectionLevel.L)
        }
    }

    private fun maxBytesM(version: Int) = DataEncoder.maxBytes(version, ErrorCorrectionLevel.M)
}

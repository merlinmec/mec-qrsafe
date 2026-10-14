package com.mecqrsafe.qrengine.matrix

import com.mecqrsafe.qrengine.ErrorCorrectionLevel
import kotlin.test.Test
import kotlin.test.assertEquals

/** Vetores das tabelas C.1 (format info) e D.1 (version info) da ISO/IEC 18004. */
class FormatInfoTest {

    @Test
    fun `format info bate com a tabela da spec`() {
        assertEquals(0b111011111000100, FormatInfo.formatBits(ErrorCorrectionLevel.L, 0))
        assertEquals(0b110011000101111, FormatInfo.formatBits(ErrorCorrectionLevel.L, 4))
        assertEquals(0b101010000010010, FormatInfo.formatBits(ErrorCorrectionLevel.M, 0))
        assertEquals(0b100000011001110, FormatInfo.formatBits(ErrorCorrectionLevel.M, 5))
        assertEquals(0b010101111101101, FormatInfo.formatBits(ErrorCorrectionLevel.Q, 7))
        assertEquals(0b001011010001001, FormatInfo.formatBits(ErrorCorrectionLevel.H, 0))
    }

    @Test
    fun `version info bate com a tabela da spec`() {
        assertEquals(0b000111110010010100, FormatInfo.versionBits(7))
        assertEquals(0b101000110001101001, FormatInfo.versionBits(40))
    }

    @Test
    fun `posicoes dos alignment patterns batem com a tabela da spec`() {
        assertEquals(listOf(), FunctionPatterns.alignmentPatternPositions(1).toList())
        assertEquals(listOf(6, 18), FunctionPatterns.alignmentPatternPositions(2).toList())
        assertEquals(listOf(6, 22, 38), FunctionPatterns.alignmentPatternPositions(7).toList())
        assertEquals(listOf(6, 34, 60, 86, 112, 138), FunctionPatterns.alignmentPatternPositions(32).toList())
        assertEquals(listOf(6, 30, 58, 86, 114, 142, 170), FunctionPatterns.alignmentPatternPositions(40).toList())
    }
}

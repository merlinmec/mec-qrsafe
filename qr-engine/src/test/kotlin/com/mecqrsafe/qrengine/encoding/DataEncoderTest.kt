package com.mecqrsafe.qrengine.encoding

import com.mecqrsafe.qrengine.ErrorCorrectionLevel
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DataEncoderTest {

    @Test
    fun `bitstream de hi na versao 1-L confere bit a bit`() {
        // 0100 (byte mode) + 00000010 (2 bytes) + 'h' 01101000 + 'i' 01101001
        // + 0000 (terminador) = 0x40 0x26 0x86 0x90, depois padding EC/11
        // até os 19 codewords de dado da versão 1-L.
        val expected = intArrayOf(0x40, 0x26, 0x86, 0x90) +
            IntArray(15) { if (it % 2 == 0) 0xEC else 0x11 }

        val codewords = DataEncoder.encode("hi".toByteArray(), 1, ErrorCorrectionLevel.L)

        assertContentEquals(expected, codewords)
    }

    @Test
    fun `terminador encolhe quando a capacidade acaba antes dos 4 bits`() {
        // 17 bytes enchem a versão 1-L até o último bit: 4 + 8 + 17*8 = 148
        // de 152 bits. Sobram exatamente os 4 do terminador, sem padding.
        val codewords = DataEncoder.encode(ByteArray(17) { 'a'.code.toByte() }, 1, ErrorCorrectionLevel.L)

        assertEquals(19, codewords.size)
        assertEquals(0x10, codewords.last(), "último codeword = metade baixa de 'a' (0001) + terminador 0000")
    }

    @Test
    fun `contagem de caracteres passa a 16 bits a partir da versao 10`() {
        assertEquals(8, DataEncoder.characterCountBits(9))
        assertEquals(16, DataEncoder.characterCountBits(10))
        assertEquals(16, DataEncoder.characterCountBits(40))
    }

    @Test
    fun `capacidade em byte mode bate com a tabela da spec`() {
        // Tabela 7 da ISO/IEC 18004, coluna "Byte".
        assertEquals(17, DataEncoder.maxBytes(1, ErrorCorrectionLevel.L))
        assertEquals(7, DataEncoder.maxBytes(1, ErrorCorrectionLevel.H))
        assertEquals(271, DataEncoder.maxBytes(10, ErrorCorrectionLevel.L))
        assertEquals(2953, DataEncoder.maxBytes(40, ErrorCorrectionLevel.L))
        assertEquals(1273, DataEncoder.maxBytes(40, ErrorCorrectionLevel.H))
    }

    @Test
    fun `escolhe a menor versao em que o conteudo cabe`() {
        assertEquals(1, DataEncoder.chooseVersion(17, ErrorCorrectionLevel.L))
        assertEquals(2, DataEncoder.chooseVersion(18, ErrorCorrectionLevel.L))
        assertEquals(40, DataEncoder.chooseVersion(2953, ErrorCorrectionLevel.L))
    }

    @Test
    fun `conteudo maior que a versao 40 e recusado com mensagem clara`() {
        val error = assertFailsWith<IllegalArgumentException> {
            DataEncoder.chooseVersion(2954, ErrorCorrectionLevel.L)
        }
        assertTrue(error.message!!.contains("2953"), "a mensagem deveria dizer o máximo: ${error.message}")
    }
}

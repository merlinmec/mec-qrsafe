package com.mecqrsafe.qrengine.version

import com.mecqrsafe.qrengine.ErrorCorrectionLevel
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Valores pontuais da tabela 9 da ISO/IEC 18004. A tabela inteira (as 40
 * versões nos 4 níveis) é conferida contra o ZXing em
 * `QrEncoderAgainstZxingTest`, que falharia na versão errada se algum
 * número daqui estivesse trocado.
 */
class SymbolCapacityTest {

    @Test
    fun `tamanho do simbolo cresce 4 modulos por versao`() {
        assertEquals(21, SymbolCapacity.symbolSize(1))
        assertEquals(25, SymbolCapacity.symbolSize(2))
        assertEquals(177, SymbolCapacity.symbolSize(40))
    }

    @Test
    fun `total de codewords por versao`() {
        assertEquals(26, SymbolCapacity.totalCodewords(1))
        assertEquals(134, SymbolCapacity.totalCodewords(5))
        assertEquals(196, SymbolCapacity.totalCodewords(7))
        assertEquals(3706, SymbolCapacity.totalCodewords(40))
    }

    @Test
    fun `sobra de remainder bits por versao`() {
        // Versão 1: 208 módulos = 26 codewords exatos. Versão 2: 359 = 44 codewords + 7 bits.
        assertEquals(208, SymbolCapacity.rawDataModules(1))
        assertEquals(359, SymbolCapacity.rawDataModules(2))
        assertEquals(29648, SymbolCapacity.rawDataModules(40))
    }

    @Test
    fun `codewords de dado por versao e nivel`() {
        assertEquals(19, SymbolCapacity.dataCodewords(1, ErrorCorrectionLevel.L))
        assertEquals(16, SymbolCapacity.dataCodewords(1, ErrorCorrectionLevel.M))
        assertEquals(13, SymbolCapacity.dataCodewords(1, ErrorCorrectionLevel.Q))
        assertEquals(9, SymbolCapacity.dataCodewords(1, ErrorCorrectionLevel.H))
        assertEquals(62, SymbolCapacity.dataCodewords(5, ErrorCorrectionLevel.Q))
        assertEquals(2956, SymbolCapacity.dataCodewords(40, ErrorCorrectionLevel.L))
        assertEquals(1276, SymbolCapacity.dataCodewords(40, ErrorCorrectionLevel.H))
    }
}

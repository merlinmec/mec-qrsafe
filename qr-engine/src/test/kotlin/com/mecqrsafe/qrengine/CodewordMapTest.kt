package com.mecqrsafe.qrengine

import com.mecqrsafe.qrengine.encoding.DataEncoder
import com.mecqrsafe.qrengine.matrix.ModuleType
import com.mecqrsafe.qrengine.matrix.QrMatrixBuilder
import com.mecqrsafe.qrengine.version.SymbolCapacity
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull

/**
 * O [CodewordMap] é o que o qr-validation usa pra dizer se um logo cabe
 * na correção de erro — um mapa errado aprovaria QR ilegível. Por isso,
 * além da contagem, os testes estragam codewords escolhidos pelo mapa e
 * conferem com o leitor do ZXing: no limite de cada bloco ainda lê, um a
 * mais num bloco já não lê.
 */
class CodewordMapTest {

    @ParameterizedTest(name = "versão {0}, nível {1}")
    @MethodSource("simbolos")
    fun `cada codeword ocupa 8 modulos de dado e o resto sao remainder bits`(version: Int, level: ErrorCorrectionLevel) {
        val qr = encodeFull(version, level)
        val modulesPerCodeword = IntArray(qr.codewords.codewordCount)
        var dataWithoutCodeword = 0

        for (row in 0 until qr.matrix.size) {
            for (column in 0 until qr.matrix.size) {
                val codeword = qr.codewords.codewordAt(row, column)
                if (codeword == null) {
                    if (qr.matrix[row, column].type == ModuleType.DATA) dataWithoutCodeword++
                } else {
                    assertEquals(ModuleType.DATA, qr.matrix[row, column].type, "codeword fora de módulo de dado em ($row, $column)")
                    modulesPerCodeword[codeword]++
                }
            }
        }

        assertEquals(SymbolCapacity.totalCodewords(version), qr.codewords.codewordCount)
        assertEquals(List(qr.codewords.codewordCount) { 8 }, modulesPerCodeword.toList())
        assertEquals(SymbolCapacity.rawDataModules(version) % 8, dataWithoutCodeword)
        assertEquals(SymbolCapacity.errorCorrectionBlocks(version, level), qr.codewords.blockCount)
    }

    @Test
    fun `correcao por bloco desconta a protecao contra leitura errada das versoes 1 a 3`() {
        fun capacity(version: Int, level: ErrorCorrectionLevel) = SymbolCapacity.correctableErrorsPerBlock(version, level)

        assertEquals(2, capacity(1, ErrorCorrectionLevel.L)) // 7 de correção, p = 3
        assertEquals(4, capacity(1, ErrorCorrectionLevel.M)) // 10, p = 2
        assertEquals(8, capacity(1, ErrorCorrectionLevel.H)) // 17, p = 1
        assertEquals(4, capacity(2, ErrorCorrectionLevel.L)) // 10, p = 2
        assertEquals(7, capacity(3, ErrorCorrectionLevel.L)) // 15, p = 1
        assertEquals(9, capacity(5, ErrorCorrectionLevel.Q)) // 18, sem p
        assertEquals(15, capacity(40, ErrorCorrectionLevel.H)) // 30, sem p
    }

    @Test
    fun `quiet zone e padroes fixos nao carregam codeword`() {
        val qr = QrEncoder.encode("sem codeword aqui", ErrorCorrectionLevel.M)

        assertNull(qr.codewords.codewordAt(0, 0), "quiet zone")
        assertNull(qr.codewords.codewordAt(qr.quietZone, qr.quietZone), "finder pattern")
    }

    @ParameterizedTest(name = "versão {0}, nível {1}")
    @MethodSource("simbolosSemProtecao")
    fun `leitor real aguenta o limite de erros em todos os blocos ao mesmo tempo`(version: Int, level: ErrorCorrectionLevel) {
        val qr = encodeFull(version, level)
        val limit = qr.codewords.correctableErrorsPerBlock
        val damaged = (0 until qr.codewords.blockCount).flatMap { block -> codewordsOf(qr, block).take(limit) }

        assertEquals(contentFor(version, level), ZxingReference.decode(corrupt(qr, damaged.toSet())))
    }

    @ParameterizedTest(name = "versão {0}, nível {1}")
    @MethodSource("simbolosSemProtecao")
    fun `um erro alem do limite num bloco so ja impede a leitura`(version: Int, level: ErrorCorrectionLevel) {
        val qr = encodeFull(version, level)
        val damaged = codewordsOf(qr, block = 0).take(qr.codewords.correctableErrorsPerBlock + 1)

        assertFails { ZxingReference.decode(corrupt(qr, damaged.toSet())) }
    }

    private fun codewordsOf(qr: QrCode, block: Int): List<Int> =
        (0 until qr.codewords.codewordCount).filter { qr.codewords.blockOf(it) == block }

    /** Inverte os 8 módulos de cada codeword escolhido — o byte com certeza muda. */
    private fun corrupt(qr: QrCode, codewords: Set<Int>) = QrMatrixBuilder(qr.matrix.size).apply {
        for (row in 0 until size) {
            for (column in 0 until size) {
                val module = qr.matrix[row, column]
                val hit = qr.codewords.codewordAt(row, column) in codewords
                set(row, column, module.dark != hit, module.type)
            }
        }
    }.build()

    private fun contentFor(version: Int, level: ErrorCorrectionLevel) =
        QrEncoderAgainstZxingTest.texto(DataEncoder.maxBytes(version, level))

    /** Texto que enche a versão, pra não sobrar padding: todo codeword carrega dado de verdade. */
    private fun encodeFull(version: Int, level: ErrorCorrectionLevel): QrCode =
        QrEncoder.encode(contentFor(version, level), level).also { assertEquals(version, it.version) }

    companion object {
        @JvmStatic
        fun simbolos(): List<Arguments> = listOf(1, 2, 5, 7, 14, 40).flatMap { version ->
            ErrorCorrectionLevel.entries.map { Arguments.of(version, it) }
        }

        /** A partir da versão 4 não há codewords de proteção, então o limite é exatamente o do ZXing. */
        @JvmStatic
        fun simbolosSemProtecao(): List<Arguments> = listOf(
            Arguments.of(4, ErrorCorrectionLevel.L),
            Arguments.of(5, ErrorCorrectionLevel.Q),
            Arguments.of(7, ErrorCorrectionLevel.M),
            Arguments.of(10, ErrorCorrectionLevel.H),
            Arguments.of(15, ErrorCorrectionLevel.Q),
            Arguments.of(25, ErrorCorrectionLevel.L),
        )
    }
}

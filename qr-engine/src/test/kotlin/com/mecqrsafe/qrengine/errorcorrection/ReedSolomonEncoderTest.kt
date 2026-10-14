package com.mecqrsafe.qrengine.errorcorrection

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/**
 * Duas camadas: um vetor numérico conhecido ("HELLO WORLD" na versão
 * 1-M) e uma propriedade matemática independente da implementação — o
 * codeword completo (dados + correção) tem α^0, α^1, ..., α^(n-1) como
 * raízes, que é a definição de um codeword Reed-Solomon válido, não
 * importa como ele foi calculado.
 *
 * A correção dentro do QR inteiro (com vários blocos intercalados) é
 * conferida contra o ZXing em `QrEncoderAgainstZxingTest`.
 */
class ReedSolomonEncoderTest {

    @Test
    fun `correcao de HELLO WORLD na versao 1-M bate com o exemplo classico`() {
        // Codewords de dado de "HELLO WORLD" em alphanumeric mode, versão
        // 1-M, e os 10 de correção esperados — o exemplo resolvido passo a
        // passo no tutorial de QR da Thonky, usado como referência por
        // boa parte das implementações.
        val data = intArrayOf(32, 91, 11, 120, 209, 114, 220, 77, 67, 64, 236, 17, 236, 17, 236, 17)
        val expected = intArrayOf(196, 35, 39, 119, 235, 215, 231, 226, 93, 23)

        assertContentEquals(expected, ReedSolomonEncoder.encode(data, 10))
    }

    /** Avalia um polinômio em GF(256) via método de Horner. poly[0] = maior grau. */
    private fun evaluate(poly: IntArray, x: Int): Int {
        var result = poly[0]
        for (i in 1 until poly.size) {
            result = GF256.multiply(result, x) xor poly[i]
        }
        return result
    }

    @Test
    fun `codeword gerado tem as raizes esperadas do polinomio gerador`() {
        val data = intArrayOf(32, 91, 11, 120, 209, 114, 220, 77, 67, 64, 236, 17)
        val eccCount = 10

        val ecc = ReedSolomonEncoder.encode(data, eccCount)
        assertEquals(eccCount, ecc.size)

        val fullCodeword = data + ecc
        for (i in 0 until eccCount) {
            assertEquals(
                0,
                evaluate(fullCodeword, GF256.exp(i)),
                "codeword deveria ser divisível pelo fator (x - α^$i) do gerador",
            )
        }
    }

    @Test
    fun `dados diferentes produzem correcao diferente`() {
        val eccA = ReedSolomonEncoder.encode(intArrayOf(1, 2, 3, 4), 6)
        val eccB = ReedSolomonEncoder.encode(intArrayOf(4, 3, 2, 1), 6)
        assert(!eccA.contentEquals(eccB)) {
            "entradas diferentes não deveriam gerar a mesma correção de erro"
        }
    }

    @Test
    fun `polinomio gerador e sempre monico`() {
        for (degree in 1..30) {
            val generator = ReedSolomonEncoder.buildGeneratorPolynomial(degree)
            assertEquals(degree + 1, generator.size)
            assertEquals(1, generator[0], "coeficiente de maior grau deveria ser 1 (monic)")
        }
    }
}

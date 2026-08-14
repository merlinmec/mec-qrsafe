package com.mecqrsafe.qrengine.errorcorrection

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * TODO (seção 4 de `DECISOES-E-TRADEOFFS.txt`, camada 1 da estratégia de
 * teste): faltam vetores numéricos OFICIAIS da especificação ISO/IEC
 * 18004 aqui. Os testes abaixo verificam uma propriedade matemática
 * independente da própria implementação (não reusam `encode` pra
 * verificar `encode`), mas não substituem comparar contra um exemplo
 * numérico da spec ou golden-file contra ZXing.
 *
 * Propriedade verificada: o polinômio codeword completo
 * (dados seguidos dos codewords de correção) tem α^0, α^1, ..., α^(n-1)
 * como raízes — essa é a definição matemática de um codeword
 * Reed-Solomon válido, independente de como ele foi calculado.
 */
class ReedSolomonEncoderTest {

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

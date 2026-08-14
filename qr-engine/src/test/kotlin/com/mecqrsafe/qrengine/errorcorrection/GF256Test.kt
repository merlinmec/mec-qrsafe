package com.mecqrsafe.qrengine.errorcorrection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Testes de propriedade estrutural, não vetores "oficiais" da spec —
 * esses ainda precisam ser adicionados separadamente (ver TODO na seção
 * 4 de `DECISOES-E-TRADEOFFS.txt`, camada 1 da estratégia de teste).
 * O que está aqui garante que a aritmética é internamente consistente.
 */
class GF256Test {

    @Test
    fun `multiplicar por 1 e identidade`() {
        for (a in 1..255) {
            assertEquals(a, GF256.multiply(a, 1))
        }
    }

    @Test
    fun `multiplicar por 0 e sempre 0`() {
        for (a in 0..255) {
            assertEquals(0, GF256.multiply(a, 0))
        }
    }

    @Test
    fun `dividir desfaz multiplicar`() {
        for (a in 1..255) {
            for (b in 1..255) {
                val product = GF256.multiply(a, b)
                assertEquals(a, GF256.divide(product, b))
            }
        }
    }

    @Test
    fun `inverso multiplicativo funciona`() {
        for (a in 1..255) {
            assertEquals(1, GF256.multiply(a, GF256.inverse(a)))
        }
    }

    @Test
    fun `multiplicacao e comutativa`() {
        for (a in 0..255 step 7) {
            for (b in 0..255 step 11) {
                assertEquals(GF256.multiply(a, b), GF256.multiply(b, a))
            }
        }
    }

    @Test
    fun `log de zero lanca excecao`() {
        assertFailsWith<IllegalArgumentException> { GF256.log(0) }
    }

    @Test
    fun `exp e log sao inversas`() {
        for (power in 0..254) {
            assertEquals(power, GF256.log(GF256.exp(power)))
        }
    }
}

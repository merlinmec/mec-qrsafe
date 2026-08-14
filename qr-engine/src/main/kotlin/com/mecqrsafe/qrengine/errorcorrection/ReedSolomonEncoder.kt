package com.mecqrsafe.qrengine.errorcorrection

/**
 * Gera os codewords de correção de erro Reed-Solomon usados pelo QR code.
 *
 * Pipeline (ver seção 4 de `DECISOES-E-TRADEOFFS.txt`):
 *   1. Constrói o polinômio gerador G(x) = (x - α^0)(x - α^1)...(x - α^(n-1))
 *      onde n = número de codewords de correção desejado.
 *   2. "Divide" o polinômio de dados (deslocado por x^n, ou seja, com n
 *      zeros anexados) por G(x) usando aritmética de [GF256].
 *   3. O resto dessa divisão são os codewords de correção.
 *
 * Em GF(2^8), subtração e soma são a mesma operação (XOR) — por isso a
 * "divisão polinomial" abaixo é implementada só com XOR e multiplicação
 * em GF256, sem nenhum sinal de menos.
 *
 * IMPORTANTE (ver seção 4, "Testabilidade contra leitores reais" em
 * `DECISOES-E-TRADEOFFS.txt`): esta implementação ainda NÃO foi validada
 * contra os vetores numéricos da especificação ISO/IEC 18004 nem contra
 * golden-files de uma lib madura (ZXing). Antes de confiar neste código
 * em produção, escrever esses testes é obrigatório — a lógica abaixo é
 * a estrutura correta do algoritmo, mas "compila e parece certo" não é
 * prova de corretude aqui.
 */
object ReedSolomonEncoder {

    /**
     * Constrói o polinômio gerador de grau [eccCodewordCount].
     * Representação: índice 0 = coeficiente de maior grau (monic, ou
     * seja, generator[0] é sempre 1).
     */
    fun buildGeneratorPolynomial(eccCodewordCount: Int): IntArray {
        require(eccCodewordCount > 0) { "Precisa de ao menos 1 codeword de correção" }

        var generator = intArrayOf(1)
        for (i in 0 until eccCodewordCount) {
            // Multiplica o polinômio acumulado por (x - α^i) == (x + α^i) em GF(2^8)
            generator = multiplyPolynomials(generator, intArrayOf(1, GF256.exp(i)))
        }
        return generator
    }

    /** Multiplica dois polinômios sobre GF(256). Índice 0 = maior grau. */
    fun multiplyPolynomials(p1: IntArray, p2: IntArray): IntArray {
        val result = IntArray(p1.size + p2.size - 1)
        for (i in p1.indices) {
            if (p1[i] == 0) continue
            for (j in p2.indices) {
                result[i + j] = result[i + j] xor GF256.multiply(p1[i], p2[j])
            }
        }
        return result
    }

    /**
     * Calcula os codewords de correção de erro para [dataCodewords],
     * gerando [eccCodewordCount] codewords extras.
     */
    fun encode(dataCodewords: IntArray, eccCodewordCount: Int): IntArray {
        val generator = buildGeneratorPolynomial(eccCodewordCount)

        // "Desloca" os dados multiplicando por x^eccCodewordCount, ou seja,
        // anexa eccCodewordCount zeros — é aqui que sobra espaço pro resto
        // da divisão polinomial virar os codewords de correção.
        val remainder = dataCodewords.copyOf(dataCodewords.size + eccCodewordCount)

        for (i in dataCodewords.indices) {
            val coefficient = remainder[i]
            if (coefficient == 0) continue
            // generator[0] é sempre 1 (polinômio monic), então o fator de
            // eliminação é sempre o próprio coeficiente — sem precisar
            // dividir por generator[0].
            for (j in generator.indices) {
                remainder[i + j] = remainder[i + j] xor GF256.multiply(generator[j], coefficient)
            }
        }

        return remainder.copyOfRange(dataCodewords.size, remainder.size)
    }
}

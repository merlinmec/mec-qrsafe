package com.mecqrsafe.qrengine

/**
 * Onde cada codeword mora na matriz e quanto estrago cada bloco aguenta —
 * o que o qr-validation precisa pra responder "esse logo ainda deixa o QR
 * legível?" com a conta real em vez de um percentual mágico de área (ver
 * seção 5 de `DECISOES-E-TRADEOFFS.txt`).
 *
 * Um codeword (8 bits) ocupa 8 módulos de dado. Cobrir qualquer um deles
 * pode trocar o byte inteiro, e o Reed-Solomon conta erro por codeword,
 * não por módulo. Cada codeword pertence a um bloco, e cada bloco corrige
 * sozinho até [correctableErrorsPerBlock] codewords errados — basta um
 * bloco estourar pra leitura falhar, mesmo com os outros intactos.
 *
 * As coordenadas são as da [com.mecqrsafe.qrengine.matrix.QrMatrix]
 * (quiet zone incluída).
 */
class CodewordMap internal constructor(
    private val matrixSize: Int,
    private val codewordAtModule: IntArray,
    private val blockOfCodeword: IntArray,
    /** Todos os blocos de um símbolo têm a mesma correção, então é um número só. */
    val correctableErrorsPerBlock: Int,
) {
    val codewordCount: Int get() = blockOfCodeword.size

    val blockCount: Int = blockOfCodeword.max() + 1

    /** O codeword que o módulo carrega, ou `null` se ele é padrão fixo, quiet zone ou remainder bit. */
    fun codewordAt(row: Int, column: Int): Int? {
        require(row in 0 until matrixSize && column in 0 until matrixSize) {
            "Posição ($row, $column) fora da matriz de tamanho $matrixSize"
        }
        return codewordAtModule[row * matrixSize + column].takeIf { it >= 0 }
    }

    fun blockOf(codeword: Int): Int {
        require(codeword in blockOfCodeword.indices) { "codeword $codeword não existe (são $codewordCount)" }
        return blockOfCodeword[codeword]
    }
}

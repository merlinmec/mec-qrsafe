package com.mecqrsafe.qrengine

import com.mecqrsafe.qrengine.matrix.QrMatrix

/**
 * Resultado do [QrEncoder]: a matriz pronta e as escolhas que o encoder
 * fez pra chegar nela.
 *
 * Versão, nível e máscara ficam fora de [QrMatrix] porque a matriz é o
 * contrato visual (o que desenhar), enquanto estes campos são metadado do
 * encoding — úteis pra API mostrar ("versão 3, nível Q") e pro
 * qr-validation calcular a capacidade de correção, mas irrelevantes pra
 * quem só pinta os módulos.
 */
data class QrCode(
    /** De 1 (21x21 módulos) a 40 (177x177). */
    val version: Int,
    val errorCorrectionLevel: ErrorCorrectionLevel,
    /** Máscara escolhida, de 0 a 7. */
    val mask: Int,
    /** Largura da margem clara em volta do símbolo, em módulos. */
    val quietZone: Int,
    /** Matriz final, já com a quiet zone em volta (ver [QrEncoder.encode]). */
    val matrix: QrMatrix,
    /** Que codeword cada módulo de dado carrega e quanto cada bloco corrige. */
    val codewords: CodewordMap,
) {
    /** Lado do símbolo em módulos, sem a quiet zone. */
    val symbolSize: Int get() = matrix.size - 2 * quietZone
}

package com.mecqrsafe.qrengine.matrix

/**
 * Estágio 5 do pipeline, parte dos dados: espalha os bits dos codewords
 * pelos módulos que os padrões fixos deixaram livres.
 *
 * O caminho é um zigue-zague em colunas de 2 módulos de largura,
 * começando no canto de baixo-direita: sobe a primeira dupla de colunas,
 * desce a próxima, e assim por diante até a esquerda. Dentro de cada
 * dupla, o bit vai primeiro no módulo da direita, depois no da esquerda.
 * A coluna 6 (timing vertical) é pulada inteira, por isso a última dupla
 * à esquerda dela é a das colunas 0-1, não 5-6.
 *
 * Os módulos que sobram depois do último bit (remainder bits, de 0 a 7
 * dependendo da versão) ficam claros e também são tratados como dado —
 * a máscara passa por eles.
 */
internal object DataPlacement {

    /** Marca, no mapa devolvido por [place], os módulos que não carregam codeword. */
    const val NO_CODEWORD = -1

    /**
     * Desenha os bits e devolve, pra cada módulo (índice `linha * size +
     * coluna`), o índice do codeword que ele carrega na sequência
     * intercalada — ou [NO_CODEWORD] nos padrões fixos e nos remainder
     * bits. É desse mapa que o qr-validation tira quantos codewords um
     * logo destrói.
     */
    fun place(grid: SymbolGrid, codewords: IntArray): IntArray {
        val size = grid.size
        val totalBits = codewords.size * 8
        val codewordAt = IntArray(size * size) { NO_CODEWORD }
        var bitIndex = 0

        var right = size - 1
        while (right >= 1) {
            if (right == 6) right = 5
            // Sobe nas duplas em que (right + 1) tem o bit 1 zerado:
            // começa subindo na borda direita e alterna a cada dupla.
            val upward = (right + 1) and 2 == 0
            for (step in 0 until size) {
                val row = if (upward) size - 1 - step else step
                for (offset in 0..1) {
                    val column = right - offset
                    if (grid.type(row, column) != null) continue
                    val dark = bitIndex < totalBits &&
                        (codewords[bitIndex ushr 3] ushr (7 - (bitIndex and 7))) and 1 == 1
                    grid.set(row, column, dark, ModuleType.DATA)
                    if (bitIndex < totalBits) codewordAt[row * size + column] = bitIndex ushr 3
                    bitIndex++
                }
            }
            right -= 2
        }

        check(bitIndex >= totalBits) {
            "só couberam $bitIndex de $totalBits bits de dado na matriz"
        }
        return codewordAt
    }
}

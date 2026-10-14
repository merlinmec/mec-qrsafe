package com.mecqrsafe.qrengine.matrix

import kotlin.math.abs

/**
 * Estágio 6 do pipeline: as 8 máscaras candidatas e a pontuação que
 * escolhe entre elas.
 *
 * Máscara = inverter os módulos de dado em que uma fórmula de (linha,
 * coluna) dá verdadeiro. Ela não muda a informação (o leitor desfaz com a
 * mesma fórmula), só a aparência: dados reais podem formar blocos grandes
 * de uma cor ou imitar um finder pattern, o que confunde o leitor. O
 * encoder testa as 8 e fica com a de MENOR penalidade.
 *
 * As regras de penalidade (N1 a N4) seguem a ISO/IEC 18004 do mesmo
 * jeito que o ZXing implementa — inclusive num detalhe que a spec deixa
 * em aberto: na regra N3, uma janela de "4 claros" que sai da matriz não
 * conta. Com isso o teste pode exigir que a máscara escolhida aqui seja a
 * mesma do ZXing, não só que o QR resultante leia (qualquer uma das 8
 * máscaras gera um QR válido; a penalidade só escolhe a mais fácil de
 * ler).
 */
internal object Masking {

    const val MASK_COUNT = 8

    private const val N1 = 3
    private const val N2 = 3
    private const val N3 = 40
    private const val N4 = 10

    /** `true` = percorrer linhas, `false` = colunas. */
    private val ORIENTATIONS = listOf(true, false)
    private val FINDER_LIKE = booleanArrayOf(true, false, true, true, true, false, true)

    /** Fórmulas da tabela 10 da spec; `true` = inverter o módulo. */
    fun isMasked(mask: Int, row: Int, column: Int): Boolean = when (mask) {
        0 -> (row + column) % 2 == 0
        1 -> row % 2 == 0
        2 -> column % 3 == 0
        3 -> (row + column) % 3 == 0
        4 -> (row / 2 + column / 3) % 2 == 0
        5 -> (row * column) % 2 + (row * column) % 3 == 0
        6 -> ((row * column) % 2 + (row * column) % 3) % 2 == 0
        7 -> ((row + column) % 2 + (row * column) % 3) % 2 == 0
        else -> throw IllegalArgumentException("máscara precisa estar entre 0 e 7, veio $mask")
    }

    /** Inverte os módulos de dado marcados pela máscara. Padrões fixos nunca são mascarados. */
    fun apply(grid: SymbolGrid, mask: Int) {
        for (row in 0 until grid.size) {
            for (column in 0 until grid.size) {
                if (grid.type(row, column) == ModuleType.DATA && isMasked(mask, row, column)) {
                    grid.flip(row, column)
                }
            }
        }
    }

    fun penalty(grid: SymbolGrid): Int =
        sameColorRuns(grid) + sameColorBlocks(grid) + finderLikePatterns(grid) + darkProportion(grid)

    /** N1: cada sequência de 5+ módulos da mesma cor numa linha ou coluna. */
    private fun sameColorRuns(grid: SymbolGrid): Int {
        var penalty = 0
        for (horizontal in ORIENTATIONS) {
            for (i in 0 until grid.size) {
                var runLength = 0
                var previous: Boolean? = null
                for (j in 0 until grid.size) {
                    val dark = if (horizontal) grid.isDark(i, j) else grid.isDark(j, i)
                    if (dark == previous) {
                        runLength++
                    } else {
                        if (runLength >= 5) penalty += N1 + (runLength - 5)
                        runLength = 1
                        previous = dark
                    }
                }
                if (runLength >= 5) penalty += N1 + (runLength - 5)
            }
        }
        return penalty
    }

    /** N2: cada bloco 2x2 de uma cor só (blocos sobrepostos contam separado). */
    private fun sameColorBlocks(grid: SymbolGrid): Int {
        var blocks = 0
        for (row in 0 until grid.size - 1) {
            for (column in 0 until grid.size - 1) {
                val dark = grid.isDark(row, column)
                if (dark == grid.isDark(row, column + 1) &&
                    dark == grid.isDark(row + 1, column) &&
                    dark == grid.isDark(row + 1, column + 1)
                ) {
                    blocks++
                }
            }
        }
        return N2 * blocks
    }

    /**
     * N3: o padrão 1:1:3:1:1 (escuro-claro-escuro-escuro-escuro-claro-escuro)
     * com 4 claros de um dos lados — é a "assinatura" de um finder
     * pattern, e um falso finder no meio dos dados atrapalha o leitor.
     * A faixa de 4 claros precisa caber inteira dentro da matriz.
     */
    private fun finderLikePatterns(grid: SymbolGrid): Int {
        var count = 0
        for (i in 0 until grid.size) {
            for (j in 0..grid.size - FINDER_LIKE.size) {
                for (horizontal in ORIENTATIONS) {
                    fun dark(k: Int) = if (horizontal) grid.isDark(i, k) else grid.isDark(k, i)
                    fun lightRange(from: Int, to: Int): Boolean =
                        from >= 0 && to <= grid.size && (from until to).none { dark(it) }

                    val matches = FINDER_LIKE.indices.all { dark(j + it) == FINDER_LIKE[it] }
                    if (matches && (lightRange(j - 4, j) || lightRange(j + 7, j + 11))) count++
                }
            }
        }
        return N3 * count
    }

    /** N4: quanto a proporção de escuros se afasta de 50%, em passos de 5%. */
    private fun darkProportion(grid: SymbolGrid): Int {
        var darkCount = 0
        for (row in 0 until grid.size) {
            for (column in 0 until grid.size) {
                if (grid.isDark(row, column)) darkCount++
            }
        }
        val total = grid.size * grid.size
        val fivePercentSteps = abs(darkCount * 2 - total) * 10 / total
        return N4 * fivePercentSteps
    }
}

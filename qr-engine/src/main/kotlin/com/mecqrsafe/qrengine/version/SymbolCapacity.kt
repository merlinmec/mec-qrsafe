package com.mecqrsafe.qrengine.version

import com.mecqrsafe.qrengine.ErrorCorrectionLevel

/**
 * Quanto cabe em cada versão (1 a 40) e nível de correção de erro, e como
 * os codewords se dividem em blocos Reed-Solomon — tabelas 7 e 9 da
 * ISO/IEC 18004.
 *
 * Só duas tabelas são guardadas (codewords de correção por bloco e número
 * de blocos); todo o resto sai delas e da geometria da versão. Guardar a
 * tabela de capacidade inteira também funcionaria, mas seria mais um
 * lugar pra um número digitado errado esconder um bug — e as que estão
 * aqui são conferidas contra o ZXing, versão por versão, nos testes.
 */
internal object SymbolCapacity {

    const val MIN_VERSION = 1
    const val MAX_VERSION = 40

    // Índice da linha = ErrorCorrectionLevel.ordinal (L, M, Q, H);
    // índice da coluna = versão (a posição 0 não é usada).
    private val ECC_CODEWORDS_PER_BLOCK = arrayOf(
        intArrayOf(-1, 7, 10, 15, 20, 26, 18, 20, 24, 30, 18, 20, 24, 26, 30, 22, 24, 28, 30, 28, 28, 28, 28, 30, 30, 26, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30),
        intArrayOf(-1, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26, 30, 22, 22, 24, 24, 28, 28, 26, 26, 26, 26, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28),
        intArrayOf(-1, 13, 22, 18, 26, 18, 24, 18, 22, 20, 24, 28, 26, 24, 20, 30, 24, 28, 28, 26, 30, 28, 30, 30, 30, 30, 28, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30),
        intArrayOf(-1, 17, 28, 22, 16, 22, 28, 26, 26, 24, 28, 24, 28, 22, 24, 24, 30, 28, 28, 26, 28, 30, 24, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30, 30),
    )

    private val ERROR_CORRECTION_BLOCKS = arrayOf(
        intArrayOf(-1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 4, 4, 4, 4, 4, 6, 6, 6, 6, 7, 8, 8, 9, 9, 10, 12, 12, 12, 13, 14, 15, 16, 17, 18, 19, 19, 20, 21, 22, 24, 25),
        intArrayOf(-1, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5, 5, 8, 9, 9, 10, 10, 11, 13, 14, 16, 17, 17, 18, 20, 21, 23, 25, 26, 28, 29, 31, 33, 35, 37, 38, 40, 43, 45, 47, 49),
        intArrayOf(-1, 1, 1, 2, 2, 4, 4, 6, 6, 8, 8, 8, 10, 12, 16, 12, 17, 16, 18, 21, 20, 23, 23, 25, 27, 29, 34, 34, 35, 38, 40, 43, 45, 48, 51, 53, 56, 59, 62, 65, 68),
        intArrayOf(-1, 1, 1, 2, 4, 4, 4, 5, 6, 8, 8, 11, 11, 16, 16, 18, 16, 19, 21, 25, 25, 25, 34, 30, 32, 35, 37, 40, 42, 45, 48, 51, 54, 57, 60, 63, 66, 70, 74, 77, 81),
    )

    /** Lado da matriz em módulos, sem a quiet zone: 21 na versão 1, +4 a cada versão. */
    fun symbolSize(version: Int): Int {
        requireVersion(version)
        return version * 4 + 17
    }

    fun eccCodewordsPerBlock(version: Int, level: ErrorCorrectionLevel): Int {
        requireVersion(version)
        return ECC_CODEWORDS_PER_BLOCK[level.ordinal][version]
    }

    fun errorCorrectionBlocks(version: Int, level: ErrorCorrectionLevel): Int {
        requireVersion(version)
        return ERROR_CORRECTION_BLOCKS[level.ordinal][version]
    }

    /**
     * Módulos que sobram pra dados + correção depois de descontar os
     * padrões fixos (finder, separador, timing, alinhamento, format e
     * version info). Pode não ser múltiplo de 8 — a sobra vira os
     * "remainder bits", sempre claros antes da máscara.
     */
    fun rawDataModules(version: Int): Int {
        requireVersion(version)
        var result = (16 * version + 128) * version + 64
        if (version >= 2) {
            val alignmentPatternsPerSide = version / 7 + 2
            result -= (25 * alignmentPatternsPerSide - 10) * alignmentPatternsPerSide - 55
            if (version >= 7) result -= 36 // os dois blocos 6x3 de version info
        }
        return result
    }

    fun totalCodewords(version: Int): Int = rawDataModules(version) / 8

    fun dataCodewords(version: Int, level: ErrorCorrectionLevel): Int =
        totalCodewords(version) - eccCodewordsPerBlock(version, level) * errorCorrectionBlocks(version, level)

    private fun requireVersion(version: Int) {
        require(version in MIN_VERSION..MAX_VERSION) { "versão precisa estar entre 1 e 40, veio $version" }
    }
}

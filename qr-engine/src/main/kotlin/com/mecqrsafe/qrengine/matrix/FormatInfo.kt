package com.mecqrsafe.qrengine.matrix

import com.mecqrsafe.qrengine.ErrorCorrectionLevel

/**
 * Estágio 7 do pipeline: format info e version info, cada um protegido
 * por um código BCH próprio.
 *
 * São os primeiros bits que o leitor decodifica — o format info diz qual
 * nível de correção e qual máscara foram usados, sem isso nenhum dado
 * pode ser lido. Por isso eles têm correção própria (BCH), separada do
 * Reed-Solomon dos dados, e aparecem duplicados na matriz.
 *
 * BCH aqui é o mesmo truque do Reed-Solomon com coeficientes de 1 bit:
 * anexa zeros, divide pelo polinômio gerador (com XOR no lugar da
 * subtração) e o resto vira os bits de proteção.
 */
internal object FormatInfo {

    /** Gerador BCH(15,5): x^10 + x^8 + x^5 + x^4 + x^2 + x + 1. */
    private const val FORMAT_GENERATOR = 0x537

    /** XOR final do format info: garante que ele nunca saia todo zerado. */
    private const val FORMAT_MASK = 0x5412

    /** Gerador BCH(18,6): x^12 + x^11 + x^10 + x^9 + x^8 + x^5 + x^2 + 1. */
    private const val VERSION_GENERATOR = 0x1F25

    /**
     * Os 2 bits de nível no format info NÃO seguem a ordem L, M, Q, H —
     * a spec define L=01, M=00, Q=11, H=10.
     */
    private fun levelBits(level: ErrorCorrectionLevel): Int = when (level) {
        ErrorCorrectionLevel.L -> 0b01
        ErrorCorrectionLevel.M -> 0b00
        ErrorCorrectionLevel.Q -> 0b11
        ErrorCorrectionLevel.H -> 0b10
    }

    /** 15 bits: 2 de nível + 3 de máscara + 10 de BCH, já com o XOR de [FORMAT_MASK]. */
    fun formatBits(level: ErrorCorrectionLevel, mask: Int): Int {
        require(mask in 0..7) { "máscara precisa estar entre 0 e 7, veio $mask" }
        val data = (levelBits(level) shl 3) or mask
        var remainder = data
        repeat(10) { remainder = (remainder shl 1) xor ((remainder ushr 9) * FORMAT_GENERATOR) }
        return ((data shl 10) or remainder) xor FORMAT_MASK
    }

    /** 18 bits: 6 da versão + 12 de BCH. Só existe a partir da versão 7. */
    fun versionBits(version: Int): Int {
        require(version in 7..40) { "version info só existe da versão 7 à 40, veio $version" }
        var remainder = version
        repeat(12) { remainder = (remainder shl 1) xor ((remainder ushr 11) * VERSION_GENERATOR) }
        return (version shl 12) or remainder
    }

    /**
     * Desenha as duas cópias do format info: uma em volta do finder de
     * cima-esquerda, outra dividida entre os finders de cima-direita e
     * baixo-esquerda. Redesenhado a cada máscara candidata, porque a
     * máscara faz parte dele.
     */
    fun drawFormatBits(grid: SymbolGrid, level: ErrorCorrectionLevel, mask: Int) {
        val bits = formatBits(level, mask)
        val size = grid.size
        fun bit(i: Int) = (bits ushr i) and 1 == 1
        fun put(row: Int, column: Int, i: Int) = grid.set(row, column, bit(i), ModuleType.FORMAT_INFO)

        // Cópia 1: coluna 8 de cima pra baixo (pulando o timing da linha
        // 6), depois linha 8 da direita pra esquerda (pulando a coluna 6).
        for (i in 0..5) put(i, 8, i)
        put(7, 8, 6)
        put(8, 8, 7)
        put(8, 7, 8)
        for (i in 9..14) put(8, 14 - i, i)

        // Cópia 2: bits 0-7 na linha 8 junto ao finder de cima-direita,
        // bits 8-14 na coluna 8 junto ao finder de baixo-esquerda.
        for (i in 0..7) put(8, size - 1 - i, i)
        for (i in 8..14) put(size - 15 + i, 8, i)
    }

    /** As duas cópias 6x3 do version info, perto dos finders de cima-direita e baixo-esquerda. */
    fun drawVersionBits(grid: SymbolGrid, version: Int) {
        if (version < 7) return
        val bits = versionBits(version)
        val size = grid.size
        for (i in 0 until 18) {
            val dark = (bits ushr i) and 1 == 1
            val near = i / 3
            val far = size - 11 + i % 3
            grid.set(near, far, dark, ModuleType.VERSION_INFO)
            grid.set(far, near, dark, ModuleType.VERSION_INFO)
        }
    }
}

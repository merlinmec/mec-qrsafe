package com.mecqrsafe.qrengine.matrix

/**
 * Grade mutável do símbolo (sem quiet zone) usada DENTRO do encoder,
 * enquanto os padrões, os dados e a máscara vão sendo desenhados.
 *
 * Fica separada de [QrMatrixBuilder] por desempenho: a escolha de
 * máscara monta e pontua 8 versões da matriz, e aqui cada módulo é só um
 * Boolean num array plano, sem objeto por módulo. O resultado final vira
 * [QrMatrix] uma vez só, no fim.
 */
internal class SymbolGrid private constructor(
    val size: Int,
    private val dark: BooleanArray,
    private val types: Array<ModuleType?>,
) {
    constructor(size: Int) : this(size, BooleanArray(size * size), arrayOfNulls(size * size))

    fun isDark(row: Int, column: Int): Boolean = dark[index(row, column)]

    /** `null` enquanto o módulo ainda não foi ocupado por padrão nem por dado. */
    fun type(row: Int, column: Int): ModuleType? = types[index(row, column)]

    fun set(row: Int, column: Int, isDark: Boolean, type: ModuleType) {
        val i = index(row, column)
        dark[i] = isDark
        types[i] = type
    }

    fun flip(row: Int, column: Int) {
        val i = index(row, column)
        dark[i] = !dark[i]
    }

    fun copy(): SymbolGrid = SymbolGrid(size, dark.copyOf(), types.copyOf())

    private fun index(row: Int, column: Int): Int {
        require(row in 0 until size && column in 0 until size) {
            "Posição ($row, $column) fora da grade de tamanho $size"
        }
        return row * size + column
    }
}

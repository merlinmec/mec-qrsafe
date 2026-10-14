package com.mecqrsafe.qrengine.matrix

/**
 * Um módulo individual da matriz: o bit (preto/claro) + o que ele
 * representa estruturalmente (ver [ModuleType]).
 */
data class Module(
    val dark: Boolean,
    val type: ModuleType,
)

/**
 * Matriz de módulos de um QR code já codificado — IMUTÁVEL.
 *
 * Decisão de design (seção 4 de `DECISOES-E-TRADEOFFS.txt`): construída
 * via [QrMatrixBuilder] (mutável internamente, por performance durante a
 * fase de posicionamento/máscara), mas exposta só como valor imutável —
 * pode ser compartilhada entre qr-validation, qr-render e a API sem
 * precisar de cópia defensiva.
 *
 * Também é o contrato entre backend e frontend: a API manda essa matriz
 * crua (não um SVG pronto) pro front desenhar — ver seção 8 de
 * `DECISOES-E-TRADEOFFS.txt` (renderer canvas próprio no front).
 */
class QrMatrix internal constructor(
    val size: Int,
    private val modules: Array<Array<Module>>,
) {
    operator fun get(row: Int, column: Int): Module {
        require(row in 0 until size && column in 0 until size) {
            "Posição ($row, $column) fora da matriz de tamanho $size"
        }
        return modules[row][column]
    }

    /** Todos os módulos de um determinado tipo — usado pelo qr-validation. */
    fun modulesOfType(type: ModuleType): List<Pair<Int, Int>> {
        val result = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until size) {
            for (column in 0 until size) {
                if (modules[row][column].type == type) {
                    result.add(row to column)
                }
            }
        }
        return result
    }
}

/**
 * Builder mutável que monta a [QrMatrix] imutável. O encoder desenha
 * padrões, dados e máscara numa grade interna mais leve (`SymbolGrid`) e
 * só passa por aqui no fim, ao acrescentar a quiet zone.
 *
 * É público porque os testes de qr-validation e qr-render montam
 * matrizes pequenas à mão com ele; código de produção fora do qr-engine
 * deveria receber matrizes só do [com.mecqrsafe.qrengine.QrEncoder].
 */
class QrMatrixBuilder(val size: Int) {
    private val modules: Array<Array<Module?>> = Array(size) { arrayOfNulls(size) }

    fun set(row: Int, column: Int, dark: Boolean, type: ModuleType) {
        modules[row][column] = Module(dark, type)
    }

    fun isSet(row: Int, column: Int): Boolean = modules[row][column] != null

    fun build(): QrMatrix {
        val finalModules = Array(size) { row ->
            Array(size) { column ->
                modules[row][column]
                    ?: error("Módulo ($row, $column) nunca foi definido — matriz incompleta")
            }
        }
        return QrMatrix(size, finalModules)
    }
}

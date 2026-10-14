package com.mecqrsafe.qrengine.matrix

import com.mecqrsafe.qrengine.ErrorCorrectionLevel
import kotlin.math.abs
import kotlin.math.max

/**
 * Estágio 5 do pipeline, parte fixa: os padrões que não carregam dado e
 * que o leitor usa pra se localizar.
 *
 *  - Finder patterns (os 3 quadrados de canto): achar e orientar o QR.
 *  - Separadores: a borda clara de 1 módulo em volta de cada finder.
 *  - Timing patterns: linha e coluna 6 alternando escuro/claro, pra o
 *    leitor medir o tamanho de cada módulo.
 *  - Alignment patterns (a partir da versão 2): corrigem a distorção de
 *    perspectiva quando o QR é fotografado torto.
 *  - Dark module, format info e version info.
 *
 * Cada módulo sai daqui com o [ModuleType] certo — é isso que o
 * qr-validation consulta pra saber o que um logo pode ou não cobrir.
 */
internal object FunctionPatterns {

    /**
     * Desenha todos os padrões fixos. O format info é desenhado com um
     * valor provisório (nível + máscara 0) só pra ocupar as posições:
     * o valor real depende da máscara e é redesenhado na escolha dela.
     */
    fun draw(grid: SymbolGrid, version: Int, level: ErrorCorrectionLevel) {
        val size = grid.size

        // Timing primeiro: finders e alinhamentos sobrescrevem onde cruzam.
        for (i in 0 until size) {
            grid.set(6, i, i % 2 == 0, ModuleType.TIMING_PATTERN)
            grid.set(i, 6, i % 2 == 0, ModuleType.TIMING_PATTERN)
        }

        drawFinder(grid, 3, 3)
        drawFinder(grid, 3, size - 4)
        drawFinder(grid, size - 4, 3)

        val positions = alignmentPatternPositions(version)
        val last = positions.lastIndex
        for (i in positions.indices) {
            for (j in positions.indices) {
                // Os três cantos ocupados pelos finders não levam alinhamento.
                val overlapsFinder = (i == 0 && j == 0) || (i == 0 && j == last) || (i == last && j == 0)
                if (!overlapsFinder) drawAlignment(grid, positions[i], positions[j])
            }
        }

        FormatInfo.drawFormatBits(grid, level, 0)
        grid.set(size - 8, 8, true, ModuleType.DARK_MODULE)
        FormatInfo.drawVersionBits(grid, version)
    }

    /**
     * Linhas/colunas dos centros dos alignment patterns. O primeiro é
     * sempre 6 (alinhado ao timing) e o último fica a 7 da borda; os do
     * meio são espaçados por igual com passo par, contado de trás pra
     * frente (por isso o espaço entre o primeiro e o segundo pode ser
     * menor que os outros).
     */
    fun alignmentPatternPositions(version: Int): IntArray {
        if (version == 1) return IntArray(0)
        val count = version / 7 + 2
        val step = (version * 8 + count * 3 + 5) / (count * 4 - 4) * 2
        val result = IntArray(count)
        result[0] = 6
        var position = version * 4 + 17 - 7
        for (i in count - 1 downTo 1) {
            result[i] = position
            position -= step
        }
        return result
    }

    /** Finder 7x7 centrado em ([centerRow], [centerColumn]) + o separador claro em volta. */
    private fun drawFinder(grid: SymbolGrid, centerRow: Int, centerColumn: Int) {
        for (dRow in -4..4) {
            for (dColumn in -4..4) {
                val row = centerRow + dRow
                val column = centerColumn + dColumn
                if (row !in 0 until grid.size || column !in 0 until grid.size) continue
                // Distância de Chebyshev ao centro: 0-1 miolo escuro, 2 anel
                // claro, 3 anel escuro, 4 separador.
                val distance = max(abs(dRow), abs(dColumn))
                if (distance == 4) {
                    grid.set(row, column, false, ModuleType.SEPARATOR)
                } else {
                    grid.set(row, column, distance != 2, ModuleType.FINDER_PATTERN)
                }
            }
        }
    }

    /** Alignment 5x5: centro escuro, anel claro, borda escura. */
    private fun drawAlignment(grid: SymbolGrid, centerRow: Int, centerColumn: Int) {
        for (dRow in -2..2) {
            for (dColumn in -2..2) {
                val distance = max(abs(dRow), abs(dColumn))
                grid.set(centerRow + dRow, centerColumn + dColumn, distance != 1, ModuleType.ALIGNMENT_PATTERN)
            }
        }
    }
}

package com.mecqrsafe.render

import com.mecqrsafe.qrengine.matrix.QrMatrix

/**
 * Gera SVG a partir de uma [QrMatrix] já calculada — usado só no
 * endpoint de export final (`GET .../export?format=svg`), garantindo que
 * o arquivo baixado é exatamente o que passou na validação, sem depender
 * da fidelidade do renderer do browser (ver seção 8 de
 * `DECISOES-E-TRADEOFFS.txt`).
 *
 * O preview ao vivo no editor NÃO usa isso — usa o renderer canvas
 * próprio no front, que desenha a mesma matriz localmente sem round-trip
 * a cada ajuste cosmético.
 *
 * v1: só módulo quadrado, cor sólida — forma customizada (dots, rounded)
 * e gradiente ficam de fora deste scaffold inicial (TODO).
 */
object SvgRenderer {

    fun render(
        matrix: QrMatrix,
        moduleSizePx: Int = 10,
        darkColor: String = "#000000",
        lightColor: String = "#FFFFFF",
    ): String {
        require(moduleSizePx > 0) { "moduleSizePx precisa ser positivo" }

        val dimension = matrix.size * moduleSizePx
        val svg = StringBuilder()
        svg.append(
            """<svg xmlns="http://www.w3.org/2000/svg" width="$dimension" height="$dimension" viewBox="0 0 $dimension $dimension">""" + "\n"
        )
        svg.append("""  <rect width="$dimension" height="$dimension" fill="$lightColor"/>""" + "\n")

        for (row in 0 until matrix.size) {
            for (column in 0 until matrix.size) {
                if (!matrix[row, column].dark) continue
                val x = column * moduleSizePx
                val y = row * moduleSizePx
                svg.append(
                    """  <rect x="$x" y="$y" width="$moduleSizePx" height="$moduleSizePx" fill="$darkColor"/>""" + "\n"
                )
            }
        }

        svg.append("</svg>")
        return svg.toString()
    }
}

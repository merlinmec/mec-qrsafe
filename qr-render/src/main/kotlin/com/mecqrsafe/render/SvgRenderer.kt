package com.mecqrsafe.render

import com.mecqrsafe.qrengine.matrix.QrMatrix
import java.util.Locale

/**
 * Gera o SVG do export final (`POST .../export?format=svg`) a partir da
 * matriz que acabou de passar na validação — o arquivo baixado é
 * exatamente o que foi conferido, sem depender do renderer do browser
 * (ver seção 8 de `DECISOES-E-TRADEOFFS.txt`).
 *
 * O preview ao vivo no editor NÃO usa isso — usa o renderer canvas
 * próprio no front, que desenha a mesma matriz localmente sem round-trip
 * a cada ajuste cosmético.
 *
 * O desenho é em unidades de módulo (viewBox = lado da matriz) e os
 * módulos escuros vão num único `<path>`, juntando os vizinhos de cada
 * linha num retângulo só: um QR da versão 40 tem ~15 mil módulos escuros,
 * que viram ~4 mil trechos em vez de 15 mil `<rect>`.
 *
 * v1: só módulo quadrado e cor sólida — forma customizada (dots,
 * rounded) e gradiente ficam pra depois.
 */
object SvgRenderer {

    fun render(matrix: QrMatrix, options: RenderOptions = RenderOptions(), widthPx: Int = matrix.size * 10): String {
        require(widthPx > 0) { "widthPx precisa ser positivo" }
        val n = matrix.size
        val svg = StringBuilder()
        svg.append("""<svg xmlns="http://www.w3.org/2000/svg" width="$widthPx" height="$widthPx" viewBox="0 0 $n $n" shape-rendering="crispEdges">""")
        svg.append('\n')
        svg.append("""  <rect width="$n" height="$n" fill="${options.lightColor}"/>""").append('\n')
        svg.append("""  <path fill="${options.darkColor}" d="""").append(darkRunsPath(matrix)).append("\"/>\n")

        options.logoBox?.let { box ->
            svg.append("""  <rect x="${fmt(box.left)}" y="${fmt(box.top)}" width="${fmt(box.side)}" height="${fmt(box.side)}" fill="${options.lightColor}"/>""")
            svg.append('\n')
            options.logoImage?.let { image ->
                val inner = LogoLayout.inner(box)
                svg.append(
                    """  <image x="${fmt(inner.left)}" y="${fmt(inner.top)}" width="${fmt(inner.side)}" height="${fmt(inner.side)}" """ +
                        """preserveAspectRatio="xMidYMid meet" href="${image.toDataUrl()}"/>""",
                )
                svg.append('\n')
            }
        }

        svg.append("</svg>")
        return svg.toString()
    }

    /** Cada sequência de módulos escuros de uma linha vira um retângulo `M x y h w v 1 h -w z`. */
    private fun darkRunsPath(matrix: QrMatrix): String {
        val path = StringBuilder()
        for (row in 0 until matrix.size) {
            var column = 0
            while (column < matrix.size) {
                if (!matrix[row, column].dark) {
                    column++
                    continue
                }
                val start = column
                while (column < matrix.size && matrix[row, column].dark) column++
                val length = column - start
                path.append("M$start ${row}h${length}v1h-${length}z")
            }
        }
        return path.toString()
    }

    private fun fmt(value: Double) = String.format(Locale.ROOT, "%.3f", value).trimEnd('0').trimEnd('.')
}

/** A área da imagem dentro da placa: uma folga em volta, pra o desenho não encostar nos módulos. */
internal object LogoLayout {
    private const val PADDING_SHARE = 0.08

    fun inner(box: LogoBox): LogoBox {
        val padding = box.side * PADDING_SHARE
        return LogoBox(box.left + padding, box.top + padding, box.side - 2 * padding)
    }
}

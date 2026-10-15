package com.mecqrsafe.render

import com.mecqrsafe.qrengine.matrix.ModuleType
import com.mecqrsafe.qrengine.matrix.QrMatrix
import java.util.Locale

/**
 * Gera o SVG do export final (`POST .../export?format=svg`) a partir da
 * matriz que acabou de passar na validação — o arquivo baixado é
 * exatamente o que foi conferido, sem depender do renderer do browser
 * (ver seção 8 de `DECISOES-E-TRADEOFFS.txt`).
 *
 * O preview ao vivo no editor NÃO usa isso — usa o renderer canvas
 * próprio no front, que desenha a mesma matriz com as mesmas medidas de
 * [QrGeometry].
 *
 * O desenho é em unidades de módulo (viewBox = lado da matriz). Com
 * módulos quadrados, os escuros vão num único `<path>`, juntando os
 * vizinhos de cada linha num retângulo só: um QR da versão 40 tem ~15 mil
 * módulos escuros, que viram ~4 mil trechos em vez de 15 mil `<rect>`.
 * Pontos e cantos arredondados vão num `<path>` também, um subcaminho por
 * módulo. Com olhos quadrados e módulos quadrados, os finders entram no
 * mesmo caminho dos módulos; com qualquer outra forma, cada olho é
 * desenhado inteiro (moldura com furo, por evenodd, e o miolo).
 */
object SvgRenderer {

    private const val GRADIENT_ID = "qr-dark"

    fun render(matrix: QrMatrix, options: RenderOptions = RenderOptions(), widthPx: Int = matrix.size * 10): String {
        require(widthPx > 0) { "widthPx precisa ser positivo" }
        val n = matrix.size
        val allSquare = options.moduleShape == ModuleShape.SQUARE && options.eyeShape == EyeShape.SQUARE
        val rendering = if (allSquare) "crispEdges" else "geometricPrecision"
        val svg = StringBuilder()
        svg.append("""<svg xmlns="http://www.w3.org/2000/svg" width="$widthPx" height="$widthPx" viewBox="0 0 $n $n" shape-rendering="$rendering">""")
        svg.append('\n')

        val quietZone = QrGeometry.quietZoneOf(matrix)
        val darkFill = options.gradient?.let { gradient ->
            svg.append("  <defs>").append(gradientDef(gradient, options.darkColor, n, quietZone)).append("</defs>\n")
            "url(#$GRADIENT_ID)"
        } ?: options.darkColor

        svg.append("""  <rect width="$n" height="$n" fill="${options.lightColor}"/>""").append('\n')

        val separateEyes = !allSquare
        val modules = when (options.moduleShape) {
            ModuleShape.SQUARE -> darkRunsPath(matrix, skipFinders = separateEyes)
            ModuleShape.ROUNDED -> perModulePath(matrix) { x, y -> roundedRect(x.toDouble(), y.toDouble(), 1.0, QrGeometry.MODULE_CORNER) }
            ModuleShape.DOTS -> perModulePath(matrix) { x, y -> circle(x + 0.5, y + 0.5, QrGeometry.DOT_DIAMETER / 2) }
        }
        svg.append("""  <path fill="$darkFill" d="""").append(modules).append("\"/>\n")

        if (separateEyes) {
            svg.append("""  <path fill="$darkFill" fill-rule="evenodd" d="""").append(eyesPath(matrix, quietZone, options.eyeShape)).append("\"/>\n")
        }

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

    private fun gradientDef(gradient: Gradient, startColor: String, n: Int, quietZone: Int): String {
        val stops = """<stop offset="0" stop-color="$startColor"/><stop offset="1" stop-color="${gradient.endColor}"/>"""
        return when (gradient.type) {
            GradientType.LINEAR -> {
                val line = QrGeometry.linearGradientLine(n, quietZone, gradient.angleDegrees)
                """<linearGradient id="$GRADIENT_ID" gradientUnits="userSpaceOnUse" x1="${fmt(line.x1)}" y1="${fmt(line.y1)}" """ +
                    """x2="${fmt(line.x2)}" y2="${fmt(line.y2)}">$stops</linearGradient>"""
            }
            GradientType.RADIAL -> {
                val center = fmt(n / 2.0)
                """<radialGradient id="$GRADIENT_ID" gradientUnits="userSpaceOnUse" cx="$center" cy="$center" """ +
                    """r="${fmt(QrGeometry.radialGradientRadius(n, quietZone))}">$stops</radialGradient>"""
            }
        }
    }

    /** Cada sequência de módulos escuros de uma linha vira um retângulo `M x y h w v 1 h -w z`. */
    private fun darkRunsPath(matrix: QrMatrix, skipFinders: Boolean): String {
        fun paints(row: Int, column: Int): Boolean {
            val module = matrix[row, column]
            return module.dark && !(skipFinders && module.type == ModuleType.FINDER_PATTERN)
        }
        val path = StringBuilder()
        for (row in 0 until matrix.size) {
            var column = 0
            while (column < matrix.size) {
                if (!paints(row, column)) {
                    column++
                    continue
                }
                val start = column
                while (column < matrix.size && paints(row, column)) column++
                val length = column - start
                path.append("M$start ${row}h${length}v1h-${length}z")
            }
        }
        return path.toString()
    }

    /** Um subcaminho por módulo escuro (fora os finders, que viram olhos). */
    private fun perModulePath(matrix: QrMatrix, shape: (column: Int, row: Int) -> String): String {
        val path = StringBuilder()
        for (row in 0 until matrix.size) {
            for (column in 0 until matrix.size) {
                val module = matrix[row, column]
                if (module.dark && module.type != ModuleType.FINDER_PATTERN) path.append(shape(column, row))
            }
        }
        return path.toString()
    }

    private fun eyesPath(matrix: QrMatrix, quietZone: Int, shape: EyeShape): String =
        QrGeometry.eyes(matrix, quietZone).joinToString("") { eye ->
            val x = eye.left
            val y = eye.top
            when (shape) {
                EyeShape.SQUARE -> square(x, y, 7.0) + square(x + 1, y + 1, 5.0) + square(x + 2, y + 2, 3.0)
                EyeShape.ROUNDED ->
                    roundedRect(x, y, 7.0, QrGeometry.EYE_OUTER_CORNER) +
                        roundedRect(x + 1, y + 1, 5.0, QrGeometry.EYE_HOLE_CORNER) +
                        roundedRect(x + 2, y + 2, 3.0, QrGeometry.EYE_CENTER_CORNER)
                EyeShape.CIRCLE -> circle(x + 3.5, y + 3.5, 3.5) + circle(x + 3.5, y + 3.5, 2.5) + circle(x + 3.5, y + 3.5, 1.5)
            }
        }

    private fun square(x: Double, y: Double, size: Double) = "M${fmt(x)} ${fmt(y)}h${fmt(size)}v${fmt(size)}h-${fmt(size)}z"

    private fun roundedRect(x: Double, y: Double, size: Double, radius: Double): String {
        val r = fmt(radius)
        val straight = fmt(size - 2 * radius)
        return "M${fmt(x + radius)} ${fmt(y)}h${straight}a$r $r 0 0 1 $r ${r}v${straight}a$r $r 0 0 1 -$r ${r}" +
            "h-${straight}a$r $r 0 0 1 -$r -${r}v-${straight}a$r $r 0 0 1 $r -${r}z"
    }

    private fun circle(cx: Double, cy: Double, radius: Double): String {
        val r = fmt(radius)
        return "M${fmt(cx - radius)} ${fmt(cy)}a$r $r 0 1 0 ${fmt(2 * radius)} 0a$r $r 0 1 0 -${fmt(2 * radius)} 0z"
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

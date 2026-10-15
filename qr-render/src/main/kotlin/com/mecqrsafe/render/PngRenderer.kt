package com.mecqrsafe.render

import com.mecqrsafe.qrengine.matrix.ModuleType
import com.mecqrsafe.qrengine.matrix.QrMatrix
import java.awt.Graphics2D
import java.awt.LinearGradientPaint
import java.awt.Paint
import java.awt.RadialGradientPaint
import java.awt.RenderingHints
import java.awt.Shape
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Gera o PNG do export final, com o mesmo desenho do [SvgRenderer] e as
 * medidas de [QrGeometry]: fundo claro, módulos escuros (na forma e no
 * degradê escolhidos), os três olhos, placa clara do logo e a imagem dele.
 *
 * Cada módulo vira um quadrado de [modulePx] pixels inteiros — módulo com
 * tamanho fracionário borra a borda e piora a leitura. Módulo quadrado sai
 * sem suavização (borda seca); pontos, cantos arredondados e olhos
 * curvos são suavizados, senão viram serrilhado. O lado final é
 * `matrix.size * modulePx`, limitado a [MAX_SIDE_PX] pra um pedido grande
 * não alocar centenas de MB.
 */
object PngRenderer {

    const val MAX_SIDE_PX = 4096

    fun render(matrix: QrMatrix, options: RenderOptions = RenderOptions(), modulePx: Int = 10): ByteArray {
        require(modulePx > 0) { "modulePx precisa ser positivo" }
        val side = matrix.size * modulePx
        require(side <= MAX_SIDE_PX) { "PNG de ${side}px de lado passa do máximo de ${MAX_SIDE_PX}px" }

        val image = BufferedImage(side, side, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        try {
            graphics.color = options.awtColor(options.lightColor)
            graphics.fillRect(0, 0, side, side)

            val quietZone = QrGeometry.quietZoneOf(matrix)
            graphics.paint = darkPaint(matrix, quietZone, options, modulePx)
            drawModules(graphics, matrix, options, modulePx)
            drawEyes(graphics, matrix, quietZone, options, modulePx)

            options.logoBox?.let { box -> drawLogo(graphics, box, options, modulePx) }
        } finally {
            graphics.dispose()
        }

        return ByteArrayOutputStream().use { out ->
            ImageIO.write(image, "png", out)
            out.toByteArray()
        }
    }

    /** O maior tamanho de módulo (em px inteiros) que cabe em [maxSidePx], respeitando [MAX_SIDE_PX]. */
    fun modulePxFor(matrix: QrMatrix, maxSidePx: Int): Int =
        (min(maxSidePx, MAX_SIDE_PX) / matrix.size).coerceAtLeast(1)

    private fun darkPaint(matrix: QrMatrix, quietZone: Int, options: RenderOptions, modulePx: Int): Paint {
        val gradient = options.gradient ?: return options.awtColor(options.darkColor)
        val colors = arrayOf(options.awtColor(options.darkColor), options.awtColor(gradient.endColor))
        val stops = floatArrayOf(0f, 1f)
        return when (gradient.type) {
            GradientType.LINEAR -> {
                val line = QrGeometry.linearGradientLine(matrix.size, quietZone, gradient.angleDegrees)
                LinearGradientPaint(
                    Point2D.Double(line.x1 * modulePx, line.y1 * modulePx),
                    Point2D.Double(line.x2 * modulePx, line.y2 * modulePx),
                    stops,
                    colors,
                )
            }
            GradientType.RADIAL -> {
                val center = matrix.size * modulePx / 2f
                val radius = (QrGeometry.radialGradientRadius(matrix.size, quietZone) * modulePx).toFloat()
                RadialGradientPaint(center, center, radius, stops, colors)
            }
        }
    }

    private fun drawModules(graphics: Graphics2D, matrix: QrMatrix, options: RenderOptions, modulePx: Int) {
        val px = modulePx.toDouble()
        if (options.moduleShape != ModuleShape.SQUARE) {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        }
        for (row in 0 until matrix.size) {
            for (column in 0 until matrix.size) {
                val module = matrix[row, column]
                // Os finders são desenhados inteiros como olhos, depois.
                if (!module.dark || module.type == ModuleType.FINDER_PATTERN) continue
                when (options.moduleShape) {
                    ModuleShape.SQUARE -> graphics.fillRect(column * modulePx, row * modulePx, modulePx, modulePx)
                    ModuleShape.ROUNDED -> graphics.fill(
                        RoundRectangle2D.Double(column * px, row * px, px, px, 2 * QrGeometry.MODULE_CORNER * px, 2 * QrGeometry.MODULE_CORNER * px),
                    )
                    ModuleShape.DOTS -> {
                        val diameter = QrGeometry.DOT_DIAMETER * px
                        val inset = (px - diameter) / 2
                        graphics.fill(Ellipse2D.Double(column * px + inset, row * px + inset, diameter, diameter))
                    }
                }
            }
        }
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT)
    }

    private fun drawEyes(graphics: Graphics2D, matrix: QrMatrix, quietZone: Int, options: RenderOptions, modulePx: Int) {
        val px = modulePx.toDouble()
        if (options.eyeShape != EyeShape.SQUARE) {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        }
        for (eye in QrGeometry.eyes(matrix, quietZone)) {
            val x = eye.left * px
            val y = eye.top * px
            val frame = Area(eyeShape(options.eyeShape, x, y, 7 * px, Part.OUTER, px))
            frame.subtract(Area(eyeShape(options.eyeShape, x + px, y + px, 5 * px, Part.HOLE, px)))
            graphics.fill(frame)
            graphics.fill(eyeShape(options.eyeShape, x + 2 * px, y + 2 * px, 3 * px, Part.CENTER, px))
        }
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT)
    }

    private enum class Part { OUTER, HOLE, CENTER }

    private fun eyeShape(shape: EyeShape, x: Double, y: Double, size: Double, part: Part, px: Double): Shape = when (shape) {
        EyeShape.SQUARE -> Rectangle2D.Double(x, y, size, size)
        EyeShape.CIRCLE -> Ellipse2D.Double(x, y, size, size)
        EyeShape.ROUNDED -> {
            val corner = when (part) {
                Part.OUTER -> QrGeometry.EYE_OUTER_CORNER
                Part.HOLE -> QrGeometry.EYE_HOLE_CORNER
                Part.CENTER -> QrGeometry.EYE_CENTER_CORNER
            } * px
            RoundRectangle2D.Double(x, y, size, size, 2 * corner, 2 * corner)
        }
    }

    private fun drawLogo(graphics: Graphics2D, box: LogoBox, options: RenderOptions, modulePx: Int) {
        graphics.color = options.awtColor(options.lightColor)
        graphics.fillRect(px(box.left, modulePx), px(box.top, modulePx), px(box.side, modulePx), px(box.side, modulePx))
        val logo = options.logoImage ?: return
        // Só a imagem do logo é suavizada na escala; os módulos ficam com borda seca.
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        val inner = LogoLayout.inner(box)
        val scale = min(inner.side / logo.width, inner.side / logo.height) * modulePx
        val width = (logo.width * scale).roundToInt()
        val height = (logo.height * scale).roundToInt()
        val x = px(inner.left, modulePx) + (px(inner.side, modulePx) - width) / 2
        val y = px(inner.top, modulePx) + (px(inner.side, modulePx) - height) / 2
        graphics.drawImage(logo.image, x, y, width, height, null)
    }

    private fun px(modules: Double, modulePx: Int) = (modules * modulePx).roundToInt()
}

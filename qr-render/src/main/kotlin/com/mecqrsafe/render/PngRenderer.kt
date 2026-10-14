package com.mecqrsafe.render

import com.mecqrsafe.qrengine.matrix.QrMatrix
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Gera o PNG do export final, com o mesmo desenho do [SvgRenderer]: fundo
 * claro, módulos escuros, placa clara do logo e a imagem dele por cima.
 *
 * Cada módulo vira um quadrado de [modulePx] pixels inteiros — módulo com
 * tamanho fracionário borra a borda (antialias) e piora a leitura. O
 * lado final é `matrix.size * modulePx`, limitado a [MAX_SIDE_PX] pra um
 * pedido grande não alocar centenas de MB (185 módulos x 50 px já dariam
 * 9250 px de lado, ~340 MB de imagem).
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
            graphics.color = options.awtColor(options.darkColor)
            for (row in 0 until matrix.size) {
                for (column in 0 until matrix.size) {
                    if (matrix[row, column].dark) graphics.fillRect(column * modulePx, row * modulePx, modulePx, modulePx)
                }
            }

            options.logoBox?.let { box ->
                graphics.color = options.awtColor(options.lightColor)
                graphics.fillRect(px(box.left, modulePx), px(box.top, modulePx), px(box.side, modulePx), px(box.side, modulePx))
                options.logoImage?.let { logo ->
                    // Só a imagem do logo é suavizada; os módulos ficam com borda seca.
                    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
                    val inner = LogoLayout.inner(box)
                    val scale = min(inner.side / logo.width, inner.side / logo.height) * modulePx
                    val width = (logo.width * scale).roundToInt()
                    val height = (logo.height * scale).roundToInt()
                    val x = px(inner.left, modulePx) + (px(inner.side, modulePx) - width) / 2
                    val y = px(inner.top, modulePx) + (px(inner.side, modulePx) - height) / 2
                    graphics.drawImage(logo.image, x, y, width, height, null)
                }
            }
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

    private fun px(modules: Double, modulePx: Int) = (modules * modulePx).roundToInt()
}

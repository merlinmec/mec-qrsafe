package com.mecqrsafe.render

import com.mecqrsafe.qrengine.ErrorCorrectionLevel
import com.mecqrsafe.qrengine.QrEncoder
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PngRendererTest {

    private val content = "https://exemplo.com/png"
    private val qr = QrEncoder.encode(content, ErrorCorrectionLevel.H)

    @Test
    fun `png le no leitor real e tem o lado de modulos vezes pixels`() {
        val png = PngRenderer.render(qr.matrix, modulePx = 8)

        assertEquals(qr.matrix.size * 8, ImageIO.read(ByteArrayInputStream(png)).width)
        assertEquals(content, TestImages.read(png))
    }

    @Test
    fun `png com cores, placa e logo desenhado ainda le`() {
        // Logo de ~20% do símbolo no centro: no nível H a validação aprova esse tamanho.
        val side = qr.symbolSize * 0.2
        val center = qr.matrix.size / 2.0
        val options = RenderOptions(
            darkColor = "#1A1A2E",
            lightColor = "#FFF8E7",
            logoBox = LogoBox(center - side / 2, center - side / 2, side),
            logoImage = LogoImage.fromDataUrl(TestImages.pngDataUrl()),
        )

        assertEquals(content, TestImages.read(PngRenderer.render(qr.matrix, options, modulePx = 10)))
    }

    @Test
    fun `tamanho de modulo escolhido cabe no lado pedido`() {
        val modulePx = PngRenderer.modulePxFor(qr.matrix, maxSidePx = 1000)

        assert(qr.matrix.size * modulePx <= 1000)
        assert(qr.matrix.size * (modulePx + 1) > 1000)
        assertEquals(PngRenderer.MAX_SIDE_PX / qr.matrix.size, PngRenderer.modulePxFor(qr.matrix, maxSidePx = 100_000))
    }

    @Test
    fun `recusa png maior que o limite`() {
        assertFailsWith<IllegalArgumentException> {
            PngRenderer.render(qr.matrix, modulePx = PngRenderer.MAX_SIDE_PX / qr.matrix.size + 1)
        }
    }
}

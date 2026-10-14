package com.mecqrsafe.render

import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LogoImageTest {

    private fun rejected(dataUrl: String): String =
        assertFailsWith<IllegalArgumentException> { LogoImage.fromDataUrl(dataUrl) }.message.orEmpty()

    @Test
    fun `aceita png e jpeg e devolve os mesmos bytes como data url`() {
        val png = LogoImage.fromDataUrl(TestImages.pngDataUrl())
        val jpegBytes = TestImages.encode(TestImages.logo(), "jpeg")
        val jpeg = LogoImage.fromDataUrl(TestImages.dataUrl(jpegBytes, "image/jpeg"))

        assertEquals(64 to 48, png.width to png.height)
        assertEquals("image/jpeg", jpeg.mimeType)
        assertEquals(TestImages.dataUrl(jpegBytes, "image/jpeg"), jpeg.toDataUrl())
    }

    @Test
    fun `recusa svg e outros tipos`() {
        assertContains(rejected("data:image/svg+xml;base64,PHN2Zy8+"), "PNG ou JPEG")
        assertContains(rejected("https://exemplo.com/logo.png"), "PNG ou JPEG")
    }

    @Test
    fun `recusa conteudo que nao bate com o tipo declarado`() {
        val jpegBytes = TestImages.encode(TestImages.logo(), "jpeg")

        assertContains(rejected(TestImages.dataUrl(jpegBytes, "image/png")), "não é image/png")
    }

    @Test
    fun `recusa base64 invalido e arquivo corrompido`() {
        assertContains(rejected("data:image/png;base64,====="), "base64")
        val truncated = TestImages.encode(TestImages.logo(), "png").copyOfRange(0, 40)
        assertContains(rejected(TestImages.dataUrl(truncated, "image/png")), "corrompido")
    }

    @Test
    fun `recusa imagem grande demais em bytes`() {
        val huge = ByteArray(LogoImage.MAX_BYTES + 1).also { byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47).copyInto(it) }

        assertContains(rejected(TestImages.dataUrl(huge, "image/png")), "KB")
    }

    @Test
    fun `recusa dimensoes enormes lendo so o cabecalho`() {
        // Poucos KB de arquivo, mas 2049 px de largura: barrado antes de decodificar os pixels.
        val wide = TestImages.encode(BufferedImage(LogoImage.MAX_SIDE_PX + 1, 1, BufferedImage.TYPE_INT_RGB), "png")

        assertContains(rejected(TestImages.dataUrl(wide, "image/png")), "${LogoImage.MAX_SIDE_PX + 1}x1")
    }
}

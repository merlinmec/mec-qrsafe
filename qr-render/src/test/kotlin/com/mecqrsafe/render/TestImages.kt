package com.mecqrsafe.render

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.imageio.ImageIO

internal object TestImages {

    /** Uma imagem com desenho (listras coloridas), pra o logo não ser um quadrado liso. */
    fun logo(width: Int = 64, height: Int = 48): BufferedImage =
        BufferedImage(width, height, BufferedImage.TYPE_INT_RGB).apply {
            for (y in 0 until height) for (x in 0 until width) setRGB(x, y, if ((x / 8 + y / 8) % 2 == 0) 0xE94560 else 0x0F3460)
        }

    fun encode(image: BufferedImage, format: String): ByteArray =
        ByteArrayOutputStream().use { ImageIO.write(image, format, it); it.toByteArray() }

    fun dataUrl(bytes: ByteArray, mime: String) = "data:$mime;base64," + Base64.getEncoder().encodeToString(bytes)

    fun pngDataUrl(image: BufferedImage = logo()) = dataUrl(encode(image, "png"), "image/png")

    /** O texto que o leitor do ZXing tira do PNG, ou `null`. */
    fun read(png: ByteArray): String? = try {
        val image = ImageIO.read(ByteArrayInputStream(png))
        QRCodeReader().decode(
            BinaryBitmap(HybridBinarizer(BufferedImageLuminanceSource(image))),
            mapOf(DecodeHintType.TRY_HARDER to true),
        ).text
    } catch (_: Exception) {
        null
    }
}

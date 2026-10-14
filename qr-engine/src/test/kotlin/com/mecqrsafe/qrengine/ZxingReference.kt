package com.mecqrsafe.qrengine

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel as ZxingLevel
import com.google.zxing.qrcode.encoder.Encoder
import com.google.zxing.qrcode.encoder.QRCode as ZxingQrCode
import com.mecqrsafe.qrengine.matrix.QrMatrix
import java.awt.image.BufferedImage

/**
 * O ZXing nos testes, em dois papéis (seção 4 de `DECISOES-E-TRADEOFFS.txt`):
 * encoder de referência, pra comparar a matriz módulo a módulo, e leitor
 * de verdade, pra provar que o QR gerado aqui é lido.
 */
internal object ZxingReference {

    fun encode(content: String, level: ErrorCorrectionLevel, mask: Int? = null): ZxingQrCode {
        val hints = mutableMapOf<EncodeHintType, Any>()
        if (mask != null) hints[EncodeHintType.QR_MASK_PATTERN] = mask
        return Encoder.encode(content, ZxingLevel.valueOf(level.name), hints)
    }

    /** Desenha a matriz (quiet zone incluída) como imagem e devolve o texto que o leitor do ZXing extrai. */
    fun decode(matrix: QrMatrix, pixelsPerModule: Int = 4): String {
        val side = matrix.size * pixelsPerModule
        val image = BufferedImage(side, side, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until side) {
            for (x in 0 until side) {
                val dark = matrix[y / pixelsPerModule, x / pixelsPerModule].dark
                image.setRGB(x, y, if (dark) 0x000000 else 0xFFFFFF)
            }
        }
        val bitmap = BinaryBitmap(HybridBinarizer(BufferedImageLuminanceSource(image)))
        return QRCodeReader().decode(bitmap, mapOf(DecodeHintType.TRY_HARDER to true)).text
    }

    /** Os módulos do símbolo (sem quiet zone) como texto, uma linha por linha da matriz — falha legível no diff. */
    fun render(isDark: (row: Int, column: Int) -> Boolean, size: Int): String =
        (0 until size).joinToString("\n") { row ->
            (0 until size).joinToString("") { column -> if (isDark(row, column)) "#" else "." }
        }
}

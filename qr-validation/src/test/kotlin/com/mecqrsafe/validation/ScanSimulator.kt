package com.mecqrsafe.validation

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.decoder.Decoder
import com.mecqrsafe.qrengine.QrCode
import java.awt.image.BufferedImage

/**
 * O leitor do ZXing nos testes do qr-validation, em dois níveis:
 *
 *  - [readImage]: o caminho de uma câmera — desenha o QR com o logo por
 *    cima e passa pela detecção (achar os finders, estimar a grade,
 *    amostrar) e pela decodificação.
 *  - [decodeSymbol]: só a decodificação, direto da matriz de módulos, sem
 *    detecção nem amostragem. Isola a pergunta "a correção de erro
 *    aguenta esses módulos errados?", que é a que a
 *    [com.mecqrsafe.validation.rules.LogoCoverageRule] responde.
 */
internal object ScanSimulator {

    /**
     * Logo como o renderer vai desenhar: a arte sobre uma placa sólida na
     * cor CLARA do QR (decisão da seção 14 de `DECISOES-E-TRADEOFFS.txt` —
     * uma placa escura pequena no meio imita o miolo de um finder
     * pattern e confunde a detecção). Devolve o texto lido ou `null`.
     */
    fun readImage(qr: QrCode, logo: LogoStyle, pixelsPerModule: Int = 10): String? {
        val footprint = LogoFootprint(qr, logo)
        val side = qr.matrix.size * pixelsPerModule
        val image = BufferedImage(side, side, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until side) {
            for (x in 0 until side) {
                val moduleX = (x + 0.5) / pixelsPerModule
                val moduleY = (y + 0.5) / pixelsPerModule
                val underLogo = moduleX in footprint.left..footprint.right && moduleY in footprint.top..footprint.bottom
                val dark = !underLogo && qr.matrix[y / pixelsPerModule, x / pixelsPerModule].dark
                image.setRGB(x, y, if (dark) 0x000000 else 0xFFFFFF)
            }
        }
        val bitmap = BinaryBitmap(HybridBinarizer(BufferedImageLuminanceSource(image)))
        return try {
            QRCodeReader().decode(bitmap, mapOf(DecodeHintType.TRY_HARDER to true)).text
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Decodifica o símbolo com os módulos de [flipped] invertidos — o pior
     * caso de um logo, que erra todo módulo que cobre. Devolve o texto ou
     * `null` se a correção de erro não deu conta.
     */
    fun decodeSymbol(qr: QrCode, flipped: Set<Pair<Int, Int>>): String? {
        val bits = BitMatrix(qr.symbolSize)
        for (row in 0 until qr.symbolSize) {
            for (column in 0 until qr.symbolSize) {
                val position = row + qr.quietZone to column + qr.quietZone
                if (qr.matrix[position.first, position.second].dark != (position in flipped)) bits.set(column, row)
            }
        }
        return try {
            Decoder().decode(bits).text
        } catch (_: Exception) {
            null
        }
    }
}

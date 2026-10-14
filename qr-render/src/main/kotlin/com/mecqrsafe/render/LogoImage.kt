package com.mecqrsafe.render

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.Base64
import javax.imageio.ImageIO

/**
 * A imagem do logo que o usuário mandou, já conferida. É arquivo de
 * terceiro entrando no servidor, então nada é decodificado antes de passar
 * pelas checagens, na ordem:
 *
 *  1. data URL só de `image/png` ou `image/jpeg` — SVG fica de fora de
 *     propósito: é XML que pode carregar script e referência externa;
 *  2. no máximo [MAX_BYTES] depois de tirar o base64;
 *  3. os bytes precisam começar com a assinatura do formato declarado;
 *  4. as DIMENSÕES são lidas do cabeçalho, sem decodificar os pixels, e
 *     barradas acima de [MAX_SIDE_PX] — um PNG de poucos KB pode declarar
 *     30000x30000 e estourar a memória ao decodificar (bomba de
 *     descompressão);
 *  5. só então os pixels são decodificados.
 *
 * Qualquer falha vira [IllegalArgumentException] com a mensagem pro
 * usuário (a API transforma em 400).
 */
class LogoImage private constructor(
    val mimeType: String,
    val bytes: ByteArray,
    val image: BufferedImage,
) {
    val width: Int get() = image.width
    val height: Int get() = image.height

    /** Os mesmos bytes conferidos, de volta como data URL — é o que o SVG embute. */
    fun toDataUrl(): String = "data:$mimeType;base64," + Base64.getEncoder().encodeToString(bytes)

    companion object {
        const val MAX_BYTES = 1024 * 1024
        const val MAX_SIDE_PX = 2048

        private val DATA_URL = Regex("^data:(image/png|image/jpeg);base64,([A-Za-z0-9+/=\\s]+)$")
        private val SIGNATURES = mapOf(
            "image/png" to byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A),
            "image/jpeg" to byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()),
        )
        private val FORMAT_NAMES = mapOf("image/png" to "png", "image/jpeg" to "jpeg")

        fun fromDataUrl(dataUrl: String): LogoImage {
            // Teto antes do regex e do base64: 4 caracteres de base64 viram 3 bytes.
            require(dataUrl.length <= MAX_BYTES / 3 * 4 + 64) { "imagem do logo passa de ${MAX_BYTES / 1024} KB" }
            val match = requireNotNull(DATA_URL.matchEntire(dataUrl)) {
                "imagem do logo precisa ser um data URL de PNG ou JPEG (data:image/png;base64,...)"
            }
            val mimeType = match.groupValues[1]
            val bytes = try {
                Base64.getMimeDecoder().decode(match.groupValues[2])
            } catch (_: IllegalArgumentException) {
                throw IllegalArgumentException("imagem do logo com base64 inválido")
            }
            require(bytes.size <= MAX_BYTES) { "imagem do logo passa de ${MAX_BYTES / 1024} KB" }

            val signature = SIGNATURES.getValue(mimeType)
            require(bytes.size >= signature.size && bytes.copyOfRange(0, signature.size).contentEquals(signature)) {
                "o conteúdo da imagem do logo não é $mimeType"
            }
            return LogoImage(mimeType, bytes, decode(bytes, FORMAT_NAMES.getValue(mimeType)))
        }

        private fun decode(bytes: ByteArray, formatName: String): BufferedImage {
            val input = ImageIO.createImageInputStream(ByteArrayInputStream(bytes))
            input.use {
                val reader = ImageIO.getImageReadersByFormatName(formatName).asSequence().firstOrNull()
                    ?: error("sem leitor de $formatName no Java")
                try {
                    reader.input = input
                    val width = reader.getWidth(0)
                    val height = reader.getHeight(0)
                    require(width in 1..MAX_SIDE_PX && height in 1..MAX_SIDE_PX) {
                        "imagem do logo tem ${width}x$height px; o máximo é ${MAX_SIDE_PX}x$MAX_SIDE_PX"
                    }
                    return reader.read(0)
                } catch (e: java.io.IOException) {
                    throw IllegalArgumentException("não deu pra ler a imagem do logo: arquivo corrompido")
                } finally {
                    reader.dispose()
                }
            }
        }
    }
}

package com.mecqrsafe.qrengine

import com.mecqrsafe.qrengine.encoding.DataEncoder
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals

/**
 * Camada 3 da estratégia de teste (seção 4 de `DECISOES-E-TRADEOFFS.txt`):
 * o QR vira imagem e um leitor de verdade (o do ZXing) precisa devolver
 * o texto original. É o teste que prova que o QR escaneia, não só que a
 * matriz "parece certa" — e o único que cobre texto não-ASCII, que vai
 * como UTF-8 (ver [DataEncoder]).
 */
class QrEncoderDecodeTest {

    @ParameterizedTest(name = "\"{0}\" no nível {1}")
    @MethodSource("textos")
    fun `leitor real devolve o texto original`(content: String, level: ErrorCorrectionLevel) {
        val qr = QrEncoder.encode(content, level)

        assertEquals(content, ZxingReference.decode(qr.matrix))
    }

    @ParameterizedTest(name = "versão {0}")
    @MethodSource("versoes")
    fun `leitor real le versoes pequenas medias e a maior`(version: Int) {
        val content = QrEncoderAgainstZxingTest.texto(DataEncoder.maxBytes(version, ErrorCorrectionLevel.M))
        val qr = QrEncoder.encode(content, ErrorCorrectionLevel.M)

        assertEquals(version, qr.version)
        assertEquals(content, ZxingReference.decode(qr.matrix))
    }

    companion object {
        @JvmStatic
        fun textos(): List<Arguments> {
            val textos = listOf(
                "https://exemplo.com.br/cardapio",
                "Olá, ação e coração",
                "日本語のテキスト",
                "emoji também: 🌳🔒",
                "1234567890",
                "TUDO MAIUSCULO",
            )
            return textos.flatMap { content -> ErrorCorrectionLevel.entries.map { Arguments.of(content, it) } }
        }

        @JvmStatic
        fun versoes(): List<Int> = listOf(1, 2, 6, 7, 10, 20, 27, 40)
    }
}

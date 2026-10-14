package com.mecqrsafe.qrengine

import com.google.zxing.qrcode.encoder.QRCode as ZxingQrCode
import com.mecqrsafe.qrengine.encoding.DataEncoder
import com.mecqrsafe.qrengine.version.SymbolCapacity
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Camada 2 da estratégia de teste (seção 4 de `DECISOES-E-TRADEOFFS.txt`):
 * a matriz gerada aqui precisa ser IDÊNTICA, módulo a módulo, à do ZXing
 * pra mesma entrada — mesma versão, mesma máscara escolhida, mesmos bits.
 * Pega erro de posicionamento, de máscara, de format/version info e de
 * qualquer número das tabelas de capacidade.
 *
 * Os textos usam minúsculas de propósito: o ZXing escolhe o modo sozinho
 * e só usa byte mode (o único deste encoder no v1) quando o texto não é
 * só dígitos ou maiúsculas. Texto não-ASCII fica fora daqui porque o
 * ZXing usaria ISO-8859-1 por padrão em vez de UTF-8; ele é coberto pela
 * leitura real em `QrEncoderDecodeTest`.
 */
class QrEncoderAgainstZxingTest {

    @ParameterizedTest(name = "{0} no nível {1}")
    @MethodSource("amostras")
    fun `matriz identica a do ZXing`(content: String, level: ErrorCorrectionLevel) {
        assertSameAsZxing(content, level)
    }

    /**
     * As 40 versões nos 4 níveis, cada uma com o texto mais longo que ela
     * comporta — se qualquer número das tabelas de [SymbolCapacity]
     * estivesse errado, a versão escolhida ou os blocos divergiriam.
     */
    @ParameterizedTest(name = "versão {0}, nível {1}")
    @MethodSource("todasAsVersoes")
    fun `cada versao e nivel na capacidade maxima`(version: Int, level: ErrorCorrectionLevel) {
        val content = texto(DataEncoder.maxBytes(version, level))

        val qr = assertSameAsZxing(content, level)

        assertEquals(version, qr.version, "o texto mais longo da versão $version deveria caber nela")
    }

    @Test
    fun `cada uma das 8 mascaras forcadas bate com o ZXing`() {
        for (mask in 0 until 8) {
            val ours = QrEncoder.encode("mascara forcada", ErrorCorrectionLevel.Q, 0, forcedMask = mask)
            val theirs = ZxingReference.encode("mascara forcada", ErrorCorrectionLevel.Q, mask)
            assertSameModules(ours, theirs, "máscara $mask")
        }
    }

    private fun assertSameAsZxing(content: String, level: ErrorCorrectionLevel): QrCode {
        val ours = QrEncoder.encode(content, level, quietZone = 0)
        val theirs = ZxingReference.encode(content, level)

        assertEquals(theirs.version.versionNumber, ours.version, "versão")
        assertEquals(theirs.maskPattern, ours.mask, "máscara escolhida")
        assertSameModules(ours, theirs, "versão ${ours.version}, nível $level")
        return ours
    }

    private fun assertSameModules(ours: QrCode, theirs: ZxingQrCode, context: String) {
        val size = theirs.matrix.width
        assertEquals(size, ours.matrix.size, "tamanho da matriz ($context)")
        assertEquals(
            ZxingReference.render({ row, column -> theirs.matrix.get(column, row).toInt() == 1 }, size),
            ZxingReference.render({ row, column -> ours.matrix[row, column].dark }, size),
            "módulos diferentes do ZXing ($context)",
        )
    }

    companion object {
        /** Texto determinístico só de minúsculas, pra forçar byte mode no ZXing. */
        fun texto(length: Int): String = String(CharArray(length) { 'a' + (it * 7) % 26 })

        @JvmStatic
        fun amostras(): List<Arguments> {
            val textos = listOf(
                "a",
                "hello world",
                "https://github.com/merlinmec/mec-qrsafe",
                "mailto:contato@exemplo.com?subject=oi",
                "WIFI:T:WPA;S:minha-rede;P:senha secreta 123;;",
                texto(300),
            )
            return textos.flatMap { content -> ErrorCorrectionLevel.entries.map { Arguments.of(content, it) } }
        }

        @JvmStatic
        fun todasAsVersoes(): List<Arguments> =
            (SymbolCapacity.MIN_VERSION..SymbolCapacity.MAX_VERSION).flatMap { version ->
                ErrorCorrectionLevel.entries.map { Arguments.of(version, it) }
            }
    }
}

package com.mecqrsafe.validation

import com.mecqrsafe.qrengine.ErrorCorrectionLevel
import com.mecqrsafe.qrengine.QrCode
import com.mecqrsafe.qrengine.QrEncoder
import com.mecqrsafe.validation.rules.FunctionPatternOverlapRule
import com.mecqrsafe.validation.rules.LogoCoverageRule
import com.mecqrsafe.validation.rules.QuietZoneRule
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * As regras de logo conferidas contra o leitor do ZXing, numa varredura de
 * tamanhos e posições, em duas camadas (ver [ScanSimulator]):
 *
 *  1. A conta da [LogoCoverageRule] é EXATA: invertendo todo módulo que o
 *     logo toca (o pior caso) e decodificando direto a matriz, o
 *     decodificador falha se e somente se a regra bloqueia. Nem
 *     permissiva, nem exagerada.
 *  2. O caminho de uma câmera: com o logo sobre a placa clara que o
 *     renderer desenha, tudo que as regras aprovam o ZXing lê da imagem —
 *     detecção, amostragem e tudo —, em três resoluções.
 *
 * Por que a camada 2 não usa o pior caso também: inverter pixels (ou uma
 * placa escura) cria desenhos que enganam a DETECÇÃO dos finder patterns
 * antes de qualquer dado ser lido — no teste, um logo que destruía 6 de 8
 * codewords possíveis já não era detectado. Isso depende da arte, não da
 * área, e nenhuma regra geométrica decide; por isso a placa clara é
 * decisão de produto (seção 14 de `DECISOES-E-TRADEOFFS.txt`).
 *
 * Nas versões 1 a 3 a camada 1 só exige um lado: a regra desconta os
 * codewords de proteção contra leitura errada que a spec pede e o ZXing
 * não, então ele lê um pouco além do limite da regra (lado seguro).
 */
class LogoRulesAgainstReaderTest {

    private val structural = listOf(FunctionPatternOverlapRule(), QuietZoneRule())
    private val coverage = LogoCoverageRule()

    @ParameterizedTest(name = "{1} no nível {2}")
    @MethodSource("casos")
    fun `conta de codewords bate exatamente com o decodificador`(
        content: String,
        @Suppress("UNUSED_PARAMETER") description: String,
        level: ErrorCorrectionLevel,
    ) {
        val qr = QrEncoder.encode(content, level)
        var approved = 0
        var blocked = 0

        for (logo in logosSemPadraoFixo(qr, level)) {
            val blocks = coverage.evaluate(qr, QrStyle(BLACK, WHITE, logo))?.severity == Severity.BLOCKING
            val decoded = ScanSimulator.decodeSymbol(qr, LogoFootprint(qr, logo).touchedModules().toSet())

            if (!blocks) {
                approved++
                assertEquals(content, decoded, "regra aprovou e o decodificador falhou: $logo, versão ${qr.version}")
            } else if (qr.version >= 4) {
                blocked++
                assertNull(decoded, "regra bloqueou e o decodificador leu: $logo, versão ${qr.version}")
            }
        }

        assertTrue(approved > 0, "a varredura precisa ter logos aprovados")
        assertTrue(qr.version < 4 || blocked > 0, "a varredura precisa ter logos bloqueados")
    }

    @ParameterizedTest(name = "{1} no nível {2}")
    @MethodSource("casos")
    fun `o que as regras aprovam o leitor le da imagem`(
        content: String,
        @Suppress("UNUSED_PARAMETER") description: String,
        level: ErrorCorrectionLevel,
    ) {
        val qr = QrEncoder.encode(content, level)
        val approved = logosSemPadraoFixo(qr, level).filter { coverage.evaluate(qr, QrStyle(BLACK, WHITE, it)) == null }

        assertTrue(approved.isNotEmpty(), "a varredura precisa ter logos aprovados")
        for (logo in approved) {
            for (pixelsPerModule in listOf(4, 7, 10)) {
                assertEquals(
                    content,
                    ScanSimulator.readImage(qr, logo, pixelsPerModule),
                    "regra aprovou e o leitor não leu a imagem: $logo, $pixelsPerModule px/módulo, versão ${qr.version}",
                )
            }
        }
    }

    /** Os logos da varredura que as regras estruturais (padrões fixos e margem) não bloqueiam. */
    private fun logosSemPadraoFixo(qr: QrCode, level: ErrorCorrectionLevel): List<LogoStyle> {
        val centers = listOf(50.0 to 50.0, 35.0 to 50.0, 50.0 to 65.0, 62.0 to 40.0)
        val widths = generateSequence(8.0) { it + 2.0 }.takeWhile { it <= 50.0 }.toList()
        return centers.flatMap { (x, y) -> widths.map { LogoStyle(it, x, y) } }
            .filter { logo -> structural.none { it.evaluate(qr, QrStyle(BLACK, WHITE, logo)) != null } }
            .also { require(it.isNotEmpty()) { "nenhum logo da varredura passou nas regras estruturais no nível $level" } }
    }

    companion object {
        private val BLACK = Color(0, 0, 0)
        private val WHITE = Color(255, 255, 255)

        @JvmStatic
        fun casos(): List<Arguments> {
            val conteudos = listOf(
                "https://exemplo.com" to "URL curta (versão pequena)",
                "https://exemplo.com.br/cardapio/restaurante-do-joao?mesa=12&origem=qr-code-impresso" to "URL média",
                "a".repeat(400) to "texto longo (muitos blocos)",
            )
            return conteudos.flatMap { (content, description) ->
                ErrorCorrectionLevel.entries.map { Arguments.of(content, description, it) }
            }
        }
    }
}

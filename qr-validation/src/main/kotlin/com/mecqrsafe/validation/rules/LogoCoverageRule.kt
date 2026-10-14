package com.mecqrsafe.validation.rules

import com.mecqrsafe.qrengine.QrCode
import com.mecqrsafe.validation.LogoDamage
import com.mecqrsafe.validation.QrStyle
import com.mecqrsafe.validation.ScanabilityRule
import com.mecqrsafe.validation.Severity
import com.mecqrsafe.validation.Violation

/**
 * Área do logo pela conta real, não por um percentual mágico (ver seção 5
 * de `DECISOES-E-TRADEOFFS.txt`): quantos codewords de dado o logo
 * destrói em cada bloco, comparado com quantos aquele bloco corrige no
 * nível escolhido.
 *
 * A contagem de codewords destruídos é a do [LogoDamage]. A conta é por
 * BLOCO porque cada bloco se corrige sozinho: basta o mais atingido
 * estourar pra leitura falhar.
 *
 * Usar a correção inteira no logo deixa zero margem pra sujeira, dobra ou
 * impressão ruim, que também viram erro. Por isso acima de
 * [warningShare] da correção a regra já avisa (WARNING); acima de 100%
 * bloqueia. O limiar de aviso é de partida, como o de contraste —
 * calibrar com teste de escaneamento real.
 */
class LogoCoverageRule(
    private val warningShare: Double = 0.75,
) : ScanabilityRule {

    override fun evaluate(qr: QrCode, style: QrStyle): Violation? {
        val logo = style.logo ?: return null
        val worstBlock = LogoDamage.of(qr, logo).worstBlock
        val capacity = qr.codewords.correctableErrorsPerBlock
        val level = qr.errorCorrectionLevel

        return when {
            worstBlock > capacity -> Violation(
                rule = "logo_coverage",
                severity = Severity.BLOCKING,
                message = "O logo destrói $worstBlock codewords no bloco mais atingido, e o nível $level " +
                    "corrige no máximo $capacity por bloco — o QR não lê. Diminua o logo.",
            )
            worstBlock > capacity * warningShare -> Violation(
                rule = "logo_coverage",
                severity = Severity.WARNING,
                message = "O logo usa $worstBlock dos $capacity codewords que o nível $level corrige no " +
                    "bloco mais atingido — sobra pouca margem pra sujeira, dobra ou impressão ruim.",
            )
            else -> null
        }
    }
}

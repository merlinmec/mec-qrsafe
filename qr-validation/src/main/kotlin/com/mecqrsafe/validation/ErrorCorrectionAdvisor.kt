package com.mecqrsafe.validation

import com.mecqrsafe.qrengine.ErrorCorrectionLevel
import com.mecqrsafe.qrengine.QrCode
import com.mecqrsafe.qrengine.QrEncoder
import com.mecqrsafe.validation.rules.LogoCoverageRule

/**
 * O modo AUTO de correção de erro: escolhe o menor nível em que o logo
 * cabe na correção e devolve o QR já codificado nele. A ORDEM importa
 * (seção 5 de `DECISOES-E-TRADEOFFS.txt`): sobe a correção primeiro e só
 * depois o [ScanabilityEngine] valida — validar no nível errado reprovaria
 * um logo que cabe com mais correção.
 *
 * Por que precisa re-codificar a cada nível em vez de só comparar
 * números: subir o nível pode subir a VERSÃO (mais correção, menos espaço
 * pra dado), o que muda o tamanho do símbolo, a posição dos codewords e
 * quantos deles o mesmo logo cobre. A única conta honesta é com o QR de
 * verdade de cada nível.
 *
 * Sem logo fica no M, o padrão de mercado. Com logo começa no M também
 * (não no L): um logo nunca deveria deixar o QR com MENOS correção do que
 * ele teria sem logo.
 */
class ErrorCorrectionAdvisor(
    private val logoCoverage: LogoCoverageRule = LogoCoverageRule(),
) {

    fun encode(content: String, style: QrStyle): QrCode {
        if (style.logo == null) return QrEncoder.encode(content, DEFAULT_LEVEL, style.quietZoneModules)

        val candidates = LOGO_LEVELS.asSequence()
            .mapNotNull { level -> encodeOrNull(content, level, style.quietZoneModules) }
            .toList()
        // M cabe sempre que algum nível maior cabe; se nem ele coube, o
        // encode dele lança o erro de "conteúdo grande demais" certo.
        if (candidates.isEmpty()) return QrEncoder.encode(content, DEFAULT_LEVEL, style.quietZoneModules)

        return candidates.firstOrNull { logoCoverage.evaluate(it, style) == null }
            ?: candidates.firstOrNull { logoCoverage.evaluate(it, style)?.severity != Severity.BLOCKING }
            ?: candidates.last()
    }

    private fun encodeOrNull(content: String, level: ErrorCorrectionLevel, quietZone: Int): QrCode? =
        try {
            QrEncoder.encode(content, level, quietZone)
        } catch (_: IllegalArgumentException) {
            null // não cabe nesse nível nem na versão 40
        }

    private companion object {
        val DEFAULT_LEVEL = ErrorCorrectionLevel.M
        val LOGO_LEVELS = listOf(ErrorCorrectionLevel.M, ErrorCorrectionLevel.Q, ErrorCorrectionLevel.H)
    }
}

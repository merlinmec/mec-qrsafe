package com.mecqrsafe.validation.rules

import com.mecqrsafe.qrengine.QrCode
import com.mecqrsafe.qrengine.QrEncoder
import com.mecqrsafe.validation.LogoFootprint
import com.mecqrsafe.validation.QrStyle
import com.mecqrsafe.validation.ScanabilityRule
import com.mecqrsafe.validation.Severity
import com.mecqrsafe.validation.Violation

/**
 * Margem clara mínima de 4 módulos em volta do símbolo, sem nada dentro —
 * hard block, não ajustável por correção de erro (ver seção 5 de
 * `DECISOES-E-TRADEOFFS.txt`). É o contraste entre a margem e o símbolo
 * que deixa o leitor achar a borda do QR; colado num fundo estampado ou
 * com um logo vazando pra fora, ele não acha.
 *
 * Duas formas de invadir a margem: exportar com menos de 4 módulos de
 * margem, ou um logo que sai do símbolo.
 */
class QuietZoneRule(
    private val minimumModules: Int = QrEncoder.DEFAULT_QUIET_ZONE,
) : ScanabilityRule {

    override fun evaluate(qr: QrCode, style: QrStyle): Violation? {
        if (qr.quietZone < minimumModules) {
            return violation(
                "A margem clara tem ${qr.quietZone} módulo(s); o mínimo é $minimumModules. " +
                    "Sem ela o leitor não acha a borda do QR, principalmente sobre fundo estampado.",
            )
        }
        val logo = style.logo ?: return null
        if (LogoFootprint(qr, logo).leavesSymbol) {
            return violation(
                "O logo sai do QR e invade a margem clara em volta dele. " +
                    "Aproxime o logo do centro ou diminua o tamanho.",
            )
        }
        return null
    }

    private fun violation(message: String) = Violation(
        rule = "quiet_zone",
        severity = Severity.BLOCKING,
        message = message,
    )
}

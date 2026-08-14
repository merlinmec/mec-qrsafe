package com.mecqrsafe.validation

/**
 * Cor sólida ou ponto de MENOR contraste de um gradiente — é isso que a
 * [ContrastRule] deve avaliar, nunca a média (ver seção 5 de
 * `DECISOES-E-TRADEOFFS.txt`: "aplica-se ao ponto de menor contraste em
 * gradientes, não à média").
 */
data class Color(val red: Int, val green: Int, val blue: Int)

data class LogoStyle(
    val sizePercentOfMatrix: Double,
    // TODO: posição do logo, se v1 permitir mover (hoje assume-se centralizado)
)

/**
 * Parâmetros de customização visual que chegam do front — separado da
 * codificação em si (isso é responsabilidade de `qr-engine`). O
 * `qr-validation` consome isso pra avaliar contraste e cobertura de logo.
 *
 * TODO: hoje mora aqui por conveniência do scaffold. Se crescer a ponto
 * de ser consumido por 3+ módulos, considerar extrair pra um módulo de
 * domínio compartilhado — não fazer isso especulativamente agora.
 */
data class QrStyle(
    val darkModuleColor: Color,
    val lightModuleColor: Color,
    val logo: LogoStyle? = null,
)

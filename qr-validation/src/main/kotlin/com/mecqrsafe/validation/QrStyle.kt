package com.mecqrsafe.validation

import com.mecqrsafe.qrengine.QrEncoder

/**
 * Cor sólida ou ponto de MENOR contraste de um gradiente — é isso que a
 * [com.mecqrsafe.validation.rules.ContrastRule] deve avaliar, nunca a
 * média (ver seção 5 de `DECISOES-E-TRADEOFFS.txt`: "aplica-se ao ponto
 * de menor contraste em gradientes, não à média").
 */
data class Color(val red: Int, val green: Int, val blue: Int)

/**
 * Logo como um quadrado opaco por cima do símbolo. As medidas são
 * relativas ao lado do SÍMBOLO (sem a quiet zone), pra não mudarem de
 * sentido quando o encoder escolhe outra versão: um logo de 25% ocupa um
 * quarto da largura do QR em qualquer versão.
 *
 * Quadrado opaco é o pior caso de um logo de verdade — um PNG com
 * transparência cobre menos módulos. Validar pelo pior caso pode recusar
 * um logo que até leria, mas nunca aprova um que não lê.
 */
data class LogoStyle(
    /** Lado do logo em % do lado do símbolo. 25.0 = um quarto da largura (≈6% da área). */
    val widthPercent: Double,
    /** Centro do logo em % do lado do símbolo, da esquerda pra direita. */
    val centerXPercent: Double = 50.0,
    /** Centro do logo em % do lado do símbolo, de cima pra baixo. */
    val centerYPercent: Double = 50.0,
) {
    init {
        require(widthPercent > 0.0 && widthPercent <= 100.0) { "widthPercent precisa estar entre 0 e 100, veio $widthPercent" }
        require(centerXPercent in 0.0..100.0) { "centerXPercent precisa estar entre 0 e 100, veio $centerXPercent" }
        require(centerYPercent in 0.0..100.0) { "centerYPercent precisa estar entre 0 e 100, veio $centerYPercent" }
    }
}

/**
 * Parâmetros de customização visual que chegam do front — separado da
 * codificação em si (isso é responsabilidade de `qr-engine`). O
 * `qr-validation` consome isso pra avaliar contraste, cobertura de logo e
 * margem.
 *
 * TODO: hoje mora aqui por conveniência do scaffold. Se crescer a ponto
 * de ser consumido por 3+ módulos, considerar extrair pra um módulo de
 * domínio compartilhado — não fazer isso especulativamente agora.
 */
data class QrStyle(
    val darkModuleColor: Color,
    val lightModuleColor: Color,
    val logo: LogoStyle? = null,
    /**
     * Margem clara em volta do símbolo, em módulos, como vai ser
     * exportada. Vai pro encoder como a quiet zone da matriz, e a
     * [com.mecqrsafe.validation.rules.QuietZoneRule] recusa abaixo de 4.
     */
    val quietZoneModules: Int = QrEncoder.DEFAULT_QUIET_ZONE,
) {
    init {
        require(quietZoneModules >= 0) { "quietZoneModules não pode ser negativa, veio $quietZoneModules" }
    }
}

package com.mecqrsafe.validation

import com.mecqrsafe.qrengine.QrEncoder

/** Cor em RGB de 0 a 255 por canal. */
data class Color(val red: Int, val green: Int, val blue: Int) {
    init {
        require(listOf(red, green, blue).all { it in 0..255 }) { "canal de cor fora de 0–255: ($red, $green, $blue)" }
    }

    /** A cor a [t] (0 a 1) do caminho até [other], misturando canal a canal como o canvas e o SVG fazem. */
    fun mix(other: Color, t: Double): Color = Color(
        red = Math.round(red + (other.red - red) * t).toInt(),
        green = Math.round(green + (other.green - green) * t).toInt(),
        blue = Math.round(blue + (other.blue - blue) * t).toInt(),
    )
}

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
    /**
     * Segunda cor dos módulos quando eles vão em degradê (de
     * [darkModuleColor] até esta). Linear ou radial não importa aqui: os
     * dois passam por todas as misturas entre as duas cores, e é o pior
     * ponto desse caminho que a
     * [com.mecqrsafe.validation.rules.ContrastRule] avalia (seção 5 de
     * `DECISOES-E-TRADEOFFS.txt`: "ponto de MENOR contraste, não a média").
     * O tipo e o ângulo do degradê são assunto do qr-render.
     */
    val darkGradientEnd: Color? = null,
) {
    init {
        require(quietZoneModules >= 0) { "quietZoneModules não pode ser negativa, veio $quietZoneModules" }
    }
}

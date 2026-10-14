package com.mecqrsafe.render

/**
 * Onde o logo fica, em unidades de módulo e nas coordenadas da
 * [com.mecqrsafe.qrengine.matrix.QrMatrix] (quiet zone incluída). O
 * qr-render não calcula isso: recebe pronto de quem validou (a geometria
 * mora num lugar só, o `LogoFootprint` do qr-validation), pra o arquivo
 * desenhar o logo exatamente onde a validação conferiu.
 */
data class LogoBox(val left: Double, val top: Double, val side: Double) {
    init {
        require(side > 0) { "o lado do logo precisa ser positivo, veio $side" }
    }
}

/**
 * Aparência do arquivo exportado.
 *
 * O logo, quando há, vai sempre sobre uma placa sólida na cor CLARA,
 * do tamanho de [logoBox] — decisão da seção 15 de
 * `DECISOES-E-TRADEOFFS.txt`: é o caso que a validação modela, e uma
 * placa escura pequena imita o miolo de um finder pattern. A imagem
 * ([logoImage]) é opcional; sem ela sai só a placa, pra quem for aplicar
 * o logo depois.
 */
data class RenderOptions(
    /** Cor em hex, `#RRGGBB`. */
    val darkColor: String = "#000000",
    /** Cor em hex, `#RRGGBB`. */
    val lightColor: String = "#FFFFFF",
    val logoBox: LogoBox? = null,
    val logoImage: LogoImage? = null,
) {
    init {
        require(HEX.matches(darkColor)) { "darkColor precisa ser #RRGGBB, veio $darkColor" }
        require(HEX.matches(lightColor)) { "lightColor precisa ser #RRGGBB, veio $lightColor" }
        require(logoImage == null || logoBox != null) { "imagem de logo sem a posição dele" }
    }

    internal fun awtColor(hex: String) = java.awt.Color(hex.substring(1).toInt(16))

    private companion object {
        val HEX = Regex("^#[0-9A-Fa-f]{6}$")
    }
}

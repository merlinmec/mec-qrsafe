package com.mecqrsafe.render

import com.mecqrsafe.qrengine.ErrorCorrectionLevel
import com.mecqrsafe.qrengine.QrEncoder
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertEquals

/**
 * A garantia por trás das formas: toda combinação que o editor oferece —
 * forma de módulo × forma de olho × degradê, com e sem logo — sai num PNG
 * que o leitor do ZXing lê. As formas não passam por uma regra de
 * validação; é este teste que as autoriza. Uma combinação que não lesse
 * sairia do produto, não seria "avisada".
 */
class ShapesAgainstReaderTest {

    @ParameterizedTest(name = "{0} + olho {1}, degradê {2}, logo {3}, versão {4}")
    @MethodSource("combinacoes")
    fun `toda combinacao oferecida le no leitor real`(
        moduleShape: ModuleShape,
        eyeShape: EyeShape,
        gradient: GradientType?,
        withLogo: Boolean,
        content: String,
    ) {
        val qr = QrEncoder.encode(content, ErrorCorrectionLevel.H)
        val logoSide = qr.symbolSize * 0.2
        val center = qr.matrix.size / 2.0
        val options = RenderOptions(
            darkColor = "#1A1A2E",
            lightColor = "#FFFFFF",
            moduleShape = moduleShape,
            eyeShape = eyeShape,
            gradient = gradient?.let { Gradient(it, endColor = "#6D28D9", angleDegrees = 45.0) },
            logoBox = if (withLogo) LogoBox(center - logoSide / 2, center - logoSide / 2, logoSide) else null,
            logoImage = if (withLogo) LogoImage.fromDataUrl(TestImages.pngDataUrl()) else null,
        )

        for (modulePx in listOf(6, 10)) {
            assertEquals(content, TestImages.read(PngRenderer.render(qr.matrix, options, modulePx)), "$modulePx px por módulo")
        }
    }

    companion object {
        private val CONTENTS = listOf(
            "https://exemplo.com.br/cardapio", // versão pequena
            "https://github.com/merlinmec/mec-qrsafe?origem=portfolio&campanha=qr-impresso-2026", // tem alignment no meio
        )

        @JvmStatic
        fun combinacoes(): List<Arguments> = ModuleShape.entries.flatMap { module ->
            EyeShape.entries.flatMap { eye ->
                listOf(null, GradientType.LINEAR, GradientType.RADIAL).flatMap { gradient ->
                    listOf(false, true).flatMap { logo ->
                        CONTENTS.map { content -> Arguments.of(module, eye, gradient, logo, content) }
                    }
                }
            }
        }
    }
}

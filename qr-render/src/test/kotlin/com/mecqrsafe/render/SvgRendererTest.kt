package com.mecqrsafe.render

import com.mecqrsafe.qrengine.ErrorCorrectionLevel
import com.mecqrsafe.qrengine.QrEncoder
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SvgRendererTest {

    private val qr = QrEncoder.encode("https://exemplo.com/svg", ErrorCorrectionLevel.H)

    private fun parse(svg: String): Element =
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(svg.byteInputStream()).documentElement

    private fun Element.children(tag: String) = getElementsByTagName(tag).let { list -> (0 until list.length).map { list.item(it) as Element } }

    @Test
    fun `svg e xml valido com o tamanho pedido e a viewBox em modulos`() {
        val root = parse(SvgRenderer.render(qr.matrix, widthPx = 600))

        assertEquals("600", root.getAttribute("width"))
        assertEquals("0 0 ${qr.matrix.size} ${qr.matrix.size}", root.getAttribute("viewBox"))
    }

    @Test
    fun `o path desenha exatamente os modulos escuros da matriz`() {
        val path = parse(SvgRenderer.render(qr.matrix)).children("path").single().getAttribute("d")
        val drawn = Array(qr.matrix.size) { BooleanArray(qr.matrix.size) }
        Regex("""M(\d+) (\d+)h(\d+)v1h-\3z""").findAll(path).forEach { run ->
            val (x, y, length) = run.destructured
            for (column in x.toInt() until x.toInt() + length.toInt()) {
                assertFalse(drawn[y.toInt()][column], "módulo ($y, $column) desenhado duas vezes")
                drawn[y.toInt()][column] = true
            }
        }

        for (row in 0 until qr.matrix.size) {
            for (column in 0 until qr.matrix.size) {
                assertEquals(qr.matrix[row, column].dark, drawn[row][column], "módulo ($row, $column)")
            }
        }
    }

    @Test
    fun `usa as cores pedidas`() {
        val root = parse(SvgRenderer.render(qr.matrix, RenderOptions(darkColor = "#1A1A2E", lightColor = "#FFF8E7")))

        assertEquals("#FFF8E7", root.children("rect").first().getAttribute("fill"))
        assertEquals("#1A1A2E", root.children("path").single().getAttribute("fill"))
    }

    @Test
    fun `logo vai sobre uma placa clara com a imagem embutida`() {
        val box = LogoBox(left = 12.0, top = 12.0, side = 9.0)
        val image = LogoImage.fromDataUrl(TestImages.pngDataUrl())
        val root = parse(SvgRenderer.render(qr.matrix, RenderOptions(lightColor = "#FFFFFF", logoBox = box, logoImage = image)))

        val plate = root.children("rect").last()
        assertEquals(listOf("12", "12", "9", "#FFFFFF"), listOf("x", "y", "width", "fill").map { plate.getAttribute(it) })
        val embedded = root.children("image").single()
        assertTrue(embedded.getAttribute("href").startsWith("data:image/png;base64,"))
        assertEquals("xMidYMid meet", embedded.getAttribute("preserveAspectRatio"))
    }

    private fun darkNonFinderModules() = (0 until qr.matrix.size).sumOf { row ->
        (0 until qr.matrix.size).count { column ->
            val module = qr.matrix[row, column]
            module.dark && module.type != com.mecqrsafe.qrengine.matrix.ModuleType.FINDER_PATTERN
        }
    }

    @Test
    fun `pontos viram um circulo por modulo escuro, e os olhos um caminho a parte`() {
        val root = parse(SvgRenderer.render(qr.matrix, RenderOptions(moduleShape = ModuleShape.DOTS, eyeShape = EyeShape.CIRCLE)))
        val (modules, eyes) = root.children("path")

        // Cada círculo são dois arcos ("a"); um subcaminho ("M") por módulo.
        assertEquals(darkNonFinderModules(), modules.getAttribute("d").count { it == 'M' })
        assertEquals("evenodd", eyes.getAttribute("fill-rule"))
        assertEquals(3 * 3, eyes.getAttribute("d").count { it == 'M' }, "3 olhos: moldura, furo e miolo")
        assertEquals("geometricPrecision", root.getAttribute("shape-rendering"))
    }

    @Test
    fun `degrade vira um gradiente em coordenadas de modulo usado pelos modulos e pelos olhos`() {
        val options = RenderOptions(darkColor = "#1A1A2E", eyeShape = EyeShape.ROUNDED, gradient = Gradient(GradientType.LINEAR, "#6D28D9", 90.0))
        val root = parse(SvgRenderer.render(qr.matrix, options))

        val gradient = root.children("linearGradient").single()
        assertEquals("userSpaceOnUse", gradient.getAttribute("gradientUnits"))
        // 90° = de cima pra baixo: x constante, y do topo ao pé do símbolo.
        assertEquals(gradient.getAttribute("x1"), gradient.getAttribute("x2"))
        assertEquals(listOf("#1A1A2E", "#6D28D9"), root.children("stop").map { it.getAttribute("stop-color") })
        assertTrue(root.children("path").all { it.getAttribute("fill") == "url(#qr-dark)" })
    }

    @Test
    fun `degrade radial sai do centro`() {
        val root = parse(SvgRenderer.render(qr.matrix, RenderOptions(gradient = Gradient(GradientType.RADIAL, "#6D28D9"))))
        val gradient = root.children("radialGradient").single()

        assertEquals((qr.matrix.size / 2.0).toString().trimEnd('0').trimEnd('.'), gradient.getAttribute("cx"))
    }

    @Test
    fun `sem imagem sai so a placa`() {
        val root = parse(SvgRenderer.render(qr.matrix, RenderOptions(logoBox = LogoBox(12.0, 12.0, 9.0))))

        assertEquals(2, root.children("rect").size)
        assertTrue(root.children("image").isEmpty())
    }
}

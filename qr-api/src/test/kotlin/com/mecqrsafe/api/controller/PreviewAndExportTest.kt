package com.mecqrsafe.api.controller

import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.jayway.jsonpath.JsonPath
import com.mecqrsafe.api.RequestSizeLimitFilter
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `/preview` e `/export` de ponta a ponta, com o filtro de tamanho de
 * corpo ligado como em produção.
 */
@SpringBootTest
class PreviewAndExportTest {

    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var sizeLimit: RequestSizeLimitFilter

    private lateinit var mockMvc: MockMvc

    private val url = "https://exemplo.com.br/cardapio?mesa=12"

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilter<DefaultMockMvcBuilder>(sizeLimit).build()
    }

    private fun body(style: String = "", errorCorrection: String = "AUTO") = """
        {
          "content": { "type": "STATIC", "value": "$url" },
          "style": { "darkColor": "#1A1A2E", "lightColor": "#FFFFFF"$style },
          "errorCorrection": "$errorCorrection"
        }
    """.trimIndent()

    private fun post(path: String, json: String) = mockMvc.post(path) {
        contentType = MediaType.APPLICATION_JSON
        content = json
    }

    private fun logoDataUrl(): String {
        val image = BufferedImage(40, 40, BufferedImage.TYPE_INT_RGB).apply {
            for (y in 0 until 40) for (x in 0 until 40) setRGB(x, y, if ((x / 5 + y / 5) % 2 == 0) 0xE94560 else 0x0F3460)
        }
        val png = ByteArrayOutputStream().use { ImageIO.write(image, "png", it); it.toByteArray() }
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(png)
    }

    private fun read(png: ByteArray): String = QRCodeReader().decode(
        BinaryBitmap(HybridBinarizer(BufferedImageLuminanceSource(ImageIO.read(ByteArrayInputStream(png))))),
        mapOf(DecodeHintType.TRY_HARDER to true),
    ).text

    @Test
    fun `preview devolve validacao, matriz com tipo e cor por letra e a caixa do logo`() {
        val json = post("/api/v1/qr-codes/preview", body(""", "logo": { "widthPercent": 20 }""")).andExpect {
            status { isOk() }
            jsonPath("$.validation.passed") { value(true) }
            jsonPath("$.matrix.quietZone") { value(4) }
            jsonPath("$.logoBox.side") { isNumber() }
        }.andReturn().response.contentAsString

        val size = JsonPath.read<Int>(json, "$.matrix.size")
        val rows = JsonPath.read<List<String>>(json, "$.matrix.rows")
        assertEquals(size, rows.size)
        assertTrue(rows.all { it.length == size })
        assertTrue(rows.first().all { it == 'q' }, "primeira linha é quiet zone clara")
        assertEquals('F', rows[4][4], "canto do finder de cima-esquerda é escuro")
        assertEquals('s', rows[4][11], "separador ao lado do finder é claro")
        assertEquals('K', rows[size - 4 - 8][4 + 8], "dark module")
        assertTrue(rows.joinToString("").all { it in "FfSsAaTtIiVvKDdq" }, "só letras da legenda")
    }

    @Test
    fun `preview sem logo vem com logoBox nulo`() {
        post("/api/v1/qr-codes/preview", body()).andExpect {
            status { isOk() }
            jsonPath("$.logoBox") { doesNotExist() }
        }
    }

    @Test
    fun `export png com logo devolve um arquivo que o leitor real le`() {
        val result = post(
            "/api/v1/qr-codes/export?format=png&size=800",
            body(""", "logo": { "widthPercent": 20, "imageDataUrl": "${logoDataUrl()}" }"""),
        ).andExpect {
            status { isOk() }
            content { contentType("image/png") }
            header { string("Content-Disposition", containsString("qrcode.png")) }
            header { string("Cache-Control", "no-store") }
        }.andReturn().response

        val png = result.contentAsByteArray
        assertTrue(ImageIO.read(ByteArrayInputStream(png)).width <= 800)
        assertEquals(url, read(png))
    }

    @Test
    fun `export svg com a imagem embutida e CSP fechada`() {
        post(
            "/api/v1/qr-codes/export?format=svg",
            body(""", "logo": { "widthPercent": 20, "imageDataUrl": "${logoDataUrl()}" }"""),
        ).andExpect {
            status { isOk() }
            content { contentType("image/svg+xml") }
            content { string(startsWith("<svg")) }
            content { string(containsString("href=\"data:image/png;base64,")) }
            header { string("Content-Security-Policy", startsWith("default-src 'none'")) }
            header { string("X-Content-Type-Options", "nosniff") }
        }
    }

    @Test
    fun `export recusa com 422 e os motivos quando o QR nao passa`() {
        post("/api/v1/qr-codes/export", body(""", "logo": { "widthPercent": 25 }""", errorCorrection = "L")).andExpect {
            status { isUnprocessableContent() }
            jsonPath("$.error") { value("not_scannable") }
            jsonPath("$.validation.passed") { value(false) }
            jsonPath("$.validation.violations[0].rule") { value("logo_coverage") }
        }
    }

    @Test
    fun `export recusa formato e tamanho invalidos`() {
        post("/api/v1/qr-codes/export?format=gif", body()).andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value(containsString("svg ou png")) }
        }
        post("/api/v1/qr-codes/export?size=10", body()).andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value("validation_error") }
            jsonPath("$.message") { value(containsString("size")) }
        }
    }

    @Test
    fun `export recusa imagem de logo que nao e png nem jpeg`() {
        val svg = "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString("<svg onload=\"alert(1)\"/>".toByteArray())

        post("/api/v1/qr-codes/export", body(""", "logo": { "widthPercent": 20, "imageDataUrl": "$svg" }""")).andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value(containsString("PNG ou JPEG")) }
        }
    }

    @Test
    fun `corpo maior que o limite e recusado antes de ler o JSON`() {
        val huge = "x".repeat(2 * 1024 * 1024 + 1)

        post("/api/v1/qr-codes/export", huge).andExpect {
            status { isPayloadTooLarge() }
            jsonPath("$.error") { value("payload_too_large") }
        }
    }

    @Test
    fun `JSON quebrado vira 400 no formato de erro da API`() {
        post("/api/v1/qr-codes/preview", "{ isso não é json").andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value("bad_request") }
        }
    }
}

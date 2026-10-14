package com.mecqrsafe.api.controller

import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

/**
 * `POST /api/v1/qr-codes/validate` de ponta a ponta: JSON entra, o
 * QrEncoder gera a matriz de verdade, o ScanabilityEngine avalia e o
 * relatório volta em JSON.
 */
@SpringBootTest
class QrCodeControllerTest {

    @Autowired
    private lateinit var context: WebApplicationContext

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build()
    }

    private fun validate(darkColor: String, lightColor: String, text: String = "https://exemplo.com") =
        mockMvc.post("/api/v1/qr-codes/validate") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "content": { "type": "STATIC", "value": "$text" },
                  "style": { "darkColor": "$darkColor", "lightColor": "$lightColor" }
                }
            """.trimIndent()
        }

    @Test
    fun `preto sobre branco passa`() {
        validate("#000000", "#FFFFFF").andExpect {
            status { isOk() }
            jsonPath("$.passed") { value(true) }
            jsonPath("$.violations.length()") { value(0) }
        }
    }

    @Test
    fun `contraste baixo reprova com violacao bloqueante`() {
        validate("#969696", "#C8C8C8").andExpect {
            status { isOk() }
            jsonPath("$.passed") { value(false) }
            jsonPath("$.violations[0].rule") { value("contrast") }
            jsonPath("$.violations[0].severity") { value("BLOCKING") }
        }
    }

    @Test
    fun `cor fora do formato hex vira 400 apontando o campo`() {
        validate("preto", "#FFFFFF").andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value("validation_error") }
            jsonPath("$.message") { value(containsString("style.darkColor")) }
        }
    }

    private fun validateStyle(style: String, errorCorrection: String = "AUTO") =
        mockMvc.post("/api/v1/qr-codes/validate") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {
                  "content": { "type": "STATIC", "value": "https://exemplo.com.br/cardapio?mesa=12" },
                  "style": { "darkColor": "#000000", "lightColor": "#FFFFFF", $style },
                  "errorCorrection": "$errorCorrection"
                }
            """.trimIndent()
        }

    @Test
    fun `sem logo o AUTO fica no M e devolve nivel e versao`() {
        validateStyle(""""quietZoneModules": 4""").andExpect {
            status { isOk() }
            jsonPath("$.passed") { value(true) }
            jsonPath("$.errorCorrectionLevel") { value("M") }
            jsonPath("$.version") { isNumber() }
        }
    }

    @Test
    fun `logo grande no AUTO sobe a correcao e passa`() {
        validateStyle(""""logo": { "widthPercent": 25 }""").andExpect {
            status { isOk() }
            jsonPath("$.passed") { value(true) }
            jsonPath("$.errorCorrectionLevel") { value("H") }
        }
    }

    @Test
    fun `o mesmo logo com nivel L fixo reprova pela conta de codewords`() {
        validateStyle(""""logo": { "widthPercent": 25 }""", errorCorrection = "L").andExpect {
            status { isOk() }
            jsonPath("$.passed") { value(false) }
            jsonPath("$.errorCorrectionLevel") { value("L") }
            jsonPath("$.violations[?(@.rule == 'logo_coverage')].severity") { value("BLOCKING") }
        }
    }

    @Test
    fun `logo no canto reprova por cobrir o finder`() {
        validateStyle(""""logo": { "widthPercent": 15, "centerXPercent": 10, "centerYPercent": 10 }""").andExpect {
            status { isOk() }
            jsonPath("$.passed") { value(false) }
            jsonPath("$.violations[?(@.rule == 'function_pattern_overlap')].severity") { value("BLOCKING") }
        }
    }

    @Test
    fun `margem menor que 4 reprova`() {
        validateStyle(""""quietZoneModules": 2""").andExpect {
            status { isOk() }
            jsonPath("$.passed") { value(false) }
            jsonPath("$.violations[0].rule") { value("quiet_zone") }
        }
    }

    @Test
    fun `logo fora da faixa vira 400 apontando o campo`() {
        validateStyle(""""logo": { "widthPercent": 150 }""").andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value("validation_error") }
            jsonPath("$.message") { value(containsString("style.logo.widthPercent")) }
        }
    }

    @Test
    fun `conteudo que nao cabe em QR nenhum vira 400`() {
        validate("#000000", "#FFFFFF", text = "a".repeat(3000)).andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value("bad_request") }
        }
    }
}

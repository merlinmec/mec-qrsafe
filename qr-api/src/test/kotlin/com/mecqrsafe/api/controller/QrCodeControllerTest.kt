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

    @Test
    fun `conteudo que nao cabe em QR nenhum vira 400`() {
        validate("#000000", "#FFFFFF", text = "a".repeat(3000)).andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value("bad_request") }
        }
    }
}

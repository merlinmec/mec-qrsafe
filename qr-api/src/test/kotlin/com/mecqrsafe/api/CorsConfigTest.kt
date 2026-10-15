package com.mecqrsafe.api

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.ApplicationContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * O editor estático da demo chama a API de outra origem (docs/DEMO.md).
 * Servidor de verdade, não MockMvc: o CORS é um filtro do servlet, e o que
 * importa é a ordem dele em relação ao limite por IP.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "mecqrsafe.cors.allowed-origins=https://editor.exemplo.com",
        "mecqrsafe.rate-limit.requests-per-minute=2",
    ],
)
class CorsConfigTest {

    @LocalServerPort
    private var port = 0

    private val client = HttpClient.newHttpClient()

    private fun send(method: String, path: String, origin: String, body: String? = null): HttpResponse<String> {
        val builder = HttpRequest.newBuilder(URI("http://localhost:$port$path")).header("Origin", origin)
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody())
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type")
        } else {
            builder.method(method, HttpRequest.BodyPublishers.ofString(body)).header("Content-Type", "application/json")
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString())
    }

    private val validateBody = """{"content":{"type":"STATIC","value":"https://exemplo.com"},"style":{"darkColor":"#000000","lightColor":"#FFFFFF"}}"""

    private fun HttpResponse<*>.allowOrigin(): String? = headers().firstValue("Access-Control-Allow-Origin").orElse(null)

    @Test
    fun `preflight da origem do editor e liberado`() {
        val response = send("OPTIONS", "/api/v1/qr-codes/validate", "https://editor.exemplo.com")
        assertEquals(200, response.statusCode())
        assertEquals("https://editor.exemplo.com", response.allowOrigin())
    }

    @Test
    fun `outra origem e recusada`() {
        val response = send("POST", "/api/v1/qr-codes/validate", "https://outro.exemplo.com", validateBody)
        assertEquals(403, response.statusCode())
        assertNull(response.allowOrigin())
    }

    @Test
    fun `o 429 do limite por IP tambem sai com CORS`() {
        val statuses = (1..3).map { send("POST", "/api/v1/qr-codes/validate", "https://editor.exemplo.com", validateBody) }
        assertEquals(listOf(200, 200, 429), statuses.map { it.statusCode() })
        // sem o cabeçalho, o browser esconderia o 429 e o editor mostraria "a API não respondeu"
        assertEquals("https://editor.exemplo.com", statuses.last().allowOrigin())
    }
}

/** Sem a propriedade (local, Docker), nenhum filtro de CORS é registrado. */
@SpringBootTest
class CorsDesligadoPorPadraoTest {

    @Autowired
    private lateinit var context: ApplicationContext

    @Test
    fun `sem origem configurada nao ha filtro de CORS`() {
        assertFalse(context.containsBean("corsFilter"))
    }
}

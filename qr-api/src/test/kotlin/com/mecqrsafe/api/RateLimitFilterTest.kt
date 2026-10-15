package com.mecqrsafe.api

import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RateLimitFilterTest {

    /** Relógio que dá pra adiantar à mão. */
    private class ManualClock(var now: Instant) : Clock() {
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId?) = this
        override fun instant() = now
    }

    private val clock = ManualClock(Instant.parse("2026-10-14T12:00:00Z"))

    private fun call(filter: RateLimitFilter, path: String = "/api/v1/qr-codes/validate", ip: String = "203.0.113.7"): MockHttpServletResponse {
        val request = MockHttpServletRequest("POST", path).apply { remoteAddr = ip }
        val response = MockHttpServletResponse()
        filter.doFilter(request, response, MockFilterChain())
        return response
    }

    @Test
    fun `desligado por padrao deixa tudo passar`() {
        val filter = RateLimitFilter(requestsPerMinute = 0, exportsPerMinute = 0, clock = clock)
        repeat(500) { assertEquals(200, call(filter).status) }
    }

    @Test
    fun `acima do teto por minuto responde 429 com Retry-After e libera no minuto seguinte`() {
        val filter = RateLimitFilter(requestsPerMinute = 3, exportsPerMinute = 0, clock = clock)
        repeat(3) { assertEquals(200, call(filter).status) }

        val blocked = call(filter)
        assertEquals(429, blocked.status)
        assertNotNull(blocked.getHeader("Retry-After"))

        clock.now = clock.now.plusSeconds(60)
        assertEquals(200, call(filter).status)
    }

    @Test
    fun `cada IP tem a sua cota`() {
        val filter = RateLimitFilter(requestsPerMinute = 1, exportsPerMinute = 0, clock = clock)
        assertEquals(200, call(filter, ip = "203.0.113.7").status)
        assertEquals(429, call(filter, ip = "203.0.113.7").status)
        assertEquals(200, call(filter, ip = "198.51.100.4").status)
    }

    @Test
    fun `export tem um teto proprio e menor`() {
        val filter = RateLimitFilter(requestsPerMinute = 100, exportsPerMinute = 2, clock = clock)
        repeat(2) { assertEquals(200, call(filter, "/api/v1/qr-codes/export").status) }
        assertEquals(429, call(filter, "/api/v1/qr-codes/export").status)
        assertEquals(200, call(filter, "/api/v1/qr-codes/validate").status, "o resto da API continua")
    }

    @Test
    fun `atras da Cloudflare a cota e do IP do cabecalho, nao do servidor da borda`() {
        val filter = RateLimitFilter(requestsPerMinute = 2, exportsPerMinute = 0, clientIpHeader = "CF-Connecting-IP", clock = clock)
        fun viaEdge(edge: String) = MockHttpServletResponse().also { response ->
            val request = MockHttpServletRequest("POST", "/api/v1/qr-codes/validate").apply {
                remoteAddr = edge // muda a cada requisição, como os servidores da Cloudflare
                addHeader("CF-Connecting-IP", "203.0.113.7")
            }
            filter.doFilter(request, response, MockFilterChain())
        }

        assertEquals(200, viaEdge("172.70.1.1").status)
        assertEquals(200, viaEdge("172.70.2.2").status)
        assertEquals(429, viaEdge("172.70.3.3").status)
    }

    @Test
    fun `sem o cabecalho configurado o cabecalho do cliente e ignorado`() {
        val filter = RateLimitFilter(requestsPerMinute = 1, exportsPerMinute = 0, clock = clock)
        fun spoofed(fake: String) = MockHttpServletResponse().also { response ->
            val request = MockHttpServletRequest("POST", "/api/v1/qr-codes/validate").apply {
                remoteAddr = "203.0.113.7"
                addHeader("CF-Connecting-IP", fake)
            }
            filter.doFilter(request, response, MockFilterChain())
        }

        assertEquals(200, spoofed("1.1.1.1").status)
        assertEquals(429, spoofed("2.2.2.2").status)
    }

    @Test
    fun `cabecalho configurado mas ausente cai no remoteAddr`() {
        val filter = RateLimitFilter(requestsPerMinute = 1, exportsPerMinute = 0, clientIpHeader = "CF-Connecting-IP", clock = clock)
        assertEquals(200, call(filter).status)
        assertEquals(429, call(filter).status)
    }

    @Test
    fun `saude e arquivos do editor ficam fora do limite`() {
        val filter = RateLimitFilter(requestsPerMinute = 1, exportsPerMinute = 0, clock = clock)
        repeat(5) {
            assertEquals(200, call(filter, "/api/v1/health").status)
            assertEquals(200, call(filter, "/assets/index.js").status)
        }
    }
}

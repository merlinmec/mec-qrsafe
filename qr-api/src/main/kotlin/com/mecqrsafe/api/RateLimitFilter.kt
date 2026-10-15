package com.mecqrsafe.api

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap

/**
 * Limite de requisições por IP para a demo pública (docs/DEMO.md). Fica
 * DESLIGADO por padrão (0) — localmente não faz sentido — e o render.yaml
 * liga com os valores da demo.
 *
 * Janela fixa de um minuto por IP, com um teto geral pra a API e um bem
 * menor pro export, que é o que gasta CPU e memória (desenhar um PNG de
 * até 4096 px). O editor faz uma requisição a cada pausa de digitação,
 * então o teto geral precisa ser folgado; quem passa dele recebe 429 com
 * Retry-After.
 *
 * O IP é o `remoteAddr`: na demo, `server.forward-headers-strategy=native`
 * faz o Tomcat trocar pelo do cliente a partir do X-Forwarded-For do
 * proxy confiável, ignorando o que o próprio cliente inventar no cabeçalho.
 */
@Component
@Order(1) // primeiro: quem já passou do teto nem chega às outras checagens
class RateLimitFilter(
    @param:Value("\${mecqrsafe.rate-limit.requests-per-minute:0}") private val requestsPerMinute: Int,
    @param:Value("\${mecqrsafe.rate-limit.exports-per-minute:0}") private val exportsPerMinute: Int,
    private val clock: Clock = Clock.systemUTC(),
) : OncePerRequestFilter() {

    private class Window(val minute: Long) {
        var requests = 0
        var exports = 0
    }

    private val windows = ConcurrentHashMap<String, Window>()

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        requestsPerMinute <= 0 || !request.requestURI.startsWith("/api/v1/qr-codes/")

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val minute = clock.millis() / 60_000
        val isExport = request.requestURI.endsWith("/export")
        val allowed = synchronized(this) {
            if (windows.size > MAX_TRACKED_IPS) windows.values.removeIf { it.minute != minute }
            val window = windows.compute(request.remoteAddr) { _, current ->
                if (current == null || current.minute != minute) Window(minute) else current
            }!!
            window.requests++
            if (isExport) window.exports++
            window.requests <= requestsPerMinute && (!isExport || exportsPerMinute <= 0 || window.exports <= exportsPerMinute)
        }
        if (allowed) return chain.doFilter(request, response)

        response.status = HttpStatus.TOO_MANY_REQUESTS.value()
        response.setHeader("Retry-After", (60 - (clock.millis() / 1000) % 60).toString())
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        response.writer.write(
            """{"error":"too_many_requests","message":"Muitas requisições em pouco tempo. Espere alguns segundos e tente de novo."}""",
        )
    }

    private companion object {
        /** Acima disso, as janelas de minutos passados são descartadas (memória limitada). */
        const val MAX_TRACKED_IPS = 10_000
    }
}

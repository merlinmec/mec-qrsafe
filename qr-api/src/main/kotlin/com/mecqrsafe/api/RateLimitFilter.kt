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
 * De onde vem o IP: se [clientIpHeader] estiver configurado e presente, é
 * ele; senão, o `remoteAddr`. No Render, que fica atrás da Cloudflare, o
 * `remoteAddr` não serve: o X-Forwarded-For chega como "visitante,
 * servidor-da-Cloudflare", o Tomcat fica com o último endereço público e
 * esse servidor muda a cada requisição — na primeira versão, 16 exports
 * seguidos passaram sem nenhum 429. A Cloudflare manda o IP real em
 * `CF-Connecting-IP` e sobrescreve o que o visitante puser nesse
 * cabeçalho, então é ele que o render.yaml configura. Fora de um proxy
 * que garanta isso, o cabeçalho NÃO deve ser configurado: qualquer um
 * poderia inventar um IP por requisição.
 */
@Component
@Order(1) // primeiro: quem já passou do teto nem chega às outras checagens
class RateLimitFilter(
    @param:Value("\${mecqrsafe.rate-limit.requests-per-minute:0}") private val requestsPerMinute: Int,
    @param:Value("\${mecqrsafe.rate-limit.exports-per-minute:0}") private val exportsPerMinute: Int,
    @param:Value("\${mecqrsafe.rate-limit.client-ip-header:}") private val clientIpHeader: String = "",
    private val clock: Clock = Clock.systemUTC(),
) : OncePerRequestFilter() {

    /** O IP do visitante segundo a configuração (ver o comentário da classe). */
    fun clientIp(request: HttpServletRequest): String =
        clientIpHeader.takeIf { it.isNotBlank() }
            ?.let { request.getHeader(it) }
            ?.substringBefore(',')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: request.remoteAddr

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
            val window = windows.compute(clientIp(request)) { _, current ->
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

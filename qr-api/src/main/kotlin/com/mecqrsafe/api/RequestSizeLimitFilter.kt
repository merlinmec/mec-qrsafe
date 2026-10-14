package com.mecqrsafe.api

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Teto de tamanho do corpo ANTES de o Jackson ler qualquer coisa. O
 * `@Size` da imagem do logo só roda depois que o JSON inteiro já está na
 * memória — sem este filtro, um corpo de centenas de MB seria lido todo
 * pra só então ser recusado. O Spring MVC não tem limite de corpo JSON
 * por padrão (o `max-http-form-post-size` do Tomcat vale só pra
 * formulário).
 *
 * Corpo sem `Content-Length` (chunked) também é recusado: sem o tamanho
 * declarado não dá pra barrar antes de ler, e os clients desta API (o
 * front, curl) sempre mandam o cabeçalho.
 */
@Component
class RequestSizeLimitFilter(
    @param:Value("\${mecqrsafe.max-request-bytes:2097152}") private val maxBytes: Long,
) : OncePerRequestFilter() {

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.method != "POST" || !request.requestURI.startsWith("/api/")

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val length = request.contentLengthLong
        when {
            length < 0 -> reject(response, HttpStatus.LENGTH_REQUIRED, "length_required", "mande o cabeçalho Content-Length")
            length > maxBytes -> reject(
                response,
                HttpStatus.CONTENT_TOO_LARGE,
                "payload_too_large",
                "o corpo passa de ${maxBytes / 1024 / 1024} MB",
            )
            else -> chain.doFilter(request, response)
        }
    }

    private fun reject(response: HttpServletResponse, status: HttpStatus, error: String, message: String) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        response.writer.write("""{"error":"$error","message":"$message"}""")
    }
}

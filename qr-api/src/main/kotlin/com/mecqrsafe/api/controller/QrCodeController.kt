package com.mecqrsafe.api.controller

import com.mecqrsafe.api.dto.PreviewResponse
import com.mecqrsafe.api.dto.ScanabilityResponse
import com.mecqrsafe.api.dto.ValidateRequest
import com.mecqrsafe.api.service.ExportFormat
import com.mecqrsafe.api.service.QrCodeService
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.CacheControl
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Ver seção 6 de `DECISOES-E-TRADEOFFS.txt` pro design da API. Os três
 * endpoints recebem o mesmo corpo (conteúdo + estilo + nível) e são
 * SEMPRE síncronos (seção 5): o cálculo é aritmética pura sobre uma
 * matriz pequena.
 */
@RestController
@RequestMapping("/api/v1/qr-codes")
class QrCodeController(
    private val qrCodeService: QrCodeService,
) {

    /** Só o resultado da validação — o mais leve, pra checar a cada ajuste. */
    @PostMapping("/validate")
    fun validate(@Valid @RequestBody request: ValidateRequest): ScanabilityResponse =
        ScanabilityResponse.from(qrCodeService.validate(request))

    /**
     * Validação + a matriz crua + onde vai a placa do logo, pro renderer
     * canvas do front. O front só precisa chamar de novo quando o
     * conteúdo, o logo ou o nível mudam; cor é cosmética e repinta local.
     */
    @PostMapping("/preview")
    fun preview(@Valid @RequestBody request: ValidateRequest): PreviewResponse =
        PreviewResponse.from(qrCodeService.validate(request))

    /**
     * O arquivo final. POST com o corpo inteiro em vez do `GET /{id}/export`
     * do plano: a v1 não tem persistência, então não existe `{id}` — o
     * servidor recodifica e revalida a partir do próprio pedido, e recusa
     * com 422 (com os motivos) se o QR não passar.
     */
    @PostMapping("/export")
    fun export(
        @Valid @RequestBody request: ValidateRequest,
        @RequestParam(defaultValue = "png") format: String,
        @RequestParam(defaultValue = "1024") @Min(128) @Max(4096) size: Int,
    ): ResponseEntity<ByteArray> {
        val file = qrCodeService.export(request, ExportFormat.parse(format), size)
        val headers = HttpHeaders().apply {
            contentType = MediaType.parseMediaType(file.format.contentType)
            contentDisposition = ContentDisposition.attachment().filename("qrcode.${file.format.extension}").build()
            cacheControl = CacheControl.noStore().headerValue
            set("X-Content-Type-Options", "nosniff")
            // Se alguém abrir o SVG direto no browser: nada de script nem
            // recurso externo, só a imagem do logo embutida como data URL.
            set("Content-Security-Policy", "default-src 'none'; img-src data:; style-src 'unsafe-inline'")
        }
        return ResponseEntity.ok().headers(headers).body(file.bytes)
    }
}

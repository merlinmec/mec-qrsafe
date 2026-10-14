package com.mecqrsafe.api.controller

import com.mecqrsafe.api.dto.ScanabilityResponse
import com.mecqrsafe.api.dto.ValidateRequest
import com.mecqrsafe.api.service.QrCodeService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Ver seção 6 de `DECISOES-E-TRADEOFFS.txt` pro design completo da API
 * (incluindo /preview e /export, ainda não implementados neste scaffold).
 *
 * Validação é SEMPRE síncrona (seção 5) — sem fila/worker, o cálculo é
 * aritmética pura sobre uma matriz pequena.
 */
@RestController
@RequestMapping("/api/v1/qr-codes")
class QrCodeController(
    private val qrCodeService: QrCodeService,
) {

    @PostMapping("/validate")
    fun validate(@Valid @RequestBody request: ValidateRequest): ScanabilityResponse {
        return ScanabilityResponse.from(qrCodeService.validate(request))
    }

    // TODO: POST /preview (matriz + validação juntas)
    // TODO: GET /{id}/export?format=svg|png — recusa gerar se a validação
    //       reprovar, independente do que o client mostrou (seção 5/9)
}

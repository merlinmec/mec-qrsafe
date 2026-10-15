package com.mecqrsafe.api.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Checagem de saúde da hospedagem (healthCheckPath do render.yaml) e alvo
 * de quem quiser saber se o servidor gratuito já acordou. Não codifica
 * nada: responder já prova que a aplicação subiu.
 */
@RestController
class HealthController {
    @GetMapping("/api/v1/health")
    fun health(): Map<String, String> = mapOf("status" to "ok")
}

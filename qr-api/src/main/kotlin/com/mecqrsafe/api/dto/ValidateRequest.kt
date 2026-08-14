package com.mecqrsafe.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

/**
 * Contrato de API — ver seção 6 de `DECISOES-E-TRADEOFFS.txt`. O campo
 * `content` já nasce com discriminador de tipo (`type`) pensando na fase
 * 2 (QR dinâmico) sem exigir retrabalho: v1 só aceita "STATIC".
 */
data class ContentRequest(
    @field:Pattern(regexp = "STATIC", message = "v1 só suporta content.type = STATIC (QR dinâmico é fase 2)")
    val type: String = "STATIC",
    @field:NotBlank
    val value: String,
)

data class QrStyleRequest(
    /** Cor em hex, ex.: "#000000". */
    @field:Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "cor precisa estar em formato hex, ex.: #000000")
    val darkColor: String,
    @field:Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "cor precisa estar em formato hex, ex.: #FFFFFF")
    val lightColor: String,
    val logoSizePercent: Double? = null,
)

enum class ErrorCorrectionRequest { AUTO, L, M, Q, H }

data class ValidateRequest(
    val content: ContentRequest,
    val style: QrStyleRequest,
    val errorCorrection: ErrorCorrectionRequest = ErrorCorrectionRequest.AUTO,
)

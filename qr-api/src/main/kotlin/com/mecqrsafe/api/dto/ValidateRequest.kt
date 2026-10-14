package com.mecqrsafe.api.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

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
    @field:Valid
    val logo: LogoRequest? = null,
    /** Margem clara em volta do QR, em módulos. Abaixo de 4 a validação bloqueia. */
    @field:Min(0)
    @field:Max(32)
    val quietZoneModules: Int = 4,
)

/**
 * Logo como um quadrado sobre o QR, em % do lado do símbolo (sem a
 * margem) — ver `LogoStyle` no qr-validation. Tamanho e posição fora da
 * faixa viram 400 aqui; dentro da faixa, quem decide se o QR ainda lê é
 * a validação.
 */
data class LogoRequest(
    @field:DecimalMin(value = "0.0", inclusive = false, message = "widthPercent precisa ser maior que 0")
    @field:DecimalMax(value = "100.0", message = "widthPercent vai até 100")
    val widthPercent: Double,
    @field:DecimalMin("0.0")
    @field:DecimalMax("100.0")
    val centerXPercent: Double = 50.0,
    @field:DecimalMin("0.0")
    @field:DecimalMax("100.0")
    val centerYPercent: Double = 50.0,
    /**
     * A imagem, como `data:image/png;base64,...` ou JPEG. Só o /export
     * usa (o preview desenha o logo no próprio browser); é conferida lá
     * antes de decodificar — ver `LogoImage` no qr-render. O teto aqui é
     * só o tamanho do texto, ~1 MB de imagem em base64.
     */
    @field:Size(max = 1_400_000, message = "imagem do logo passa de 1 MB")
    val imageDataUrl: String? = null,
)

enum class ErrorCorrectionRequest { AUTO, L, M, Q, H }

data class ValidateRequest(
    // @Valid nos objetos aninhados: sem ele o Bean Validation para no
    // primeiro nível e as regras de ContentRequest/QrStyleRequest (cor em
    // hex, texto não vazio) nunca rodam.
    @field:Valid
    val content: ContentRequest,
    @field:Valid
    val style: QrStyleRequest,
    val errorCorrection: ErrorCorrectionRequest = ErrorCorrectionRequest.AUTO,
)

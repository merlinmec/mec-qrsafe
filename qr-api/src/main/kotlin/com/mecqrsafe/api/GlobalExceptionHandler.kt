package com.mecqrsafe.api

import com.mecqrsafe.api.dto.ScanabilityResponse
import com.mecqrsafe.api.service.ExportBusyException
import com.mecqrsafe.api.service.NotScannableException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import tools.jackson.databind.exc.InvalidFormatException

data class ErrorResponse(val error: String, val message: String?)

/** O export recusado: o mesmo relatório do /validate, pra UI mostrar o que ajustar. */
data class ExportRefusedResponse(val error: String, val message: String, val validation: ScanabilityResponse)

@RestControllerAdvice
class GlobalExceptionHandler {

    /** Mesmo formato dos outros erros, com o campo e o motivo de cada regra que falhou. */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest()
            .body(
                ErrorResponse(
                    "validation_error",
                    ex.bindingResult.fieldErrors.joinToString("; ") { "${it.field}: ${it.defaultMessage}" },
                ),
            )

    /** Parâmetro de query fora da faixa (ex.: `size` do export). */
    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleParameterValidation(ex: HandlerMethodValidationException): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest()
            .body(
                ErrorResponse(
                    "validation_error",
                    ex.parameterValidationResults.joinToString("; ") { result ->
                        "${result.methodParameter.parameterName}: " + result.resolvableErrors.joinToString { it.defaultMessage.orEmpty() }
                    },
                ),
            )

    /**
     * JSON quebrado ou com um valor que não cabe no campo. No segundo caso a
     * mensagem diz qual campo e, se for uma lista fechada (forma do módulo,
     * tipo do degradê...), quais valores ele aceita.
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
        val invalid = generateSequence<Throwable>(ex) { it.cause }.filterIsInstance<InvalidFormatException>().firstOrNull()
            ?: return ResponseEntity.badRequest().body(ErrorResponse("bad_request", "corpo da requisição não é um JSON válido"))
        val field = invalid.path.joinToString(".") { it.propertyName ?: "[${it.index}]" }
        val accepted = invalid.targetType?.takeIf { it.isEnum }?.enumConstants?.joinToString(", ")
        val hint = accepted?.let { " (aceitos: $it)" }.orEmpty()
        return ResponseEntity.badRequest().body(ErrorResponse("validation_error", "$field: valor \"${invalid.value}\" não aceito$hint"))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest()
            .body(ErrorResponse("bad_request", ex.message))

    /** 503: exportações demais ao mesmo tempo (limite de memória da demo). */
    @ExceptionHandler(ExportBusyException::class)
    fun handleExportBusy(ex: ExportBusyException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .header("Retry-After", "5")
            .body(ErrorResponse("busy", ex.message))

    /** 422: o pedido está bem formado, mas o QR resultante não escaneia — o servidor não gera o arquivo. */
    @ExceptionHandler(NotScannableException::class)
    fun handleNotScannable(ex: NotScannableException): ResponseEntity<ExportRefusedResponse> =
        ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
            .body(
                ExportRefusedResponse(
                    "not_scannable",
                    "O QR não passou na validação; ajuste o que está nas violações e exporte de novo.",
                    ScanabilityResponse.from(ex.result),
                ),
            )
}

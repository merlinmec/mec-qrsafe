package com.mecqrsafe.api

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ErrorResponse(val error: String, val message: String?)

@RestControllerAdvice
class GlobalExceptionHandler {

    /**
     * Algumas regras de `qr-validation` ainda lançam `TODO()` — isso vira
     * `NotImplementedError` em Kotlin. Traduzir pra 501 em vez de deixar
     * vazar como 500 cru é o mínimo de honestidade de API: "este endpoint
     * existe, mas essa parte do motor ainda não foi implementada", não
     * "o servidor quebrou".
     */
    @ExceptionHandler(NotImplementedError::class)
    fun handleNotImplemented(ex: NotImplementedError): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(ErrorResponse("not_implemented", ex.message))

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

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> =
        ResponseEntity.badRequest()
            .body(ErrorResponse("bad_request", ex.message))
}

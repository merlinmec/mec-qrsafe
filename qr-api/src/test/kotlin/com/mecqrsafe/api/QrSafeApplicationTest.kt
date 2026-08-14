package com.mecqrsafe.api

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * Teste de fumaça: confirma que o contexto Spring sobe (wiring de
 * QrCodeController -> QrCodeService -> ScanabilityEngine funciona) sem
 * precisar de QrEncoder estar implementado — esse teste não chama
 * nenhum endpoint, só valida que os beans se conectam.
 */
@SpringBootTest
class QrSafeApplicationTest {

    @Test
    fun `contexto sobe sem erro`() {
        // se o contexto não subir, o teste falha antes de chegar aqui
    }
}

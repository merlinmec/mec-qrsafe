package com.mecqrsafe.validation

import com.mecqrsafe.qrengine.ErrorCorrectionLevel
import com.mecqrsafe.validation.rules.LogoCoverageRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ErrorCorrectionAdvisorTest {

    private val content = "https://exemplo.com.br/cardapio/restaurante-do-joao?mesa=12"
    private val advisor = ErrorCorrectionAdvisor()

    private fun style(width: Double?, quietZone: Int = 4) =
        QrStyle(Color(0, 0, 0), Color(255, 255, 255), width?.let { LogoStyle(widthPercent = it) }, quietZone)

    @Test
    fun `sem logo fica no M`() {
        assertEquals(ErrorCorrectionLevel.M, advisor.encode(content, style(null)).errorCorrectionLevel)
    }

    @Test
    fun `logo pequeno nao rebaixa a correcao pra L`() {
        assertEquals(ErrorCorrectionLevel.M, advisor.encode(content, style(5.0)).errorCorrectionLevel)
    }

    @Test
    fun `logo crescendo sobe o nivel e o nivel escolhido aguenta o logo`() {
        val levels = (5..40 step 5).map { width ->
            val qr = advisor.encode(content, style(width.toDouble()))
            val violation = LogoCoverageRule().evaluate(qr, style(width.toDouble()))
            if (qr.errorCorrectionLevel != ErrorCorrectionLevel.H) assertNull(violation, "nível ${qr.errorCorrectionLevel} com logo de $width%")
            qr.errorCorrectionLevel
        }

        assertEquals(levels.sorted(), levels, "o nível só pode subir quando o logo cresce: $levels")
        assertEquals(ErrorCorrectionLevel.H, levels.last())
    }

    @Test
    fun `logo que nao cabe nem no H devolve o H pra regra reprovar`() {
        assertEquals(ErrorCorrectionLevel.H, advisor.encode(content, style(60.0)).errorCorrectionLevel)
    }

    @Test
    fun `usa a margem pedida no estilo`() {
        assertEquals(7, advisor.encode(content, style(10.0, quietZone = 7)).quietZone)
    }

    @Test
    fun `conteudo que so cabe com pouca correcao fica nos niveis em que cabe`() {
        val content = "a".repeat(2000) // cabe no M na versão 40, não no Q nem no H

        assertEquals(ErrorCorrectionLevel.M, advisor.encode(content, style(10.0)).errorCorrectionLevel)
    }

    @Test
    fun `conteudo grande demais pra qualquer nivel e recusado`() {
        assertFailsWith<IllegalArgumentException> { advisor.encode("a".repeat(3000), style(10.0)) }
    }
}

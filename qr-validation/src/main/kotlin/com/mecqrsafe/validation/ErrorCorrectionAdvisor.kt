package com.mecqrsafe.validation

import com.mecqrsafe.qrengine.ErrorCorrectionLevel

/**
 * Quando um logo é adicionado/cresce, recalcula o nível mínimo de
 * correção de erro necessário (L→M→Q→H) — a ORDEM importa: sobe a
 * correção primeiro, valida a área do logo depois (ver seção 5 de
 * `DECISOES-E-TRADEOFFS.txt`).
 *
 * TODO: a conta real é "quantos codewords de dado o logo cobre, e isso
 * ainda cabe na capacidade de correção do nível?" — não um número mágico
 * fixo de % de área. Isso depende de `QrEncoder` calcular quantos
 * codewords cada módulo coberto representa, o que ainda não existe.
 * O corpo abaixo é um placeholder conservador só pra não quebrar o
 * scaffold — NÃO é a regra de negócio final.
 */
object ErrorCorrectionAdvisor {

    fun recommendLevel(logo: LogoStyle?): ErrorCorrectionLevel {
        if (logo == null) return ErrorCorrectionLevel.M

        // Placeholder grosseiro por faixa de tamanho — substituir pela
        // conta real de codewords cobertos vs. capacidade de correção
        // assim que QrEncoder existir de verdade.
        return when {
            logo.sizePercentOfMatrix <= 15.0 -> ErrorCorrectionLevel.Q
            logo.sizePercentOfMatrix <= 30.0 -> ErrorCorrectionLevel.H
            else -> throw IllegalArgumentException(
                "Logo maior que 30% da matriz não tem correção de erro " +
                    "suficiente em nenhum nível — reduza o tamanho do logo."
            )
        }
    }
}

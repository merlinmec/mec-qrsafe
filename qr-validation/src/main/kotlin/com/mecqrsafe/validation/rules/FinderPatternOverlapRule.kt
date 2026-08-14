package com.mecqrsafe.validation.rules

import com.mecqrsafe.qrengine.matrix.QrMatrix
import com.mecqrsafe.validation.QrStyle
import com.mecqrsafe.validation.ScanabilityRule
import com.mecqrsafe.validation.Violation

/**
 * Os 3 quadrados de canto (+ a quiet zone ao redor deles) NUNCA podem ser
 * cobertos por logo — hard block, não é questão de threshold, é ali que
 * o decoder localiza e orienta o QR antes de tentar ler qualquer dado
 * (ver seção 5 de `DECISOES-E-TRADEOFFS.txt`).
 *
 * TODO: ainda não implementado de verdade. Falta modelar a GEOMETRIA do
 * logo sobre a matriz (hoje [com.mecqrsafe.validation.LogoStyle] só tem
 * `sizePercentOfMatrix`, sem posição/formato) — sem isso não dá pra saber
 * quais módulos específicos o logo cobriria. Implementar isso é
 * pré-requisito real antes desta regra funcionar (não é so preencher o
 * corpo da função).
 */
class FinderPatternOverlapRule : ScanabilityRule {
    override fun evaluate(matrix: QrMatrix, style: QrStyle): Violation? {
        TODO(
            "Precisa modelar a geometria do logo (posição, não só tamanho) " +
                "sobre a matriz antes de dar pra checar sobreposição com os " +
                "módulos de ModuleType.FINDER_PATTERN. Ver TODO em QrStyle.kt."
        )
    }
}

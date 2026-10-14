package com.mecqrsafe.validation.rules

import com.mecqrsafe.qrengine.QrCode
import com.mecqrsafe.qrengine.matrix.ModuleType
import com.mecqrsafe.validation.LogoFootprint
import com.mecqrsafe.validation.QrStyle
import com.mecqrsafe.validation.ScanabilityRule
import com.mecqrsafe.validation.Severity
import com.mecqrsafe.validation.Violation

/**
 * O logo não pode encostar nos padrões fixos que o leitor usa ANTES de
 * ler qualquer dado — hard block, não é questão de threshold (ver seção 5
 * de `DECISOES-E-TRADEOFFS.txt`). Nenhum deles tem correção de erro:
 *  - finder patterns + separadores: é por eles que o leitor acha o QR na
 *    imagem e descobre a orientação;
 *  - timing patterns: dão a grade (quantos módulos tem cada linha);
 *  - format info, version info e dark module: dizem o nível, a máscara e
 *    a versão — sem eles o leitor não sabe nem como desfazer a máscara.
 *
 * "Encostar" é qualquer sobreposição, nem que seja numa beirada
 * ([LogoFootprint.touches]), porque aqui não há margem pra perder.
 *
 * O alignment pattern fica de fora de propósito: ele só ajuda a corrigir
 * perspectiva, os leitores seguem sem ele, e um logo centralizado a
 * partir da versão 2 quase sempre cobre o do meio — proibir isso
 * recusaria o caso de uso mais comum. Os testes conferem com o leitor do
 * ZXing que um logo em cima dele ainda lê.
 */
class FunctionPatternOverlapRule : ScanabilityRule {

    override fun evaluate(qr: QrCode, style: QrStyle): Violation? {
        val logo = style.logo ?: return null
        val touched = LogoFootprint(qr, logo).touchedModules()
            .map { (row, column) -> qr.matrix[row, column].type }
            .filter { it in PROTECTED }
            .toSet()
        if (touched.isEmpty()) return null

        val names = PROTECTED.filter { it in touched }.map { DESCRIPTIONS.getValue(it) }.distinct()
        return Violation(
            rule = "function_pattern_overlap",
            severity = Severity.BLOCKING,
            message = "O logo encosta em ${names.joinToString(" e ")} — o leitor depende " +
                "disso pra achar e decodificar o QR, e não há correção de erro ali. " +
                "Diminua o logo ou afaste-o das bordas.",
        )
    }

    private companion object {
        val PROTECTED = listOf(
            ModuleType.FINDER_PATTERN,
            ModuleType.SEPARATOR,
            ModuleType.TIMING_PATTERN,
            ModuleType.FORMAT_INFO,
            ModuleType.VERSION_INFO,
            ModuleType.DARK_MODULE,
        )
        val DESCRIPTIONS = mapOf(
            ModuleType.FINDER_PATTERN to "os quadrados dos cantos",
            ModuleType.SEPARATOR to "os quadrados dos cantos",
            ModuleType.TIMING_PATTERN to "as linhas de sincronismo",
            ModuleType.FORMAT_INFO to "a informação de formato",
            ModuleType.VERSION_INFO to "a informação de versão",
            ModuleType.DARK_MODULE to "a informação de formato",
        )
    }
}

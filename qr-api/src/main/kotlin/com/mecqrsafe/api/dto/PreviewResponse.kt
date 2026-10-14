package com.mecqrsafe.api.dto

import com.mecqrsafe.api.service.ValidationResult
import com.mecqrsafe.qrengine.QrCode
import com.mecqrsafe.qrengine.matrix.ModuleType

/**
 * A matriz crua pro renderer canvas do front (seção 8 de
 * `DECISOES-E-TRADEOFFS.txt`), quiet zone incluída: uma string por linha,
 * um caractere por módulo. A LETRA diz o tipo do módulo e a CAIXA diz a
 * cor — maiúscula = escuro, minúscula = claro:
 *
 *     F finder   S separador   A alignment   T timing
 *     I format info   V version info   K dark module
 *     D dado   Q quiet zone
 *
 * O tipo vai junto porque o front desenha os finders com forma própria
 * (os "olhos" do QR) sem reimplementar a geometria; e texto compacto em
 * vez de array de objetos porque a versão 40 tem 185 x 185 módulos —
 * ~34 mil caracteres aqui contra centenas de KB em JSON de objetos.
 */
data class MatrixResponse(
    val size: Int,
    val quietZone: Int,
    val rows: List<String>,
) {
    companion object {
        private val LETTERS = mapOf(
            ModuleType.FINDER_PATTERN to 'F',
            ModuleType.SEPARATOR to 'S',
            ModuleType.ALIGNMENT_PATTERN to 'A',
            ModuleType.TIMING_PATTERN to 'T',
            ModuleType.FORMAT_INFO to 'I',
            ModuleType.VERSION_INFO to 'V',
            ModuleType.DARK_MODULE to 'K',
            ModuleType.DATA to 'D',
            ModuleType.QUIET_ZONE to 'Q',
        )

        fun from(qr: QrCode): MatrixResponse = MatrixResponse(
            size = qr.matrix.size,
            quietZone = qr.quietZone,
            rows = (0 until qr.matrix.size).map { row ->
                buildString(qr.matrix.size) {
                    for (column in 0 until qr.matrix.size) {
                        val module = qr.matrix[row, column]
                        val letter = LETTERS.getValue(module.type)
                        append(if (module.dark) letter else letter.lowercaseChar())
                    }
                }
            },
        )
    }
}

/** Onde desenhar a placa do logo, em módulos e nas coordenadas da matriz — a mesma caixa que a validação conferiu. */
data class LogoBoxResponse(val left: Double, val top: Double, val side: Double)

/**
 * O que está por dentro da matriz, pra a tela mostrar a conta e não só o
 * veredito: a máscara escolhida, os blocos Reed-Solomon com quanto cada
 * um corrige e quanto o logo destrói, e qual codeword cada módulo carrega
 * (o "raio-X" — dá pra ver a intercalação espalhando o estrago do logo
 * por todos os blocos).
 *
 * `codewordRows` segue as coordenadas de [MatrixResponse.rows]; -1 é
 * módulo sem codeword (padrão fixo, quiet zone ou remainder bit).
 */
data class EngineeringResponse(
    val mask: Int,
    val symbolSize: Int,
    val blockCount: Int,
    val correctableErrorsPerBlock: Int,
    val destroyedPerBlock: List<Int>,
    val codewordBlocks: List<Int>,
    val destroyedCodewords: List<Int>,
    val codewordRows: List<List<Int>>,
) {
    companion object {
        fun from(result: ValidationResult): EngineeringResponse {
            val qr = result.qr
            val codewords = qr.codewords
            return EngineeringResponse(
                mask = qr.mask,
                symbolSize = qr.symbolSize,
                blockCount = codewords.blockCount,
                correctableErrorsPerBlock = codewords.correctableErrorsPerBlock,
                destroyedPerBlock = result.logoDamage.destroyedPerBlock,
                codewordBlocks = (0 until codewords.codewordCount).map { codewords.blockOf(it) },
                destroyedCodewords = result.logoDamage.destroyedCodewords.sorted(),
                codewordRows = (0 until qr.matrix.size).map { row ->
                    (0 until qr.matrix.size).map { column -> codewords.codewordAt(row, column) ?: -1 }
                },
            )
        }
    }
}

data class PreviewResponse(
    val validation: ScanabilityResponse,
    val matrix: MatrixResponse,
    val logoBox: LogoBoxResponse?,
    val engineering: EngineeringResponse,
) {
    companion object {
        fun from(result: ValidationResult): PreviewResponse = PreviewResponse(
            validation = ScanabilityResponse.from(result),
            matrix = MatrixResponse.from(result.qr),
            logoBox = result.logoFootprint?.let { LogoBoxResponse(it.left, it.top, it.side) },
            engineering = EngineeringResponse.from(result),
        )
    }
}

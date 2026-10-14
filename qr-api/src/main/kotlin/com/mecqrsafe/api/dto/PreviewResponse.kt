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

data class PreviewResponse(
    val validation: ScanabilityResponse,
    val matrix: MatrixResponse,
    val logoBox: LogoBoxResponse?,
) {
    companion object {
        fun from(result: ValidationResult): PreviewResponse = PreviewResponse(
            validation = ScanabilityResponse.from(result),
            matrix = MatrixResponse.from(result.qr),
            logoBox = result.logoFootprint?.let { LogoBoxResponse(it.left, it.top, it.side) },
        )
    }
}

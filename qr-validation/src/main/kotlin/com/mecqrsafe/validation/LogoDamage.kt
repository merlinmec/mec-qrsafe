package com.mecqrsafe.validation

import com.mecqrsafe.qrengine.QrCode

/**
 * O estrago que o logo faz na correção de erro: quais codewords ele
 * destrói e quantos caem em cada bloco Reed-Solomon.
 *
 * Um codeword conta como destruído se o logo encosta em qualquer um dos
 * seus 8 módulos ([LogoFootprint.touches]) — um bit errado já troca o
 * byte, e o Reed-Solomon conta erro por codeword. É a conta da
 * [com.mecqrsafe.validation.rules.LogoCoverageRule]; fica pública porque o
 * /preview mostra o mesmo número na tela (a barra de cada bloco e o
 * raio-X da matriz), e os dois não podem divergir.
 */
class LogoDamage private constructor(
    /** Índices (na sequência intercalada) dos codewords que o logo destrói. */
    val destroyedCodewords: Set<Int>,
    /** Quantos codewords destruídos caem em cada bloco, na ordem dos blocos. */
    val destroyedPerBlock: List<Int>,
) {
    val worstBlock: Int get() = destroyedPerBlock.maxOrNull() ?: 0

    companion object {
        fun of(qr: QrCode, logo: LogoStyle?): LogoDamage {
            val destroyed = logo?.let { style ->
                LogoFootprint(qr, style).touchedModules()
                    .mapNotNull { (row, column) -> qr.codewords.codewordAt(row, column) }
                    .toSet()
            }.orEmpty()
            val perBlock = IntArray(qr.codewords.blockCount)
            for (codeword in destroyed) perBlock[qr.codewords.blockOf(codeword)]++
            return LogoDamage(destroyed, perBlock.toList())
        }
    }
}

package com.mecqrsafe.qrengine.encoding

import com.mecqrsafe.qrengine.ErrorCorrectionLevel
import com.mecqrsafe.qrengine.version.SymbolCapacity

/**
 * Estágios 1 e 2 do pipeline (seção 4 de `DECISOES-E-TRADEOFFS.txt`):
 * escolhe a menor versão em que o conteúdo cabe e monta o bitstream de
 * dados em byte mode.
 *
 * Só byte mode no v1: numeric e alphanumeric gastam menos bits pra
 * conteúdo só de dígitos ou maiúsculas, mas isso é otimização de tamanho,
 * não de corretude — qualquer texto cabe em byte mode.
 *
 * O texto vai como UTF-8 sem segmento ECI. ECI é o jeito "oficial" de
 * dizer qual charset os bytes usam, mas leitores antigos tropeçam nele;
 * sem ECI, os leitores atuais reconhecem UTF-8 pelos próprios bytes (e
 * texto ASCII sai idêntico em qualquer charset).
 */
internal object DataEncoder {

    private const val BYTE_MODE_INDICATOR = 0b0100
    private const val MODE_INDICATOR_BITS = 4
    private const val TERMINATOR_MAX_BITS = 4
    private val PAD_CODEWORDS = intArrayOf(0xEC, 0x11)

    /** O campo de contagem do byte mode cresce de 8 pra 16 bits a partir da versão 10. */
    fun characterCountBits(version: Int): Int = if (version <= 9) 8 else 16

    /** Menor versão em que [byteCount] bytes cabem no nível [level]. */
    fun chooseVersion(byteCount: Int, level: ErrorCorrectionLevel): Int =
        (SymbolCapacity.MIN_VERSION..SymbolCapacity.MAX_VERSION).firstOrNull { version ->
            bitsNeeded(byteCount, version) <= SymbolCapacity.dataCodewords(version, level) * 8
        } ?: throw IllegalArgumentException(
            "Conteúdo com $byteCount bytes não cabe num QR code no nível $level " +
                "(máximo: ${maxBytes(SymbolCapacity.MAX_VERSION, level)} bytes na versão 40). " +
                "Encurte o texto ou use um nível de correção menor.",
        )

    /** Quantos bytes cabem em byte mode na versão e nível dados. */
    fun maxBytes(version: Int, level: ErrorCorrectionLevel): Int =
        (SymbolCapacity.dataCodewords(version, level) * 8 - MODE_INDICATOR_BITS - characterCountBits(version)) / 8

    /**
     * Bitstream completo, já em codewords: modo + contagem + dados +
     * terminador + alinhamento a byte + bytes de padding até encher a
     * capacidade de dados da versão.
     */
    fun encode(data: ByteArray, version: Int, level: ErrorCorrectionLevel): IntArray {
        val capacityBits = SymbolCapacity.dataCodewords(version, level) * 8
        require(bitsNeeded(data.size, version) <= capacityBits) {
            "${data.size} bytes não cabem na versão $version, nível $level"
        }

        val buffer = BitBuffer()
        buffer.append(BYTE_MODE_INDICATOR, MODE_INDICATOR_BITS)
        buffer.append(data.size, characterCountBits(version))
        for (byte in data) {
            buffer.append(byte.toInt() and 0xFF, 8)
        }

        // Terminador: até 4 zeros, menos se a capacidade acabar antes.
        buffer.append(0, minOf(TERMINATOR_MAX_BITS, capacityBits - buffer.size))
        buffer.append(0, (8 - buffer.size % 8) % 8)

        // Padding alternando 0xEC e 0x11 (11101100 e 00010001): a spec
        // escolheu esses bytes pra o espaço vazio não virar áreas grandes
        // de uma cor só, que atrapalham o leitor.
        var padIndex = 0
        while (buffer.size < capacityBits) {
            buffer.append(PAD_CODEWORDS[padIndex % 2], 8)
            padIndex++
        }
        return buffer.toCodewords()
    }

    private fun bitsNeeded(byteCount: Int, version: Int): Int =
        MODE_INDICATOR_BITS + characterCountBits(version) + byteCount * 8
}

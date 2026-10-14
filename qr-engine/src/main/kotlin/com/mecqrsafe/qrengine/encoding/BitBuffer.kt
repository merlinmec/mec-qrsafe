package com.mecqrsafe.qrengine.encoding

/**
 * Sequência de bits que cresce só pelo fim — é como o QR monta o
 * bitstream de dados (modo, contagem de caracteres, dados, terminador,
 * padding) antes de virar codewords de 8 bits.
 *
 * Os campos do bitstream não são alinhados a byte (o modo tem 4 bits, a
 * contagem tem 8 ou 16), por isso não dá pra montar direto num ByteArray.
 */
internal class BitBuffer {

    private val bits = ArrayList<Boolean>()

    val size: Int
        get() = bits.size

    /** Anexa os [length] bits menos significativos de [value], do mais alto pro mais baixo. */
    fun append(value: Int, length: Int) {
        require(length in 0..31) { "length precisa estar entre 0 e 31, veio $length" }
        require(value ushr length == 0) { "valor $value não cabe em $length bits" }
        for (i in length - 1 downTo 0) {
            bits.add((value ushr i) and 1 == 1)
        }
    }

    /** Agrupa os bits em codewords de 8 bits. Só faz sentido com o buffer já alinhado a byte. */
    fun toCodewords(): IntArray {
        check(size % 8 == 0) { "bitstream com $size bits não está alinhado a byte" }
        return IntArray(size / 8) { index ->
            var codeword = 0
            for (bit in 0 until 8) {
                codeword = (codeword shl 1) or (if (bits[index * 8 + bit]) 1 else 0)
            }
            codeword
        }
    }
}

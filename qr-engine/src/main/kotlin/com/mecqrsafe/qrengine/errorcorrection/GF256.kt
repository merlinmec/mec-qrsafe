package com.mecqrsafe.qrengine.errorcorrection

/**
 * Aritmética sobre o corpo finito GF(256) usado pelo QR code (polinômio
 * primitivo x^8 + x^4 + x^3 + x^2 + 1, ou 0x11D), com gerador α = 2.
 *
 * É a base de tudo em [ReedSolomonEncoder] — sem entender por que a
 * multiplicação "embrulha" nesse campo, o resto do encoder vira mágica
 * decorada. Ver seção 4 de `DECISOES-E-TRADEOFFS.txt` e o tema de estudo
 * "Aritmética em corpos finitos, GF(256)" em `mec-explain.txt`.
 *
 * Implementação via tabelas de log/antilog pré-computadas — é a técnica
 * padrão usada por implementações reais para transformar multiplicação em
 * soma (e divisão em subtração) sobre os expoentes de α.
 */
object GF256 {

    private const val PRIMITIVE_POLYNOMIAL = 0x11D
    private const val FIELD_SIZE = 256
    private const val ORDER = FIELD_SIZE - 1 // 255 — ordem do grupo multiplicativo

    /** exp[i] = α^i. Tabela estendida até 2*ORDER pra evitar mod em todo lookup. */
    private val expTable = IntArray(2 * ORDER)

    /** log[a] = i tal que α^i = a. log[0] não é definido (0 não tem log). */
    private val logTable = IntArray(FIELD_SIZE)

    init {
        var value = 1
        for (i in 0 until ORDER) {
            expTable[i] = value
            logTable[value] = i
            value = value shl 1
            if (value and 0x100 != 0) {
                value = value xor PRIMITIVE_POLYNOMIAL
            }
        }
        // Duplica a tabela pra permitir exp[i] com i em [0, 2*ORDER) sem mod.
        for (i in ORDER until 2 * ORDER) {
            expTable[i] = expTable[i - ORDER]
        }
    }

    /** α^power, com power podendo ser qualquer inteiro >= 0 (não só < 255). */
    fun exp(power: Int): Int = expTable[power % ORDER]

    fun log(value: Int): Int {
        require(value != 0) { "log(0) não é definido em GF(256)" }
        return logTable[value]
    }

    fun multiply(a: Int, b: Int): Int {
        if (a == 0 || b == 0) return 0
        return expTable[logTable[a] + logTable[b]]
    }

    fun divide(a: Int, b: Int): Int {
        require(b != 0) { "Divisão por zero em GF(256)" }
        if (a == 0) return 0
        return expTable[(logTable[a] - logTable[b] + ORDER) % ORDER]
    }

    fun inverse(a: Int): Int {
        require(a != 0) { "0 não tem inverso multiplicativo em GF(256)" }
        return expTable[ORDER - logTable[a]]
    }
}

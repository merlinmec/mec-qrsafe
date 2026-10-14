package com.mecqrsafe.qrengine.errorcorrection

import com.mecqrsafe.qrengine.ErrorCorrectionLevel
import com.mecqrsafe.qrengine.version.SymbolCapacity

/**
 * Estágio 4 do pipeline: divide os codewords de dados em blocos, calcula
 * a correção Reed-Solomon de cada bloco e intercala tudo.
 *
 * Por que intercalar em vez de pôr um bloco depois do outro: um borrão ou
 * um logo destrói módulos VIZINHOS. Intercalando, codewords vizinhos na
 * matriz pertencem a blocos diferentes, e o estrago se espalha por vários
 * blocos em vez de estourar a capacidade de correção de um só.
 *
 * Os blocos podem ter dois tamanhos: quando os codewords não dividem por
 * igual, os últimos blocos ficam com 1 codeword de dado a mais. A
 * correção tem sempre o mesmo tamanho em todos os blocos.
 */
internal object CodewordInterleaver {

    /** Um lugar da sequência intercalada: de qual bloco vem e em que posição dele. */
    data class Slot(val block: Int, val index: Int, val errorCorrection: Boolean)

    fun addErrorCorrectionAndInterleave(
        dataCodewords: IntArray,
        version: Int,
        level: ErrorCorrectionLevel,
    ): IntArray {
        require(dataCodewords.size == SymbolCapacity.dataCodewords(version, level)) {
            "esperava ${SymbolCapacity.dataCodewords(version, level)} codewords de dado, vieram ${dataCodewords.size}"
        }

        val eccPerBlock = SymbolCapacity.eccCodewordsPerBlock(version, level)
        val dataBlocks = ArrayList<IntArray>()
        val eccBlocks = ArrayList<IntArray>()
        var offset = 0
        for (size in dataBlockSizes(version, level)) {
            val data = dataCodewords.copyOfRange(offset, offset + size)
            offset += size
            dataBlocks.add(data)
            eccBlocks.add(ReedSolomonEncoder.encode(data, eccPerBlock))
        }

        return slots(version, level)
            .map { slot -> (if (slot.errorCorrection) eccBlocks else dataBlocks)[slot.block][slot.index] }
            .toIntArray()
    }

    /**
     * A ordem da intercalação, sem os valores: o i-ésimo [Slot] diz de onde
     * vem o i-ésimo codeword da sequência final. Primeiro os dados, coluna
     * por coluna (o 1º codeword de cada bloco, depois o 2º de cada
     * bloco... — os blocos curtos acabam um antes), depois a correção, do
     * mesmo jeito.
     *
     * É a mesma ordem que [addErrorCorrectionAndInterleave] usa pra montar a
     * sequência, e o que permite ao [com.mecqrsafe.qrengine.CodewordMap]
     * dizer a que bloco pertence cada codeword desenhado na matriz.
     */
    fun slots(version: Int, level: ErrorCorrectionLevel): List<Slot> {
        val sizes = dataBlockSizes(version, level)
        val eccPerBlock = SymbolCapacity.eccCodewordsPerBlock(version, level)
        val result = ArrayList<Slot>(SymbolCapacity.totalCodewords(version))
        for (index in 0 until sizes.max()) {
            for (block in sizes.indices) {
                if (index < sizes[block]) result.add(Slot(block, index, errorCorrection = false))
            }
        }
        for (index in 0 until eccPerBlock) {
            for (block in sizes.indices) {
                result.add(Slot(block, index, errorCorrection = true))
            }
        }
        check(result.size == SymbolCapacity.totalCodewords(version)) {
            "intercalação gerou ${result.size} codewords, esperava ${SymbolCapacity.totalCodewords(version)}"
        }
        return result
    }

    /** Quantos codewords de dado vão em cada bloco: os curtos primeiro, os de 1 a mais no fim. */
    private fun dataBlockSizes(version: Int, level: ErrorCorrectionLevel): IntArray {
        val blockCount = SymbolCapacity.errorCorrectionBlocks(version, level)
        val eccPerBlock = SymbolCapacity.eccCodewordsPerBlock(version, level)
        val totalCodewords = SymbolCapacity.totalCodewords(version)
        val shortBlockCount = blockCount - totalCodewords % blockCount
        val shortBlockDataSize = totalCodewords / blockCount - eccPerBlock
        return IntArray(blockCount) { block -> shortBlockDataSize + if (block < shortBlockCount) 0 else 1 }
    }
}

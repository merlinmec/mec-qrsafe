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

    fun addErrorCorrectionAndInterleave(
        dataCodewords: IntArray,
        version: Int,
        level: ErrorCorrectionLevel,
    ): IntArray {
        require(dataCodewords.size == SymbolCapacity.dataCodewords(version, level)) {
            "esperava ${SymbolCapacity.dataCodewords(version, level)} codewords de dado, vieram ${dataCodewords.size}"
        }

        val blockCount = SymbolCapacity.errorCorrectionBlocks(version, level)
        val eccPerBlock = SymbolCapacity.eccCodewordsPerBlock(version, level)
        val totalCodewords = SymbolCapacity.totalCodewords(version)
        val shortBlockCount = blockCount - totalCodewords % blockCount
        val shortBlockDataSize = totalCodewords / blockCount - eccPerBlock

        val dataBlocks = ArrayList<IntArray>(blockCount)
        val eccBlocks = ArrayList<IntArray>(blockCount)
        var offset = 0
        for (block in 0 until blockCount) {
            val size = shortBlockDataSize + if (block < shortBlockCount) 0 else 1
            val data = dataCodewords.copyOfRange(offset, offset + size)
            offset += size
            dataBlocks.add(data)
            eccBlocks.add(ReedSolomonEncoder.encode(data, eccPerBlock))
        }

        val result = IntArray(totalCodewords)
        var position = 0
        // Primeiro os dados, coluna por coluna: o 1º codeword de cada
        // bloco, depois o 2º de cada bloco... (os blocos curtos acabam um
        // antes). Depois a correção, do mesmo jeito.
        for (index in 0..shortBlockDataSize) {
            for (block in dataBlocks) {
                if (index < block.size) result[position++] = block[index]
            }
        }
        for (index in 0 until eccPerBlock) {
            for (block in eccBlocks) {
                result[position++] = block[index]
            }
        }
        check(position == totalCodewords) { "intercalação gerou $position codewords, esperava $totalCodewords" }
        return result
    }
}

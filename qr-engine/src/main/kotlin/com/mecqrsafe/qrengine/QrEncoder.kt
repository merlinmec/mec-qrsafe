package com.mecqrsafe.qrengine

import com.mecqrsafe.qrengine.encoding.DataEncoder
import com.mecqrsafe.qrengine.errorcorrection.CodewordInterleaver
import com.mecqrsafe.qrengine.matrix.DataPlacement
import com.mecqrsafe.qrengine.matrix.FormatInfo
import com.mecqrsafe.qrengine.matrix.FunctionPatterns
import com.mecqrsafe.qrengine.matrix.Masking
import com.mecqrsafe.qrengine.matrix.ModuleType
import com.mecqrsafe.qrengine.matrix.QrMatrixBuilder
import com.mecqrsafe.qrengine.matrix.SymbolGrid
import com.mecqrsafe.qrengine.version.SymbolCapacity

/**
 * Fachada pública do encoder QR — o único ponto de entrada que qr-api e
 * qr-validation deveriam usar (não chamar as classes internas de
 * encoding/matrix/errorcorrection diretamente de fora deste módulo; elas
 * são `internal` justamente pra isso).
 *
 * Pipeline (seção 4 de `DECISOES-E-TRADEOFFS.txt` e ISO/IEC 18004), cada
 * estágio num arquivo próprio e testado sozinho:
 *
 *   1-2. [DataEncoder]: menor versão que comporta o texto + bitstream em
 *        byte mode (modo, contagem, dados, terminador, padding)
 *   3-4. [CodewordInterleaver]: blocos, Reed-Solomon de cada bloco
 *        ([com.mecqrsafe.qrengine.errorcorrection.ReedSolomonEncoder]) e
 *        intercalação
 *   5.   [FunctionPatterns] + [DataPlacement]: padrões fixos e os bits de
 *        dado em zigue-zague
 *   6.   [Masking]: as 8 máscaras, fica a de menor penalidade
 *   7.   [FormatInfo]: format/version info com BCH
 */
object QrEncoder {

    /** Margem clara exigida pela spec em volta do símbolo, em módulos. */
    const val DEFAULT_QUIET_ZONE = 4

    /**
     * Codifica [content] (como UTF-8) no nível [level], na menor versão
     * em que ele cabe.
     *
     * A matriz devolvida já inclui a quiet zone de [quietZone] módulos
     * claros em volta, com [ModuleType.QUIET_ZONE] — ela faz parte do QR
     * (sem ela o leitor não acha a borda do símbolo), e é essa área que a
     * regra de quiet zone do qr-validation confere.
     *
     * @throws IllegalArgumentException se o conteúdo não couber nem na versão 40.
     */
    fun encode(
        content: String,
        level: ErrorCorrectionLevel,
        quietZone: Int = DEFAULT_QUIET_ZONE,
    ): QrCode = encode(content, level, quietZone, forcedMask = null)

    /** Igual ao público, mas permite fixar a máscara — usado nos testes contra o ZXing. */
    internal fun encode(
        content: String,
        level: ErrorCorrectionLevel,
        quietZone: Int,
        forcedMask: Int?,
    ): QrCode {
        require(quietZone >= 0) { "quietZone não pode ser negativa, veio $quietZone" }
        require(forcedMask == null || forcedMask in 0 until Masking.MASK_COUNT) {
            "máscara precisa estar entre 0 e 7, veio $forcedMask"
        }

        val data = content.toByteArray(Charsets.UTF_8)
        val version = DataEncoder.chooseVersion(data.size, level)
        val dataCodewords = DataEncoder.encode(data, version, level)
        val allCodewords = CodewordInterleaver.addErrorCorrectionAndInterleave(dataCodewords, version, level)

        val unmasked = SymbolGrid(SymbolCapacity.symbolSize(version))
        FunctionPatterns.draw(unmasked, version, level)
        val codewordAt = DataPlacement.place(unmasked, allCodewords)

        val mask = forcedMask ?: chooseMask(unmasked, level)
        val symbol = withMask(unmasked, level, mask)
        return QrCode(
            version = version,
            errorCorrectionLevel = level,
            mask = mask,
            quietZone = quietZone,
            matrix = withQuietZone(symbol, quietZone),
            codewords = codewordMap(codewordAt, symbol.size, quietZone, version, level),
        )
    }

    /** O mapa de [DataPlacement] (coordenadas do símbolo) levado pras coordenadas da matriz com quiet zone. */
    private fun codewordMap(
        symbolCodewordAt: IntArray,
        symbolSize: Int,
        quietZone: Int,
        version: Int,
        level: ErrorCorrectionLevel,
    ): CodewordMap {
        val matrixSize = symbolSize + 2 * quietZone
        val codewordAt = IntArray(matrixSize * matrixSize) { DataPlacement.NO_CODEWORD }
        for (row in 0 until symbolSize) {
            for (column in 0 until symbolSize) {
                codewordAt[(row + quietZone) * matrixSize + column + quietZone] = symbolCodewordAt[row * symbolSize + column]
            }
        }
        return CodewordMap(
            matrixSize = matrixSize,
            codewordAtModule = codewordAt,
            blockOfCodeword = CodewordInterleaver.slots(version, level).map { it.block }.toIntArray(),
            correctableErrorsPerBlock = SymbolCapacity.correctableErrorsPerBlock(version, level),
        )
    }

    /** A de menor penalidade; no empate, a de menor número. */
    private fun chooseMask(unmasked: SymbolGrid, level: ErrorCorrectionLevel): Int =
        (0 until Masking.MASK_COUNT).minBy { mask -> Masking.penalty(withMask(unmasked, level, mask)) }

    /** Cópia com a máscara aplicada e o format info dela já desenhado (a penalidade conta ele também). */
    private fun withMask(unmasked: SymbolGrid, level: ErrorCorrectionLevel, mask: Int): SymbolGrid =
        unmasked.copy().also { grid ->
            Masking.apply(grid, mask)
            FormatInfo.drawFormatBits(grid, level, mask)
        }

    private fun withQuietZone(symbol: SymbolGrid, quietZone: Int) =
        QrMatrixBuilder(symbol.size + 2 * quietZone).apply {
            for (row in 0 until size) {
                for (column in 0 until size) {
                    val symbolRow = row - quietZone
                    val symbolColumn = column - quietZone
                    val insideSymbol = symbolRow in 0 until symbol.size && symbolColumn in 0 until symbol.size
                    if (insideSymbol) {
                        val type = checkNotNull(symbol.type(symbolRow, symbolColumn)) {
                            "módulo ($symbolRow, $symbolColumn) ficou sem tipo"
                        }
                        set(row, column, symbol.isDark(symbolRow, symbolColumn), type)
                    } else {
                        set(row, column, false, ModuleType.QUIET_ZONE)
                    }
                }
            }
        }.build()
}

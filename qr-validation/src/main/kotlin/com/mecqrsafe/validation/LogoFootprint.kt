package com.mecqrsafe.validation

import com.mecqrsafe.qrengine.QrCode
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Onde o logo cai na matriz, em unidades de módulo e nas coordenadas da
 * [com.mecqrsafe.qrengine.matrix.QrMatrix] (quiet zone incluída): o
 * módulo (linha, coluna) é o quadrado de `[coluna, coluna+1) x [linha,
 * linha+1)`.
 *
 * Um módulo conta como atingido se o logo encosta nele, nem que seja
 * numa beirada ([touches]). Parece exagero — o leitor amostra cada módulo
 * perto do centro —, mas "perto" é a palavra: a grade que ele estima a
 * partir dos finder patterns tem erro de fração de módulo, e no teste com
 * o ZXing módulos só com a beirada coberta foram lidos errado (e outros,
 * com o centro coberto por pouco, foram lidos certo). Na borda do logo o
 * resultado depende da câmera, então a validação assume o pior.
 *
 * É público porque o export desenha o logo exatamente nesta caixa
 * ([left], [top], [side]) — a geometria que a validação conferiu é a
 * mesma que vai pro arquivo, calculada num lugar só.
 */
class LogoFootprint(qr: QrCode, logo: LogoStyle) {

    private val matrixSize = qr.matrix.size

    /** Lado do logo, em módulos. */
    val side = logo.widthPercent / 100.0 * qr.symbolSize
    private val centerX = qr.quietZone + logo.centerXPercent / 100.0 * qr.symbolSize
    private val centerY = qr.quietZone + logo.centerYPercent / 100.0 * qr.symbolSize

    val left = centerX - side / 2
    val right = centerX + side / 2
    val top = centerY - side / 2
    val bottom = centerY + side / 2

    /** O logo sai do símbolo (e entra na quiet zone ou além da matriz)? */
    internal val leavesSymbol: Boolean = run {
        val symbolStart = qr.quietZone.toDouble()
        val symbolEnd = (qr.quietZone + qr.symbolSize).toDouble()
        left < symbolStart || top < symbolStart || right > symbolEnd || bottom > symbolEnd
    }

    internal fun touches(row: Int, column: Int): Boolean =
        column < right && column + 1 > left && row < bottom && row + 1 > top

    /** Os módulos da matriz que o logo toca, nem que seja na beirada. */
    internal fun touchedModules(): Sequence<Pair<Int, Int>> {
        val rows = indexRange(top, bottom)
        val columns = indexRange(left, right)
        return rows.asSequence()
            .flatMap { row -> columns.asSequence().map { column -> row to column } }
            .filter { (row, column) -> touches(row, column) }
    }

    private fun indexRange(from: Double, to: Double): IntRange =
        floor(from).toInt().coerceAtLeast(0)..(ceil(to).toInt() - 1).coerceAtMost(matrixSize - 1)
}

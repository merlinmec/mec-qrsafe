package com.mecqrsafe.render

import com.mecqrsafe.qrengine.matrix.ModuleType
import com.mecqrsafe.qrengine.matrix.QrMatrix
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Forma de cada módulo de dado (e dos padrões fixos que não são os olhos). */
enum class ModuleShape { SQUARE, ROUNDED, DOTS }

/** Forma dos três "olhos" — os finder patterns de 7x7 nos cantos. */
enum class EyeShape { SQUARE, ROUNDED, CIRCLE }

enum class GradientType { LINEAR, RADIAL }

/**
 * Degradê nos módulos escuros, de [RenderOptions.darkColor] até [endColor].
 * Linear vai na direção de [angleDegrees] (0° = da esquerda pra direita,
 * 90° = de cima pra baixo); radial sai do centro do símbolo. O contraste
 * de todo o caminho já foi conferido pela ContrastRule.
 */
data class Gradient(val type: GradientType, val endColor: String, val angleDegrees: Double = 0.0) {
    init {
        require(Regex("^#[0-9A-Fa-f]{6}$").matches(endColor)) { "endColor precisa ser #RRGGBB, veio $endColor" }
    }
}

/**
 * A geometria do desenho, em unidades de módulo e nas coordenadas da
 * matriz (quiet zone incluída), num lugar só — o PNG e o SVG traduzem
 * isto pra Java2D e pra SVG, e o renderer canvas do front repete os
 * MESMOS números (frontend/src/render/drawQr.ts), pra o preview ser o
 * arquivo exportado.
 *
 * Os tamanhos não são estéticos apenas: cada combinação de forma de
 * módulo e de olho é lida pelo leitor do ZXing nos testes
 * (`ShapesAgainstReaderTest`); uma forma que não lesse não seria oferecida.
 */
object QrGeometry {

    /** Diâmetro do ponto no estilo [ModuleShape.DOTS], em módulos. */
    const val DOT_DIAMETER = 0.9

    /** Raio do canto no estilo [ModuleShape.ROUNDED], em módulos. */
    const val MODULE_CORNER = 0.3

    /** Raios dos olhos arredondados: a moldura de fora, o furo dela e o miolo. */
    const val EYE_OUTER_CORNER = 2.0
    const val EYE_HOLE_CORNER = 1.2
    const val EYE_CENTER_CORNER = 0.9

    /** Canto superior esquerdo de cada olho (7x7). */
    data class Eye(val left: Double, val top: Double) {
        val size get() = 7.0
    }

    /** Largura da quiet zone, lida da própria matriz (o qr-render só recebe a matriz). */
    fun quietZoneOf(matrix: QrMatrix): Int {
        val middle = matrix.size / 2
        return (0 until matrix.size).first { matrix[middle, it].type != ModuleType.QUIET_ZONE }
    }

    /** Os três olhos: cima-esquerda, cima-direita e baixo-esquerda do símbolo. */
    fun eyes(matrix: QrMatrix, quietZone: Int = quietZoneOf(matrix)): List<Eye> {
        val far = (matrix.size - quietZone - 7).toDouble()
        val near = quietZone.toDouble()
        return listOf(Eye(near, near), Eye(far, near), Eye(near, far)).onEach { eye ->
            check(matrix[eye.top.toInt(), eye.left.toInt()].type == ModuleType.FINDER_PATTERN) {
                "esperava um finder pattern em (${eye.top}, ${eye.left})"
            }
        }
    }

    /** Linha do degradê linear: atravessa o símbolo inteiro na direção do ângulo. */
    data class Line(val x1: Double, val y1: Double, val x2: Double, val y2: Double)

    fun linearGradientLine(matrixSize: Int, quietZone: Int, angleDegrees: Double): Line {
        val symbol = (matrixSize - 2 * quietZone).toDouble()
        val center = matrixSize / 2.0
        val radians = Math.toRadians(angleDegrees)
        val dx = cos(radians)
        val dy = sin(radians)
        // Meio comprimento da projeção do quadrado do símbolo na direção (dx, dy):
        // o degradê começa e termina exatamente nos cantos do símbolo.
        val half = symbol / 2 * (abs(dx) + abs(dy))
        return Line(center - dx * half, center - dy * half, center + dx * half, center + dy * half)
    }

    /** Raio do degradê radial: do centro até o canto do símbolo. */
    fun radialGradientRadius(matrixSize: Int, quietZone: Int): Double =
        (matrixSize - 2 * quietZone) / 2.0 * sqrt(2.0)
}

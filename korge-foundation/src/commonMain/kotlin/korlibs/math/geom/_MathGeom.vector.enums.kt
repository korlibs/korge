package korlibs.math.geom.vector

import korlibs.datastructure.*

/**
 * Determines how the lines end or start
 */
enum class LineCap {
    /**
     * A butt cap, conserving the length of the path.
     *
     * ```
     *   ┌───────
     *   │┈┈┈┈┈┈┈
     *   └───────
     * ```
     */
    BUTT,

    /**
     * A square cap, expanding the length of the path.
     *
     * ```
     * ┌─────────
     * │  ┈┈┈┈┈┈┈
     * └─────────
     * ```
     */
    SQUARE,
    /**
     * A rounded circular cap, expanding the length of the path.
     *
     * ```
     * ╭─────────
     * │  ┈┈┈┈┈┈┈
     * ╰─────────
     * ```
     *
     * *Note:*
     * The roundness of the figure is limited by the characters used.
     */
    ROUND;

    companion object {
        operator fun get(str: String?): LineCap = when {
            str.isNullOrEmpty() -> BUTT
            else -> when (str[0].uppercaseChar()) {
                'B' -> BUTT
                'S' -> SQUARE
                'R' -> ROUND
                else -> BUTT
            }
        }
    }
}

data class StrokeInfo(
    val thickness: Float = 1f,
    val pixelHinting: Boolean = false,
    val scaleMode: LineScaleMode = LineScaleMode.NORMAL,
    val startCap: LineCap = LineCap.BUTT,
    val endCap: LineCap = LineCap.BUTT,
    val join: LineJoin = LineJoin.MITER,
    val miterLimit: Float = 20f,
    val dash: IFloatArrayList? = null,
    val dashOffset: Float = 0f
)

/**
 * Describes how two lines/curves converge
 */
enum class LineJoin {
    /**
     * Bevel join:
     *
     * ```
     * ╲  ╲╱  ╱
     *  ╲⎽⎽⎽⎽╱
     * ```
     */
    BEVEL,
    /**
     * Rounded join:
     *
     * ```
     * ╲  ╲╱  ╱
     *  ╲    ╱
     *    ⌣
     * ```
     */
    ROUND,

    /**
     * Pointed join:
     *
     * ```
     * ╲  ╲╱  ╱
     *  ╲    ╱
     *   ╲  ╱
     *    ╲╱
     * ```
     *
     * *Note:*
     * This join is usually limited by the [miterLimit],
     * a ratio that determines the maximum length of the pointed angle,
     * that can be really long for small angles
     */
    MITER;

    companion object {
        operator fun get(str: String?): LineJoin = when {
            str.isNullOrEmpty() -> MITER
            else -> when (str[0].uppercaseChar()) {
                'M' -> MITER
                'B' -> BEVEL
                'S' -> BEVEL //SQUARE
                'R' -> ROUND
                else -> MITER
            }
        }
    }
}

enum class LineScaleMode(val hScale: Boolean, val vScale: Boolean) {
    NONE(false, false),
    HORIZONTAL(true, false),
    VERTICAL(false, true),
    NORMAL(true, true);

    val anyScale: Boolean = hScale || vScale
    val allScale: Boolean = hScale && vScale
}

enum class Winding(val str: String) {
    /**
     * https://en.wikipedia.org/wiki/Even-odd_rule
     **/
    EVEN_ODD("evenOdd"),
    /**
     * **DEFAULT**
     *
     * https://en.wikipedia.org/wiki/Nonzero-rule
     **/
    NON_ZERO("nonZero");

    companion object {
        val DEFAULT: Winding get() = NON_ZERO
    }
}

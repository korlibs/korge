package korlibs.math.geom.vector

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
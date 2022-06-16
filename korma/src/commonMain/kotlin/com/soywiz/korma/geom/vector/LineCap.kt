package com.soywiz.korma.geom.vector

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

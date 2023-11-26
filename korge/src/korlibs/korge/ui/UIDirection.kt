package korlibs.korge.ui

/**
 * Analogous to flex-direction: <https://developer.mozilla.org/en-US/docs/Web/CSS/flex-direction>
 */
enum class UIDirection(
    val isHorizontal: Boolean = false,
    val isVertical: Boolean = false,
    val isReverse: Boolean = false,
) {
    ROW(isHorizontal = true),
    COLUMN(isVertical = true),
    ROW_REVERSE(isHorizontal = true, isReverse = true),
    COLUMN_REVERSE(isVertical = true, isReverse = true),
    ;

    companion object {
        @Deprecated("", ReplaceWith("ROW", "korlibs.korge.ui.UIDirection.ROW"))
        val HORIZONTAL get() = ROW
        @Deprecated("", ReplaceWith("COLUMN", "korlibs.korge.ui.UIDirection.COLUMN"))
        val VERTICAL get() = COLUMN
    }
}

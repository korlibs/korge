package korlibs.event

enum class MouseButton(val id: Int, val bits: Int = 1 shl id) {
    LEFT(0), MIDDLE(1), RIGHT(2), BUTTON3(3),
    BUTTON4(4), BUTTON5(5), BUTTON6(6), BUTTON7(7),
    BUTTON_WHEEL(8),
    BUTTON_UNKNOWN(10),
    NONE(11, bits = 0);

    val isLeft get() = this == LEFT
    val isMiddle get() = this == MIDDLE
    val isRight get() = this == RIGHT

    fun pressedFromFlags(flags: Int): Boolean = (flags and this.bits) != 0

    companion object {
        val MAX = NONE.ordinal + 1
        val BUTTONS = values()
        operator fun get(id: Int) = BUTTONS.getOrElse(id) { BUTTON_UNKNOWN }
    }
}
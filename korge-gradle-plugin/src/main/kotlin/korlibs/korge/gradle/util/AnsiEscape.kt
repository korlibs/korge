package korlibs.korge.gradle.util

// https://www.lihaoyi.com/post/BuildyourownCommandLinewithANSIescapecodes.html
interface AnsiEscape {
    companion object : AnsiEscape {
        inline operator fun <T> invoke(block: AnsiEscape.() -> T): T = block()
    }

    fun StringBuilder.appendAnsiScape(code: Int, extra: String? = null, char: Char = 'm'): StringBuilder {
        append('\u001B')
        append('[')
        append(code)
        if (extra != null) append(extra)
        append(char)
        return this
    }

    enum class Color(val index: Int) { BLACK(0), RED(1), GREEN(2), YELLOW(3), BLUE(4), PURPLE(5), CYAN(6), WHITE(7) }

    fun StringBuilder.appendReset() = appendAnsiScape(0)
    fun StringBuilder.appendBold() = appendAnsiScape(1)
    fun StringBuilder.appendUnderline() = appendAnsiScape(4)
    fun StringBuilder.appendColorReversed() = appendAnsiScape(7)
    fun StringBuilder.appendFgColor(color: Color, bright: Boolean = false) = appendAnsiScape(30 + color.index, extra = if (bright) ";1" else null)
    fun StringBuilder.appendBgColor(color: Color, bright: Boolean = false) = appendAnsiScape(40 + color.index, extra = if (bright) ";1" else null)
    fun StringBuilder.appendMoveUp(n: Int = 1) = appendAnsiScape(n, char = 'A')
    fun StringBuilder.appendMoveDown(n: Int = 1) = appendAnsiScape(n, char = 'B')
    fun StringBuilder.appendMoveRight(n: Int = 1) = appendAnsiScape(n, char = 'C')
    fun StringBuilder.appendMoveLeft(n: Int = 1) = appendAnsiScape(n, char = 'D')

    fun String.color(color: Color, bright: Boolean = false) = buildString { appendFgColor(color, bright).append(this@color).appendReset() }
    fun String.bgColor(color: Color, bright: Boolean = false) = buildString { appendBgColor(color, bright).append(this@bgColor).appendReset() }
    fun String.ansiEscape(code: Int) = buildString { appendAnsiScape(code).append(this@ansiEscape).appendReset() }

    val String.bold get() = ansiEscape(1)
    val String.underline get() = ansiEscape(4)
    val String.colorReversed get() = ansiEscape(7)

    val String.black get() = color(Color.BLACK)
    val String.red get() = color(Color.RED)
    val String.green get() = color(Color.GREEN)
    val String.yellow get() = color(Color.YELLOW)
    val String.blue get() = color(Color.BLUE)
    val String.purple get() = color(Color.PURPLE)
    val String.cyan get() = color(Color.CYAN)
    val String.white get() = color(Color.WHITE)

    val String.bgBlack get() = bgColor(Color.BLACK)
    val String.bgRed get() = bgColor(Color.RED)
    val String.bgGreen get() = bgColor(Color.GREEN)
    val String.bgYellow get() = bgColor(Color.YELLOW)
    val String.bgBlue get() = bgColor(Color.BLUE)
    val String.bgPurple get() = bgColor(Color.PURPLE)
    val String.bgCyan get() = bgColor(Color.CYAN)
    val String.bgWhite get() = bgColor(Color.WHITE)
}

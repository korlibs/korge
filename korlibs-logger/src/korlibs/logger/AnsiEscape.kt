package korlibs.logger

// https://www.lihaoyi.com/post/BuildyourownCommandLinewithANSIescapecodes.html
/**
 * Supports generating [AnsiEscape] escape codes. This allows to change
 * the color and attributes of the text.
 *
 * For example:
 *
 * ```
 * println(AnsiEscape { ("hello".red + " world".blue).bold })
 * ```
 */
interface AnsiEscape {
    companion object : AnsiEscape {
        /** Supports having access to the AnsiEscape build methods */
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

    /** Available Ansi Colors: [BLACK], [RED], [GREEN], [YELLOW], [BLUE], [PURPLE], [CYAN], [WHITE] */
    enum class Color(val index: Int) { BLACK(0), RED(1), GREEN(2), YELLOW(3), BLUE(4), PURPLE(5), CYAN(6), WHITE(7) }

    fun StringBuilder.appendReset() = appendAnsiScape(0)
    fun StringBuilder.appendBold() = appendAnsiScape(1)
    fun StringBuilder.appendUnderline() = appendAnsiScape(4)
    fun StringBuilder.appendColorReversed() = appendAnsiScape(7)
    fun StringBuilder.appendFgColor(color: Color, bright: Boolean = false) = appendAnsiScape(30 + color.index, extra = if (bright) ";1" else null)
    fun StringBuilder.appendBgColor(color: Color, bright: Boolean = false) = appendAnsiScape(40 + color.index, extra = if (bright) ";1" else null)
    fun StringBuilder.appendFgColor256(index: Int) = appendAnsiScape(38, extra = ";5;$index")
    fun StringBuilder.appendBgColor256(index: Int) = appendAnsiScape(48, extra = ";5;$index")
    fun StringBuilder.appendMoveUp(n: Int = 1) = appendAnsiScape(n, char = 'A')
    fun StringBuilder.appendMoveDown(n: Int = 1) = appendAnsiScape(n, char = 'B')
    fun StringBuilder.appendMoveRight(n: Int = 1) = appendAnsiScape(n, char = 'C')
    fun StringBuilder.appendMoveLeft(n: Int = 1) = appendAnsiScape(n, char = 'D')

    /** Makes this string to be in foreground [color] and [bright] */
    fun String.color(color: Color, bright: Boolean = false) = buildString { appendFgColor(color, bright).append(this@color).appendReset() }
    /** Makes this string to be in background [color] and [bright] */
    fun String.bgColor(color: Color, bright: Boolean = false) = buildString { appendBgColor(color, bright).append(this@bgColor).appendReset() }

    /** Makes this string to be in foreground color [index] */
    fun String.color256(index: Int) = buildString { appendFgColor256(index).append(this@color256).appendReset() }
    /** Makes this string to be in background color [index] */
    fun String.bgColor256(index: Int) = buildString { appendBgColor256(index).append(this@bgColor256).appendReset() }

    //fun String.color256(index: Int, bright: Boolean = false) = buildString { appendFgColor(index, bright).append(this@color256).appendReset() }
    //fun String.bgColor256(index: Int, bright: Boolean = false) = buildString { appendBgColor(index, bright).append(this@bgColor256).appendReset() }

    /** Sets a raw ansi escape sequence for this String */
    fun String.ansiEscape(code: Int, extra: String? = null, char: Char = 'm') = buildString { appendAnsiScape(code, extra, char).append(this@ansiEscape).appendReset() }

    /** Makes this String to be [bold] */
    val String.bold get() = ansiEscape(1)
    /** Makes this String to be [underline]d */
    val String.underline get() = ansiEscape(4)
    /** Makes this String to be [reversed]/highlighted (background/color swapped) */
    val String.colorReversed get() = ansiEscape(7)

    /** Shortcut for this.color(Color.BLACK) */
    val String.black get() = color(Color.BLACK)
    /** Shortcut for this.color(Color.RED) */
    val String.red get() = color(Color.RED)
    /** Shortcut for this.color(Color.GREEN) */
    val String.green get() = color(Color.GREEN)
    /** Shortcut for this.color(Color.YELLOW) */
    val String.yellow get() = color(Color.YELLOW)
    /** Shortcut for this.color(Color.BLUE) */
    val String.blue get() = color(Color.BLUE)
    /** Shortcut for this.color(Color.PURPLE) */
    val String.purple get() = color(Color.PURPLE)
    /** Shortcut for this.color(Color.CYAN) */
    val String.cyan get() = color(Color.CYAN)
    /** Shortcut for this.color(Color.WHITE) */
    val String.white get() = color(Color.WHITE)

    /** Shortcut for this.bgColor(Color.BLACK) */
    val String.bgBlack get() = bgColor(Color.BLACK)
    /** Shortcut for this.bgColor(Color.RED) */
    val String.bgRed get() = bgColor(Color.RED)
    /** Shortcut for this.bgColor(Color.GREEN) */
    val String.bgGreen get() = bgColor(Color.GREEN)
    /** Shortcut for this.bgColor(Color.YELLOW) */
    val String.bgYellow get() = bgColor(Color.YELLOW)
    /** Shortcut for this.bgColor(Color.BLUE) */
    val String.bgBlue get() = bgColor(Color.BLUE)
    /** Shortcut for this.bgColor(Color.PURPLE) */
    val String.bgPurple get() = bgColor(Color.PURPLE)
    /** Shortcut for this.bgColor(Color.CYAN) */
    val String.bgCyan get() = bgColor(Color.CYAN)
    /** Shortcut for this.bgColor(Color.WHITE) */
    val String.bgWhite get() = bgColor(Color.WHITE)
}

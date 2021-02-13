package com.soywiz.klogger

open class BaseConsole() : AnsiEscape {
    enum class Kind(val level: Int, val color: AnsiEscape.Color?) {
        ERROR(0, AnsiEscape.Color.RED),
        WARN(1, AnsiEscape.Color.YELLOW),
        INFO(2, AnsiEscape.Color.BLUE),
        DEBUG(3, AnsiEscape.Color.CYAN),
        TRACE(4, AnsiEscape.Color.GREEN),
        LOG(5, null),
    }

    open fun log(kind: Kind, vararg msg: Any?) {
        println(logToString(kind, *msg))
    }

    protected open fun logToString(kind: Kind, vararg msg: Any?): String = buildString {
        val color = kind.color
        if (color != null) appendFgColor(color)
        msg.joinTo(this, ", ")
        if (color != null) appendReset()
    }

    /** Registers a [log] in the console */
    fun log(vararg msg: Any?): Unit = log(Kind.LOG, *msg)

    /** Registers a [trace] in the console */
    fun trace(vararg msg: Any?): Unit = log(Kind.TRACE, *msg)

    /** Registers a [debug] in the console */
    fun debug(vararg msg: Any?): Unit = log(Kind.DEBUG, *msg)

    /** Registers a [info] in the console */
    fun info(vararg msg: Any?): Unit = log(Kind.INFO, *msg)

    /** Registers a [warn] in the console */
    fun warn(vararg msg: Any?): Unit = log(Kind.WARN, *msg)

    /** Registers an [error] in the console */
    fun error(vararg msg: Any?): Unit = log(Kind.ERROR, *msg)
}

expect object Console : BaseConsole

fun Console.assert(cond: Boolean, msg: String): Unit {
    if (cond) throw AssertionError(msg)
}

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

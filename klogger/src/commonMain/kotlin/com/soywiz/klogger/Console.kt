package com.soywiz.klogger

open class BaseConsole() {
    enum class Kind(val level: Int, val color: String) {
        ERROR(0, ConsoleColor.FG_RED),
        WARN(1, ConsoleColor.FG_YELLOW),
        INFO(2, ConsoleColor.FG_BLUE),
        DEBUG(3, ConsoleColor.FG_CYAN),
        TRACE(4, ConsoleColor.FG_GREEN),
        LOG(5, ConsoleColor.RESET),
    }

    open fun log(kind: Kind, vararg msg: Any?) {
        println(logToString(kind, *msg))
    }

    protected open fun logToString(kind: Kind, vararg msg: Any?) = "${kind.color}${msg.joinToString(", ")}${ConsoleColor.RESET}"

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

object ConsoleColor {
    const val RESET = "\u001B[0m"

    const val FG_BLACK = "\u001B[30m"
    const val FG_RED = "\u001B[31m"
    const val FG_GREEN = "\u001B[32m"
    const val FG_YELLOW = "\u001B[33m"
    const val FG_BLUE = "\u001B[34m"
    const val FG_PURPLE = "\u001B[35m"
    const val FG_CYAN = "\u001B[36m"
    const val FG_WHITE = "\u001B[37m"

    const val BG_BLACK = "\u001B[40m"
    const val BG_RED = "\u001B[41m"
    const val BG_GREEN = "\u001B[42m"
    const val BG_YELLOW = "\u001B[43m"
    const val BG_BLUE = "\u001B[44m"
    const val BG_PURPLE = "\u001B[45m"
    const val BG_CYAN = "\u001B[46m"
    const val BG_WHITE = "\u001B[47m"

    val String.black get() = "$FG_BLACK$this$RESET"
    val String.red get() = "$FG_RED$this$RESET"
    val String.green get() = "$FG_GREEN$this$RESET"
    val String.yellow get() = "$FG_YELLOW$this$RESET"
    val String.blue get() = "$FG_BLUE$this$RESET"
    val String.purple get() = "$FG_PURPLE$this$RESET"
    val String.cyan get() = "$FG_CYAN$this$RESET"
    val String.white get() = "$FG_WHITE$this$RESET"

    val String.bgBlack get() = "$BG_BLACK$this$RESET"
    val String.bgRed get() = "$BG_RED$this$RESET"
    val String.bgGreen get() = "$BG_GREEN$this$RESET"
    val String.bgYellow get() = "$BG_YELLOW$this$RESET"
    val String.bgBlue get() = "$BG_BLUE$this$RESET"
    val String.bgPurple get() = "$BG_PURPLE$this$RESET"
    val String.bgCyan get() = "$BG_CYAN$this$RESET"
    val String.bgWhite get() = "$BG_WHITE$this$RESET"

    inline operator fun <T> invoke(block: ConsoleColor.() -> T) = block(ConsoleColor)
}

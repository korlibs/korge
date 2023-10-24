package korlibs.logger

import kotlin.native.concurrent.ThreadLocal

@PublishedApi
internal var baseConsoleHook: ((
    kind: BaseConsole.Kind, msg: Array<out Any?>,
    logInternal: (kind: BaseConsole.Kind, msg: Array<out Any?>) -> Unit,
) -> Unit)? = null

open class BaseConsole() : AnsiEscape {
    enum class Kind(val level: Int, val color: AnsiEscape.Color?) {
        ERROR(0, AnsiEscape.Color.RED),
        WARN(1, AnsiEscape.Color.YELLOW),
        INFO(2, AnsiEscape.Color.BLUE),
        DEBUG(3, AnsiEscape.Color.CYAN),
        TRACE(4, AnsiEscape.Color.GREEN),
        LOG(5, null),
    }

    data class LogEntry(val kind: Kind, val msg: List<Any?>) {
        override fun toString(): String = msg.joinToString(", ")
    }

    inline fun capture(
        block: () -> Unit
    ): List<LogEntry> = arrayListOf<LogEntry>().also { out ->
        hook(hook = { kind, msg, _ ->
            out += LogEntry(kind, msg.toList())
        }) {
            block()
        }
    }

    inline fun <T> hook(
        noinline hook: (
            kind: BaseConsole.Kind, msg: Array<out Any?>,
            logInternal: (kind: BaseConsole.Kind, msg: Array<out Any?>) -> Unit,
        ) -> Unit,
        block: () -> T
    ): T {
        val old = baseConsoleHook
        try {
            baseConsoleHook = hook
            return block()
        } finally {
            baseConsoleHook = old
        }
    }

    fun log(kind: Kind, vararg msg: Any?) {
        val hook = baseConsoleHook
        if (hook != null) {
            hook(kind, msg, ::logInternal)
        } else {
            logInternal(kind, *msg)
        }
    }

    protected open fun logInternal(kind: Kind, vararg msg: Any?) {
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

fun Console.assert(cond: Boolean, msg: String) {
    if (cond) throw AssertionError(msg)
}


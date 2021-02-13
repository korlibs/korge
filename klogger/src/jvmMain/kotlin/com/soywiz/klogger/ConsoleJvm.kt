package com.soywiz.klogger

actual object Console : BaseConsole() {
    override fun log(kind: Kind, vararg msg: Any?) {
        val stream = if (kind == Kind.ERROR) System.err else System.out
        stream.println(logToString(kind, *msg))
    }

    override fun logToString(kind: Kind, vararg msg: Any?): String = buildString {
        val color = kind.color
        if (color != null) appendFgColor(color)
        append('#')
        append(Thread.currentThread().id)
        append(": ")
        msg.joinTo(this, ", ")
        if (color != null) appendReset()
    }
}

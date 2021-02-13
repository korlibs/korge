package com.soywiz.klogger

actual object Console : BaseConsole() {
    override fun log(kind: Kind, vararg msg: Any?) {
        val stream = if (kind == Kind.ERROR) System.err else System.out
        stream.println(logToString(kind, *msg))
    }

    override fun logToString(kind: Kind, vararg msg: Any?): String =
        "${kind.color}#${Thread.currentThread().id}: ${msg.joinToString(", ")}${ConsoleColor.RESET}"
}

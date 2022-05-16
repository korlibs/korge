package com.soywiz.klogger

import platform.Foundation.NSLog

actual object Console : BaseConsole() {
    override fun logInternal(kind: Kind, vararg msg: Any?) {
        NSLog("%s", logToString(kind, *msg))
    }

    override fun logToString(kind: Kind, vararg msg: Any?): String {
        return msg.joinToString(", ")
    }
}

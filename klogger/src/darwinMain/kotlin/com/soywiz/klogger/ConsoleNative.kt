package com.soywiz.klogger

import platform.Foundation.NSLog

actual object Console : BaseConsole() {
    override fun log(kind: Kind, vararg msg: Any?) {
        NSLog("%s", logToString(kind, *msg))
    }
}

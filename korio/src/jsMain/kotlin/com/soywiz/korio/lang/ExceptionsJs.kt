package com.soywiz.korio.lang

actual open class IOException actual constructor(msg: String) : Exception(msg)
actual open class EOFException actual constructor(msg: String) : IOException(msg)
actual open class FileNotFoundException actual constructor(msg: String) : IOException(msg)

actual fun Throwable.printStackTrace() {
    val e = this
    try {
        console.error(e.asDynamic())
        console.error(e.asDynamic().stack)
    } catch (e: dynamic) {
        console.error("Error logging into console")
        try {
            console.error(e)
        } catch (e: dynamic) {
        }
    }
}

actual fun enterDebugger() {
    js("debugger;")
}

package com.soywiz.korio.lang

actual open class IOException actual constructor(msg: String) : Exception(msg)
actual open class EOFException actual constructor(msg: String) : IOException(msg)
actual open class FileNotFoundException actual constructor(msg: String) : IOException(msg)

actual fun Throwable.printStackTrace() {
	this.printStackTrace()
}

actual fun enterDebugger(): Unit {
	println("enterDebugger")
}

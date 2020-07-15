package com.soywiz.korio.lang

actual typealias IOException = java.io.IOException
actual typealias EOFException = java.io.EOFException
actual typealias FileNotFoundException = java.io.FileNotFoundException

actual fun Throwable.printStackTrace() {
	this.printStackTrace()
}

actual fun enterDebugger(): Unit {
	println("enterDebugger")
}

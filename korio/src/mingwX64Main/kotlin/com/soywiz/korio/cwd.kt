package com.soywiz.korio

import kotlinx.cinterop.*
import platform.posix.*

fun getExecutablePath(): String = kotlinx.cinterop.memScoped {
	val maxSize = 4096
	val data = allocArray<kotlinx.cinterop.ByteVar>(maxSize + 1)
	platform.windows.GetModuleFileNameA(null, data.reinterpret(), maxSize.convert())
	data.toKString()
}.replace('/', '\\')

fun getExecutableDirectory(): String = getExecutablePath().substringBeforeLast('\\')

actual fun nativeCwd(): String = getExecutableDirectory()

actual fun doMkdir(path: String, attr: Int): Int = platform.posix.mkdir(path)
fun realpath(path: String): String = path

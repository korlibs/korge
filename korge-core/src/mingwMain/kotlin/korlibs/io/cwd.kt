package korlibs.io

import kotlinx.cinterop.*
import platform.posix.*

fun getExecutablePath(): String = kotlinx.cinterop.memScoped {
	val maxSize = 4096
	val data = allocArray<kotlinx.cinterop.UShortVar>(maxSize + 1)
	platform.windows.GetModuleFileNameW(null, data.reinterpret(), maxSize.convert())
	data.toKString()
}.replace('\\', '/')

fun getExecutableDirectory(): String = getExecutablePath().substringBeforeLast('/')

actual fun nativeCwd(): String = getExecutableDirectory()

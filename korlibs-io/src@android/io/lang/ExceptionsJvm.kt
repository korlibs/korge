package korlibs.io.lang

actual typealias IOException = java.io.IOException
actual typealias EOFException = java.io.EOFException
actual typealias FileNotFoundException = java.io.FileNotFoundException

actual fun enterDebugger() {
	println("enterDebugger")
}

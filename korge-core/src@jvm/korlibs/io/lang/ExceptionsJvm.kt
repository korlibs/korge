package korlibs.io.lang

actual typealias IOException = java.io.IOException
actual typealias EOFException = java.io.EOFException
actual typealias FileNotFoundException = java.io.FileNotFoundException

// We can subscribe to this exception to enter debugger when thrown even if catched
class EnterDebuggerException : Throwable("enterDebugger")

actual fun enterDebugger() {
	//println("enterDebugger")
    throw EnterDebuggerException()
}

package korlibs.io.lang

// We can subscribe to this exception to enter debugger when thrown even if catched
class EnterDebuggerException : Throwable("enterDebugger")

actual fun enterDebugger() {
	//println("enterDebugger")
    throw EnterDebuggerException()
}

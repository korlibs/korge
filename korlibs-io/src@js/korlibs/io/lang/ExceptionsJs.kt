package korlibs.io.lang

/*
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
*/

actual fun enterDebugger() {
    js("debugger;")
}

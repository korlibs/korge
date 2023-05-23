package korlibs.logger

@JsName("Console")
internal open external class JsConsole {
    fun error(vararg msg: Any?)
    fun warn(vararg msg: Any?)
    fun info(vararg msg: Any?)
    fun log(vararg msg: Any?)
}

internal external val console: JsConsole /* compiled code */

actual object Console : BaseConsole() {
    override fun logInternal(kind: Kind, vararg msg: Any?) {
        when (kind) {
            Kind.ERROR -> console.error(*msg)
            Kind.WARN -> console.warn(*msg)
            Kind.INFO -> console.info(*msg)
            Kind.DEBUG -> console.log(*msg)
            Kind.TRACE -> console.log(*msg)
            Kind.LOG -> console.log(*msg)
        }
    }
}

package korlibs.logger

@JsName("Console")
internal open external class JsConsole {
    fun error(vararg msg: String)
    fun warn(vararg msg: String)
    fun info(vararg msg: String)
    fun log(vararg msg: String)
}

internal external val console: JsConsole /* compiled code */

actual object Console : BaseConsole() {
    override fun logInternal(kind: Kind, vararg msg: Any?) {
        when (kind) {
            Kind.ERROR -> console.error(*msg.map { it.toString() }.toTypedArray())
            Kind.WARN -> console.warn(*msg.map { it.toString() }.toTypedArray())
            Kind.INFO -> console.info(*msg.map { it.toString() }.toTypedArray())
            Kind.DEBUG -> console.log(*msg.map { it.toString() }.toTypedArray())
            Kind.TRACE -> console.log(*msg.map { it.toString() }.toTypedArray())
            Kind.LOG -> console.log(*msg.map { it.toString() }.toTypedArray())
        }
    }
}

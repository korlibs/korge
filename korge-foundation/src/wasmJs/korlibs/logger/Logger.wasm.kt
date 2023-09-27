package korlibs.logger

import kotlinx.browser.*


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

actual object DefaultLogOutput : Logger.Output {
    override fun output(logger: Logger, level: Logger.Level, msg: Any?) = Logger.ConsoleLogOutput.output(logger, level, msg)
}

internal actual val miniEnvironmentVariables: Map<String, String> by lazy {
    when {
        //jsTypeOf(process) != "undefined" -> jsObjectToMap(process.env)
        //jsTypeOf(Deno) != "undefined" -> jsObjectToMap(Deno.env)
        else -> QueryString_decode((document.location?.search ?: "").trimStart('?')).map { it.key to (it.value.firstOrNull() ?: it.key) }.toMap()
    }
}

private fun QueryString_decode(str: CharSequence): Map<String, List<String>> {
    val out = linkedMapOf<String, ArrayList<String>>()
    str.split('&').forEach { chunk ->
        val parts = chunk.split('=', limit = 2)
        val key = parts[0]
        val value = parts.getOrElse(1) { key }
        val list = out.getOrPut(key) { arrayListOf() }
        list += value
    }
    return out
}

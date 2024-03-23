package korlibs.logger

import kotlinx.browser.document

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

actual object DefaultLogOutput : Logger.Output {
    actual override fun output(logger: Logger, level: Logger.Level, msg: Any?) = Logger.ConsoleLogOutput.output(logger, level, msg)
}

private external val process: dynamic
private external val Deno: dynamic

private val isDenoJs: Boolean by lazy { js("(typeof Deno === 'object' && Deno.statSync)").unsafeCast<Boolean>() }
private val isNodeJs: Boolean by lazy { js("((typeof process !== 'undefined') && process.release && (process.release.name.search(/node|io.js/) !== -1))").unsafeCast<Boolean>() }

internal actual val miniEnvironmentVariables: Map<String, String> by lazy {

    when {
        isNodeJs -> jsObjectToMap(process.env)
        isDenoJs -> jsObjectToMap(Deno.env)
        js("(typeof document !== 'undefined')") -> QueryString_decode((document.location?.search ?: "").trimStart('?')).map { it.key to (it.value.firstOrNull() ?: it.key) }.toMap()
        else -> mapOf()
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

private fun jsToArray(obj: dynamic): Array<Any?> = Array<Any?>(obj.length) { obj[it] }
private fun jsObjectKeys(obj: dynamic): dynamic = js("(Object.keys(obj))")
private fun jsObjectKeysArray(obj: dynamic): Array<String> = jsToArray(jsObjectKeys(obj)) as Array<String>
private fun jsObjectToMap(obj: dynamic): Map<String, dynamic> = jsObjectKeysArray(obj).associate { it to obj[it] }

package korlibs.logger.internal

import kotlinx.browser.*

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

package korlibs.io.lang

import korlibs.io.*

actual object JSEval {
    actual const val available: Boolean = true
    actual val globalThis: Any? get() = jsGlobal

    actual operator fun invoke(
        // language: javascript
        code: String,
        params: Map<String, Any?>,
    ): Any? {
        val keys = params.keys.toList()
        val func = eval("(function(${keys.joinToString()}) { $code })")
        return func.apply(jsGlobal, keys.map { params[it] }.toTypedArray())
    }
}

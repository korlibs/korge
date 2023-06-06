package korlibs.io.lang

import korlibs.io.*
import korlibs.io.wasm.*

@JsName("Function")
external class JsFunction {
    fun apply(obj: JsAny, args: JsArray<JsAny?>): JsAny?
}

@JsFun("(code) => { return eval(code); }")
private external fun eval(str: String): JsFunction?

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
        val global: JsAny = jsGlobal
        val args: JsArray<JsAny?> = jsArrayOf(*keys.map { params[it] as JsAny }.toTypedArray()).unsafeCast()
        return func?.apply(global, args)
    }
}

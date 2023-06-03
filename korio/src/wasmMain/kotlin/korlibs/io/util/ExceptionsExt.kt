package korlibs.io.util

import korlibs.io.wasm.*

@JsName("Error")
external class JsError : JsAny {
    val message: String?
}

external interface JsResult<T : JsAny> : JsAny {
    val result: T?
    val error: JsError?
}

val JsUnit: JsAny = jsEmptyObj()

@JsFun("(block) => { try { return { result: block(), error: null }; } catch (e) { return { result: null, error: e }; } }")
private external fun <T : JsAny> runCatchingJsExceptions(block: () -> T): JsResult<T>

fun <T : JsAny> wrapWasmJsExceptions(block: () -> T): T {
    val result = runCatchingJsExceptions { block() }
    if (result.error != null) throw Exception(result.error!!.message)
    return result.result!!
}

fun wrapWasmJsExceptionsUnit(block: () -> Unit) {
    val result = runCatchingJsExceptions {
        block()
        JsUnit
    }
    if (result.error != null) throw Exception(result.error!!.message)
}

package korlibs.render.internal

import korlibs.graphics.gl.*
import korlibs.graphics.gl.jsObject
import korlibs.wasm.*


@JsName("Error")
internal external class JsError : JsAny {
    val message: String?
}

internal external interface JsResult<T : JsAny> : JsAny {
    val result: T?
    val error: JsError?
}

@JsFun("(block) => { try { return { result: block(), error: null }; } catch (e) { return { result: null, error: e }; } }")
internal external fun <T : JsAny> runCatchingJsExceptions(block: () -> T): JsResult<T>

internal fun <T : JsAny> wrapWasmJsExceptions(block: () -> T): T {
    val result = runCatchingJsExceptions { block() }
    if (result.error != null) throw Exception(result.error!!.message)
    return result.result!!
}

internal fun wrapWasmJsExceptionsUnit(block: () -> Unit) {
    val result = runCatchingJsExceptions {
        block()
        jsEmptyObj()
    }
    if (result.error != null) throw Exception(result.error!!.message)
}

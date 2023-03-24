package korlibs.io

import korlibs.io.runtime.browser.JsRuntimeBrowser
import korlibs.io.runtime.deno.JsRuntimeDeno
import korlibs.io.runtime.node.JsRuntimeNode
import org.w3c.dom.*
import org.w3c.dom.events.*
import org.w3c.performance.*
import kotlinx.browser.*
import kotlin.collections.set

abstract external class GlobalScope : EventTarget, WindowOrWorkerGlobalScope, GlobalPerformance {
	fun postMessage(message: dynamic, targetOrigin: dynamic = definedExternally, transfer: dynamic = definedExternally)
	fun requestAnimationFrame(callback: (Double) -> Unit): Int
	fun cancelAnimationFrame(handle: Int): Unit
}

val jsGlobalDynamic: dynamic = js("((typeof globalThis !== 'undefined') ? globalThis : ((typeof global !== 'undefined') ? global : self))")
val jsGlobal: GlobalScope = jsGlobalDynamic

val isDenoJs by lazy { js("(typeof Deno === 'object' && Deno.statSync)").unsafeCast<Boolean>() }
val isWeb by lazy { js("(typeof window === 'object')").unsafeCast<Boolean>() }
val isWorker by lazy { js("(typeof importScripts === 'function')").unsafeCast<Boolean>() }
val isNodeJs by lazy { js("((typeof process !== 'undefined') && process.release && (process.release.name.search(/node|io.js/) !== -1))").unsafeCast<Boolean>() }
val isShell get() = !isWeb && !isNodeJs && !isWorker

val jsRuntime by lazy {
    when {
        isDenoJs -> JsRuntimeDeno
        isNodeJs -> JsRuntimeNode
        else -> JsRuntimeBrowser
    }
}

fun HTMLCollection.toList(): List<Element?> = (0 until length).map { this[it] }
fun <T : Element> HTMLCollection.toTypedList(): List<T> = (0 until length).map { this[it].unsafeCast<T>() }

private external class Date(time: Double)

fun jsNew(clazz: dynamic): dynamic = js("(new (clazz))()")
fun jsNew(clazz: dynamic, a0: dynamic): dynamic = js("(new (clazz))(a0)")
fun jsNew(clazz: dynamic, a0: dynamic, a1: dynamic): dynamic = js("(new (clazz))(a0, a1)")
fun jsNew(clazz: dynamic, a0: dynamic, a1: dynamic, a2: dynamic): dynamic = js("(new (clazz))(a0, a1, a2)")
fun jsEnsureNumber(v: dynamic): Number = js("(+v)")
fun jsEnsureInt(v: dynamic): Int = js("(v|0)")
fun jsEnsureString(v: dynamic): String = js("(String(v))")
fun jsEmptyObj(): dynamic = js("({})")
fun jsEmptyArray(): dynamic = js("([])")
fun jsObjectKeys(obj: dynamic): dynamic = js("(Object.keys(obj))")
fun jsObjectKeysArray(obj: dynamic): Array<String> = jsToArray(jsObjectKeys(obj)) as Array<String>
fun jsObjectToMap(obj: dynamic): Map<String, dynamic> = jsObjectKeysArray(obj).associate { it to obj[it] }
fun jsToArray(obj: dynamic): Array<Any?> = Array<Any?>(obj.length) { obj[it] }
fun jsArray(vararg elements: dynamic): Array<dynamic> {
	val out = jsEmptyArray()
	for (e in elements) out.push(e)
	return out
}

inline fun <reified T> jsToArrayT(obj: dynamic): Array<T> = Array<T>(obj.length) { obj[it] }
fun jsObject(vararg pairs: Pair<String, Any?>): dynamic {
	val out = jsEmptyObj()
	for (pair in pairs) out[pair.first] = pair.second
	return out
}

fun Map<String, Any?>.toJsObject() = jsObject(*this.entries.map { it.key to it.value }.toTypedArray())

fun jsToObjectMap(obj: dynamic): Map<String, Any?>? {
	if (obj == null) return null
	val out = linkedMapOf<String, Any?>()
	val keys = jsObjectKeys(obj)
	for (n in 0 until keys.length) {
		val key = keys[n]
		out["$key"] = obj[key]
	}
	return out
}

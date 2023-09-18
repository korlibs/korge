package korlibs.io

import korlibs.io.runtime.browser.*
import korlibs.io.wasm.*
import korlibs.platform.*
import org.w3c.dom.*
import org.w3c.dom.events.*
import org.w3c.performance.*
import kotlin.collections.set

abstract external class GlobalScope : EventTarget, WindowOrWorkerGlobalScope, GlobalPerformance, JsAny {
	fun postMessage(message: JsAny, targetOrigin: JsAny = definedExternally, transfer: JsAny = definedExternally)
	fun requestAnimationFrame(callback: (Double) -> Unit): Int
	fun cancelAnimationFrame(handle: Int)
}

@JsFun("() => { return ((typeof globalThis !== 'undefined') ? globalThis : ((typeof global !== 'undefined') ? global : self)); }")
external fun getJsGlobalDynamic(): GlobalScope

//val jsGlobalDynamic: dynamic = getJsGlobalDynamic()
val jsGlobal: GlobalScope = getJsGlobalDynamic()

val isDenoJs get() = Platform.isJsDenoJs
val isWeb get() = Platform.isJsBrowser
val isWorker get() = Platform.isJsWorker
val isNodeJs get() = Platform.isJsNodeJs
val isShell get() = !isWeb && !isNodeJs && !isWorker

val jsRuntime by lazy {
    when {
        //isDenoJs -> JsRuntimeDeno
        //isNodeJs -> JsRuntimeNode
        else -> JsRuntimeBrowser
    }
}

fun HTMLCollection.toList(): List<Element?> = (0 until length).map { this[it] }
fun <T : Element> HTMLCollection.toTypedList(): List<T> = (0 until length).map { this[it]!!.unsafeCast<T>() }

private external class Date(time: Double)

//fun jsNew(clazz: dynamic): dynamic = js("(new (clazz))()")
//fun jsNew(clazz: dynamic, a0: dynamic): dynamic = js("(new (clazz))(a0)")
//fun jsNew(clazz: dynamic, a0: dynamic, a1: dynamic): dynamic = js("(new (clazz))(a0, a1)")
//fun jsNew(clazz: dynamic, a0: dynamic, a1: dynamic, a2: dynamic): dynamic = js("(new (clazz))(a0, a1, a2)")
//fun jsEnsureNumber(v: dynamic): Number = js("(+v)")
//fun jsEnsureInt(v: dynamic): Int = js("(v|0)")
//fun jsEnsureString(v: dynamic): String = js("(String(v))")
fun jsObjectKeysArray(obj: JsAny?): Array<String> = (jsToArray(jsObjectKeys(obj)) as JsArray<JsString>).toList().map { it.toString() }.toTypedArray()
fun jsObjectToMap(obj: JsAny?): Map<String, JsAny?> = jsObjectKeysArray(obj).associate { it to obj!!.getAny(it.toJsString()) }
fun jsToArray(obj: JsAny?): Array<Any?> = Array<Any?>(obj!!.unsafeCast<JsArray<*>>().length) { obj.getAny(it) }
fun jsArray(vararg elements: JsAny?): Array<JsAny?> {
	val out = jsEmptyArray<JsAny?>()
	for (e in elements) out.push(e)
	return out.toList().toTypedArray()
}

//inline fun <reified T> jsToArrayT(obj: JsAny): Array<T> = Array<T>(obj.getAny("length")) { obj[it] }
fun jsObject(vararg pairs: Pair<String, Any?>): JsAny {
	val out = jsEmptyObj()
	for (pair in pairs) out.setAny(pair.first.toJsString(), pair.second as? JsAny?)
	return out
}

fun Map<String, Any?>.toJsObject() = jsObject(*this.entries.map { it.key to it.value }.toTypedArray())

fun jsToObjectMap(obj: JsAny?): Map<String, Any?>? {
	if (obj == null) return null
	val out = linkedMapOf<String, Any?>()
	val keys = jsObjectKeys(obj)
	for (n in 0 until keys.length) {
		val key = keys[n]
		out["$key"] = obj.getAny(key)
	}
	return out
}

package korlibs.korge.service.storage

import korlibs.korge.view.*
import korlibs.memory.*
import kotlinx.browser.*

@JsName("Object")
private external object JsObject {
    fun keys(value: JsAny): JsArray<JsString>
}

actual class NativeStorage actual constructor(views: Views) : IStorageWithKeys by WasmNativeStorage(views)

fun WasmNativeStorage(views: Views): IStorageWithKeys {
    return BrowserNativeStorage(views)
}

open class BrowserNativeStorage(val views: Views) : IStorageWithKeys {
    override fun toString(): String = "NativeStorage(${toMap()})"

    companion object {
        val PREFIX = "org.korge.storage."
        fun getKey(key: String): String = "$PREFIX$key"
    }

    override fun keys(): List<String> {
        val keys = JsObject.keys(localStorage)
        return (0 until keys.length)
            .map { keys[it].toString() }
            .filter { it.startsWith(PREFIX) }
            .map { it.removePrefix(PREFIX) }
    }

    override fun set(key: String, value: String) {
        localStorage.setItem(getKey(key), value)
    }

    override fun getOrNull(key: String): String? = localStorage.getItem(getKey(key))

    override fun remove(key: String) {
        localStorage.removeItem(getKey(key))
    }

    //actual override fun removeAll() { localStorage.clear() }
}


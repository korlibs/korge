package korlibs.korge.service.storage

import korlibs.korge.view.*
import kotlinx.browser.*

@JsName("Object")
private external object JsObject {
    fun keys(): JsArray<JsString>
}

actual class NativeStorage actual constructor(val views: Views) : IStorageWithKeys {
    override fun toString(): String = "NativeStorage(${toMap()})"

    actual override fun keys(): List<String> {
        val keys = JsObject.keys()
        return (0 until keys.length).map { keys[it].toString() }
    }

	actual override fun set(key: String, value: String) {
		localStorage.setItem(key, value)
	}

	actual override fun getOrNull(key: String): String? {
		return localStorage.getItem(key)
	}

	actual override fun remove(key: String) {
		localStorage.removeItem(key)
	}

	actual override fun removeAll() {
		localStorage.clear()
	}
}

package korlibs.korge.service.storage

import korlibs.io.*
import korlibs.io.lang.*
import korlibs.io.runtime.node.*
import korlibs.korge.view.*
import korlibs.memory.*
import korlibs.platform.*
import kotlinx.browser.*

@JsName("Object")
private external object JsObject {
    fun keys(obj: dynamic): dynamic
}

private val REQ get() = "req"
private external val eval: dynamic
private fun require_node(name: String): dynamic = korlibs.korge.service.storage.eval("(${REQ}uire('$name'))")

actual class NativeStorage actual constructor(views: Views) : IStorageWithKeys by JSNativeStorage(views)

fun JSNativeStorage(views: Views): IStorageWithKeys {
    return when {
        Platform.isJsNodeJs -> NodeJSNativeStorage(views)
        else -> BrowserNativeStorage(views)
    }
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


open class NodeJSNativeStorage(views: Views) : FiledBasedNativeStorage(views) {
    val fs by lazy { require_node("fs") }

    override fun mkdirs(folder: String) = fs.mkdirSync(folder, jsObject("recursive" to true))
    override fun saveStr(data: String) = fs.writeFileSync(gameStorageFile, data.toByteArray(UTF8))
    override fun loadStr(): String = fs.readFileSync(gameStorageFile).unsafeCast<ByteArray>().toString(UTF8)
}

package korlibs.korge.service.storage

import korlibs.datastructure.*
import korlibs.datastructure.lock.*
import korlibs.io.serialization.json.*
import korlibs.korge.view.*
import kotlin.collections.*
import kotlin.native.concurrent.*

/** Cross-platform way of synchronously storing small data */
//expect fun NativeStorage(views: Views): IStorageWithKeys

expect class NativeStorage(views: Views) : IStorageWithKeys {
    override fun keys(): List<String>
	override fun set(key: String, value: String)
	override fun getOrNull(key: String): String?
	override fun remove(key: String)
	override fun removeAll()
}

@ThreadLocal
val Views.storage: NativeStorage by Extra.PropertyThis<Views, NativeStorage> { NativeStorage(this) }

val ViewsContainer.storage: NativeStorage get() = this.views.storage



////////////

abstract class FiledBasedNativeStorage(val views: Views) : IStorageWithKeys {
    val gameStorageFolder by lazy { views.realSettingsFolder.also { mkdirs(it) } }
    val gameStorageFile get() = "${gameStorageFolder}/game.storage"
    protected abstract fun mkdirs(folder: String)
    protected abstract fun saveStr(data: String)
    protected abstract fun loadStr(): String

    private val lock = Lock()
    private var map: MutableMap<String, String>? = null

    override fun toString(): String = "NativeStorage(${toMap()})"
    override fun keys(): List<String> = lock {
        ensureMap()
        return map!!.keys.toList()
    }

    private fun ensureMap(): MutableMap<String, String> {
        if (map == null) {
            map = LinkedHashMap()
            val str = kotlin.runCatching { loadStr() }.getOrNull()
            if (!str.isNullOrEmpty()) {
                try {
                    map!!.putAll(str.fromJson() as Map<String, String>)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
        return map!!
    }

    private fun save() = lock {
        saveStr(ensureMap().toJson())
    }

    override fun set(key: String, value: String) = lock {
        ensureMap()[key] = value
        save()
    }

    override fun getOrNull(key: String): String? = lock {
        return ensureMap()[key]
    }

    override fun remove(key: String) = lock {
        ensureMap().remove(key)
        save()
    }

    override fun removeAll() = lock {
        ensureMap().clear()
        save()
    }
}

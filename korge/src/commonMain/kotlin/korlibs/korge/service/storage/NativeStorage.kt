package korlibs.korge.service.storage

import korlibs.datastructure.*
import korlibs.datastructure.atomic.*
import korlibs.io.lang.*
import korlibs.io.serialization.json.*
import korlibs.korge.view.Views
import korlibs.korge.view.ViewsContainer
import kotlin.native.concurrent.ThreadLocal

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

    private var map = KdsAtomicRef<CopyOnWriteFrozenMap<String, String>?>(null)

    override fun toString(): String = "NativeStorage(${toMap()})"
    override fun keys(): List<String> {
        ensureMap()
        return map.value?.keys?.toList() ?: emptyList()
    }

    private fun ensureMap(): CopyOnWriteFrozenMap<String, String> {
        if (map.value == null) {
            map.value = CopyOnWriteFrozenMap()
            val str = kotlin.runCatching { loadStr() }.getOrNull()
            if (str != null && str.isNotEmpty()) {
                try {
                    map.value!!.putAll(str.fromJson() as Map<String, String>)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
        return map.value!!
    }

    private fun save() {
        saveStr(ensureMap().toJson())
    }

    override fun set(key: String, value: String) {
        ensureMap()[key] = value
        save()
    }

    override fun getOrNull(key: String): String? {
        return ensureMap()[key]
    }

    override fun remove(key: String) {
        ensureMap().remove(key)
        save()
    }

    override fun removeAll() {
        ensureMap().clear()
        save()
    }
}

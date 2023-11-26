package korlibs.korge.service.storage

import korlibs.korge.view.*
import platform.Foundation.*

actual class NativeStorage actual constructor(views: Views) : IStorageWithKeys by DarwinNativeStorage
//actual class NativeStorage actual constructor(views: Views) : IStorageWithKeys, DefaultNativeStorage(views)

object DarwinNativeStorage : IStorageWithKeys {
    //val storage = NSUserDefaults.standardUserDefaults
    val storage = NSUserDefaults(suiteName = "korge")

    val PREFIX = "org.korge.storage."

    override fun keys(): List<String> = storage.dictionaryRepresentation().keys
        .map { it.toString() }
        .filter { it.startsWith(PREFIX) }
        .map { it.removePrefix(PREFIX) }

    fun getKey(key: String): String = "$PREFIX$key"

    override fun set(key: String, value: String) {
        storage.setObject(value, getKey(key))
        storage.synchronize()
    }

    override fun getOrNull(key: String): String? {
        return storage.objectForKey(getKey(key))?.toString()
    }

    override fun remove(key: String) {
        storage.removeObjectForKey(getKey(key))
        storage.synchronize()
    }

    override fun removeAll() {
        for (key in keys()) {
            storage.removeObjectForKey(getKey(key))
        }
        storage.synchronize()
    }
}

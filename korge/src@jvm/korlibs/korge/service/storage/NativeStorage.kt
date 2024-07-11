package korlibs.korge.service.storage

import korlibs.korge.view.*
import java.io.*
import java.util.*

actual class NativeStorage actual constructor(val views: Views) : IStorageWithKeys {
	val props = Properties()
    val folder = File(views.realSettingsFolder).also { kotlin.runCatching { it.mkdirs() } }
    val file = File(folder, "game.jvm.storage")

	init {
		load()
	}

    override fun toString(): String = "NativeStorage(${toMap()})"

    actual override fun keys(): List<String> = props.keys.toList().map { it.toString() }

	private fun load() {
        if (!file.exists()) return

        try {
            FileInputStream(file).use { fis -> props.load(fis) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

	private fun save() {
		try {
			FileOutputStream(file).use { fout -> props.store(fout, "") }
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}

	actual override fun set(key: String, value: String) {
		props[key] = value
		save()
	}

	actual override fun getOrNull(key: String): String? {
		return props[key]?.toString()
	}

	actual override fun remove(key: String) {
		props.remove(key)
		save()
	}

	actual override fun removeAll() {
		props.clear()
		save()
	}
}

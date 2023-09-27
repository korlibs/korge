package korlibs.korge.service.storage

import android.content.*
import korlibs.korge.view.*
import korlibs.render.*

actual class NativeStorage actual constructor(val views: Views) : IStorageWithKeys {
    private val context = views.gameWindow.gameWindowAndroidContextOrNull
    private val preferences = context?.getSharedPreferences("KorgeNativeStorage", Context.MODE_PRIVATE)

    private val memory = InmemoryStorage()

    private inline fun edit(block: SharedPreferences.Editor.() -> Unit) {
        preferences?.edit()?.apply {
            block()
            apply()
        }
    }

    override fun toString(): String = "NativeStorage(${toMap()})"
    actual override fun keys(): List<String> = preferences?.all?.keys?.toList() ?: memory.keys()

    actual override fun set(key: String, value: String) {
        when {
            preferences != null -> edit { putString(key, value) }
            else -> memory[key] = value
        }
	}

	actual override fun getOrNull(key: String): String? {
        return when {
            preferences != null -> preferences.getString(key, null)
            else -> memory.getOrNull(key)
        }
    }

	actual override fun remove(key: String) {
        when {
            preferences != null -> edit { remove(key) }
            else -> memory.remove(key)
        }
	}

	actual override fun removeAll() {
        when {
            preferences != null -> edit { super.removeAll() }
            else -> memory.removeAll()
        }
	}
}

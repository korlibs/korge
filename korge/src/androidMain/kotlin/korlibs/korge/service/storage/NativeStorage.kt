package korlibs.korge.service.storage

import android.content.*
import korlibs.korge.view.*
import korlibs.render.*

actual class NativeStorage actual constructor(val views: Views) : IStorage {
    private val context = (views.gameWindow as AndroidGameWindow).androidContext
    private val preferences = context.getSharedPreferences("KorgeNativeStorage", Context.MODE_PRIVATE)

    private inline fun edit(block: SharedPreferences.Editor.() -> Unit) {
        preferences.edit().apply {
            block()
            apply()
        }
    }

    override fun toString(): String = "NativeStorage(${toMap()})"
    actual fun keys(): List<String> = preferences.getAll().keys.toList()

    actual override fun set(key: String, value: String) {
        edit { putString(key, value) }
	}

	actual override fun getOrNull(key: String): String? = preferences.getString(key, null)

	actual override fun remove(key: String) {
        edit { remove(key) }
	}

	actual override fun removeAll() {
        edit { removeAll() }
	}
}
package com.soywiz.korge.service.storage

import android.content.*
import com.soywiz.korge.view.*
import com.soywiz.korgw.*

actual class NativeStorage actual constructor(val views: Views) : IStorage {
    private val context = (views.gameWindow as AndroidGameWindow).androidContext
    private val preferences = context.getSharedPreferences("KorgeNativeStorage", Context.MODE_PRIVATE)

    private inline fun edit(block: SharedPreferences.Editor.() -> Unit) {
        preferences.edit().apply {
            block()
            apply()
        }
    }

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

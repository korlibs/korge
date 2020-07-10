package com.soywiz.korge.service.storage

import com.soywiz.korge.view.*
import java.io.*
import java.util.*

actual class NativeStorage actual constructor(val views: Views) : IStorage {
	val props = Properties()

	init {
		load()
	}

	private fun load() {
		try {
			FileInputStream(File("game.storage")).use { fis ->
				props.load(fis)
			}
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}

	private fun save() {
		try {
			FileOutputStream(File("game.storage")).use { fout ->
				props.store(fout, "")
			}
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

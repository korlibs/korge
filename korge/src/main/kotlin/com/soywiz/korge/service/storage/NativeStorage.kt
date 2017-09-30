package com.soywiz.korge.service.storage

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

actual object NativeStorage : IStorage {
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

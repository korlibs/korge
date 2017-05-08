package com.soywiz.korge.service.storage.awt

import com.soywiz.korge.service.storage.StorageBase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class AwtStorage : StorageBase() {
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

	override fun set(key: String, value: String) {
		props[key] = value
		save()
	}

	override fun getOrNull(key: String): String? {
		return props[key]?.toString()
	}

	override fun remove(key: String) {
		props.remove(key)
		save()
	}

	override fun removeAll() {
		props.clear()
		save()
	}
}

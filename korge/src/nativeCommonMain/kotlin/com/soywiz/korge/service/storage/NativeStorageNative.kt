package com.soywiz.korge.service.storage

import com.soywiz.kds.*
import com.soywiz.korge.native.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.serialization.json.*

actual object NativeStorage : IStorage {
    private fun saveStr(data: String) = KorgeSimpleNativeSyncIO.writeBytes("settings.json", data.toByteArray(UTF8))
    private fun loadStr(): String = KorgeSimpleNativeSyncIO.readBytes("settings.json").toString(UTF8)

    private var map: LinkedHashMap<String, String>? = null

    private fun ensureMap(): LinkedHashMap<String, String> {
        if (map == null) {
            map = try {
                LinkedHashMap(loadStr().fromJson() as Map<String, String>)
            } catch (e: Throwable) {
                linkedHashMapOf()
            }
        }
        return map!!
    }

    private fun save() {
        saveStr(ensureMap().toJson())
    }

    actual override fun set(key: String, value: String) {
        ensureMap()[key] = value
        save()
    }

    actual override fun getOrNull(key: String): String? {
        return ensureMap()[key]
    }

    actual override fun remove(key: String) {
        ensureMap().remove(key)
        save()
    }

    actual override fun removeAll() {
        ensureMap().clear()
        save()
    }
}


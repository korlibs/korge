package com.soywiz.korge.service.storage

import com.soywiz.kds.*
import com.soywiz.kds.atomic.KdsAtomicRef
import com.soywiz.korge.native.*
import com.soywiz.korge.view.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.serialization.json.*

actual class NativeStorage actual constructor(val views: Views) : IStorage {
    private fun saveStr(data: String) = KorgeSimpleNativeSyncIO.writeBytes("settings.json", data.toByteArray(UTF8))
    private fun loadStr(): String = KorgeSimpleNativeSyncIO.readBytes("settings.json").toString(UTF8)

    private var map = KdsAtomicRef<CopyOnWriteFrozenMap<String, String>?>(null)

    private fun ensureMap(): CopyOnWriteFrozenMap<String, String> {
        if (map.value == null) {
            map.value = CopyOnWriteFrozenMap()
            kotlin.runCatching { map.value!!.putAll(loadStr().fromJson() as Map<String, String>) }
        }
        return map.value!!
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


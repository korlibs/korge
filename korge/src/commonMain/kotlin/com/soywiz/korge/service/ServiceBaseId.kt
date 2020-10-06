package com.soywiz.korge.service

import com.soywiz.kds.linkedHashMapOf

open class ServiceBaseId() {
    private val map: LinkedHashMap<String, String> = linkedHashMapOf()
    operator fun set(platform: String, id: String) { map[platform] = id }
    operator fun get(platform: String) = map[platform] ?: error("Id not set for platform '$platform'")
    override fun toString(): String = "${this::class.simpleName}($map)"
}

fun ServiceBaseId.platform(platform: String) = this[platform]
fun ServiceBaseId.android() = platform("android")
fun ServiceBaseId.ios() = platform("ios")
fun <T : ServiceBaseId> T.platform(platform: String, id: String): T = this.apply { this[platform] = id }
fun <T : ServiceBaseId> T.android(id: String): T = platform("android", id)
fun <T : ServiceBaseId> T.ios(id: String): T = platform("ios", id)


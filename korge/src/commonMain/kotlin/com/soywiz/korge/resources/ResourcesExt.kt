package com.soywiz.korge.resources

import com.soywiz.korim.font.*
import com.soywiz.korim.format.*
import com.soywiz.korinject.*
import com.soywiz.korio.resources.*

suspend fun resources(): Resources = injector().get()

fun resourceBitmap(@ResourcePath path: String, cache: ResourceCache = ResourceCache.GLOBAL) = resource(cache) { root[path].readBitmapSlice() }
fun resourceFont(@ResourcePath path: String, cache: ResourceCache = ResourceCache.GLOBAL) = resource(cache) { root[path].readFont() }
fun resourceBitmapFont(@ResourcePath path: String, cache: ResourceCache = ResourceCache.GLOBAL) = resource(cache) { root[path].readBitmapFont() }
fun resourceTtfFont(@ResourcePath path: String, cache: ResourceCache = ResourceCache.GLOBAL) = resource(cache) { root[path].readTtfFont() }

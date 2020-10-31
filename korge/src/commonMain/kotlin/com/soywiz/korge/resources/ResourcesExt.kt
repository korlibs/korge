package com.soywiz.korge.resources

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.font.*
import com.soywiz.korim.format.*
import com.soywiz.korinject.*
import com.soywiz.korio.resources.*

suspend fun resources(): Resources = injector().get()

fun resourceBitmap(@ResourcePath path: String, mipmaps: Boolean = true, cache: ResourceCache = ResourceCache.LOCAL) = resource(cache) { root[path].readBitmapSlice().also { it.bmp.mipmaps(enable = mipmaps) } }
fun resourceFont(@ResourcePath path: String, mipmaps: Boolean = true, cache: ResourceCache = ResourceCache.LOCAL) = resource(cache) { root[path].readFont(mipmaps = mipmaps) }
fun resourceBitmapFont(@ResourcePath path: String, mipmaps: Boolean = true, cache: ResourceCache = ResourceCache.LOCAL) = resource(cache) { root[path].readBitmapFont(mipmaps = mipmaps) }
fun resourceTtfFont(@ResourcePath path: String, preload: Boolean = false, cache: ResourceCache = ResourceCache.LOCAL) = resource(cache) { root[path].readTtfFont(preload = preload) }

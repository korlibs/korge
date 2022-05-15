package com.soywiz.korge.resources

import com.soywiz.korim.bitmap.mipmaps
import com.soywiz.korim.font.readBitmapFont
import com.soywiz.korim.font.readFont
import com.soywiz.korim.font.readTtfFont
import com.soywiz.korim.format.readBitmapSlice
import com.soywiz.korinject.injector
import com.soywiz.korio.resources.ResourceCache
import com.soywiz.korio.resources.ResourcePath
import com.soywiz.korio.resources.Resources
import com.soywiz.korio.resources.resource

suspend fun resources(): Resources = injector().get()

fun resourceBitmap(@ResourcePath path: String, mipmaps: Boolean = true, cache: ResourceCache = ResourceCache.LOCAL) = resource(cache) { root[path].readBitmapSlice().also { it.bmpBase.mipmaps(enable = mipmaps) } }
fun resourceFont(@ResourcePath path: String, mipmaps: Boolean = true, cache: ResourceCache = ResourceCache.LOCAL) = resource(cache) { root[path].readFont(mipmaps = mipmaps) }
fun resourceBitmapFont(@ResourcePath path: String, mipmaps: Boolean = true, cache: ResourceCache = ResourceCache.LOCAL) = resource(cache) { root[path].readBitmapFont(mipmaps = mipmaps) }
fun resourceTtfFont(@ResourcePath path: String, preload: Boolean = false, cache: ResourceCache = ResourceCache.LOCAL) = resource(cache) { root[path].readTtfFont(preload = preload) }

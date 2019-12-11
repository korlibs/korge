package com.soywiz.korge.ui

import com.soywiz.korge.resources.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korinject.*
import com.soywiz.korio.lang.*

//e: java.lang.UnsupportedOperationException: Class literal annotation arguments are not yet supported: Factory
//@AsyncFactoryClass(UISkin.Factory::class)
@Deprecated("Use new UI")
class UISkin(val views: Views, val texture: BitmapSlice<Bitmap>) {
	val buttonOut = texture.sliceWithSize(0, 0, 64, 64)
	val buttonOver = texture.sliceWithSize(64, 0, 64, 64)
	val buttonDown = texture.sliceWithSize(128, 0, 64, 64)

	class Factory(
		private val vpath: VPath,
		private val resourcesRoot: ResourcesRoot,
		internal val views: Views
	) : AsyncFactory<UISkin> {
		override suspend fun create(): UISkin {
			val texture = try {
				val rpath = vpath.path
				val tex = resourcesRoot[rpath].readBitmapSlice()
				println("UISkin.Factory: $rpath")
				tex
			} catch (e: Throwable) {
				e.printStackTrace()
				println("UISkin.Factory: #WHITE#")
				Bitmaps.white
			}
			return UISkin(views, texture)
		}
	}
}

suspend fun AsyncInjector.getUISkin(path: String) = UISkin.Factory(VPath(path), get(), get())

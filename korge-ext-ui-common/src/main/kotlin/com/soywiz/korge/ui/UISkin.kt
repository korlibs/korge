package com.soywiz.korge.ui

import com.soywiz.korge.render.Texture
import com.soywiz.korge.render.readTexture
import com.soywiz.korge.resources.Path
import com.soywiz.korge.resources.ResourcesRoot
import com.soywiz.korge.view.Views
import com.soywiz.korio.inject.AsyncFactory
import com.soywiz.korio.inject.AsyncFactoryClass
import com.soywiz.korio.lang.printStackTrace

//e: java.lang.UnsupportedOperationException: Class literal annotation arguments are not yet supported: Factory
//@AsyncFactoryClass(UISkin.Factory::class)
class UISkin(val views: Views, val texture: Texture) {
	val buttonOut: Texture = texture.slice(0, 0, 64, 64)
	val buttonOver: Texture = texture.slice(64, 0, 64, 64)
	val buttonDown: Texture = texture.slice(128, 0, 64, 64)

	class Factory(
		private val path: Path,
		private val resourcesRoot: ResourcesRoot,
		internal val views: Views
	) : AsyncFactory<UISkin> {
		suspend override fun create(): UISkin {
			val texture = try {
				val tex = resourcesRoot[path].readTexture(views, mipmaps = true)
				println("UISkin.Factory: ${path.path}")
				tex
			} catch (e: Throwable) {
				e.printStackTrace()
				println("UISkin.Factory: #WHITE#")
				views.whiteTexture
			}
			return UISkin(views, texture)
		}
	}
}

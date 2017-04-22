package com.soywiz.korge.atlas

import com.soywiz.korge.render.TransformedTexture
import com.soywiz.korge.render.readTexture
import com.soywiz.korge.resources.Path
import com.soywiz.korge.resources.ResourcesRoot
import com.soywiz.korge.view.Views
import com.soywiz.korio.inject.AsyncFactory
import com.soywiz.korio.inject.AsyncFactoryClass
import com.soywiz.korio.vfs.VfsFile

@AsyncFactoryClass(Atlas.Factory::class)
class Atlas(val info: AtlasInfo) {
	val textures = hashMapOf<String, TransformedTexture>()

	suspend fun load(views: Views, folder: VfsFile): Atlas = this.apply {
		val atlasTex = folder[info.image].readTexture(views)
		for (frame in info.frames.values) {
			textures[frame.name] = TransformedTexture(
				atlasTex.slice(frame.frame),
				frame.spriteSourceSize.x.toFloat(), frame.spriteSourceSize.y.toFloat(),
				frame.rotated
			)
		}
	}

	class Factory(
		val path: Path,
		val views: Views,
		val resourcesRoot: ResourcesRoot
	) : AsyncFactory<Atlas> {
		suspend override fun create(): Atlas = resourcesRoot[path].readAtlas(views)
	}
}

suspend fun VfsFile.readAtlas(views: Views): Atlas = Atlas(AtlasInfo.loadJsonSpriter(this.readString())).load(views, this.parent)

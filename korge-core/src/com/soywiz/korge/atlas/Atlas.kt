package com.soywiz.korge.atlas

import com.soywiz.korge.render.TransformedTexture
import com.soywiz.korge.render.readTexture
import com.soywiz.korge.resources.Path
import com.soywiz.korge.resources.ResourcesRoot
import com.soywiz.korge.view.Views
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.inject.AsyncFactory
import com.soywiz.korio.inject.AsyncFactoryClass
import com.soywiz.korio.vfs.VfsFile

@AsyncFactoryClass(Atlas.Factory::class)
class Atlas(val info: AtlasInfo) {
	val textures = hashMapOf<String, TransformedTexture>()

	operator fun get(name: String) = textures[name] ?: invalidOp("Can't find texture '$name' in atlas")

	suspend internal fun load(views: Views, folder: VfsFile): Atlas = this.apply {
		val atlasTex = folder[info.image].readTexture(views)
		for ((frameName, frame) in info.frames) {
			textures[frameName] = TransformedTexture(
				atlasTex.slice(frame.frame.rect),
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
		suspend override fun create(): Atlas {
			if (path.path.endsWith(".atlas")) {
				return resourcesRoot[path].appendExtension("json").readAtlas(views)
			} else {
				return resourcesRoot[path].readAtlas(views)
			}
		}
	}
}

suspend fun VfsFile.readAtlas(views: Views): Atlas = Atlas(AtlasInfo.loadJsonSpriter(this.readString())).load(views, this.parent)

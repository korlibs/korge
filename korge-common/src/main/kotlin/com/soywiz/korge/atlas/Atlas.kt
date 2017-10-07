package com.soywiz.korge.atlas

import com.soywiz.korge.plugin.KorgePlugin
import com.soywiz.korge.render.TransformedTexture
import com.soywiz.korge.render.readTexture
import com.soywiz.korge.resources.Path
import com.soywiz.korge.resources.ResourcesRoot
import com.soywiz.korge.resources.VPath
import com.soywiz.korge.view.Views
import com.soywiz.korio.error.invalidOp
import com.soywiz.korio.inject.AsyncFactory
import com.soywiz.korio.serialization.Mapper
import com.soywiz.korio.vfs.VfsFile

object AtlasPlugin : KorgePlugin() {
	init {
		Mapper.registerType { AtlasInfo.Meta(it["app"].gen(), it["format"].gen(), it["image"].gen(), it["scale"].gen(), it["size"].gen(), it["version"].gen()) }
		Mapper.registerType { AtlasInfo.Rect(it["x"].gen(), it["y"].gen(), it["w"].gen(), it["h"].gen()) }
		Mapper.registerType { AtlasInfo.Size(it["w"].gen(), it["h"].gen()) }
		Mapper.registerType { AtlasInfo.Entry(it["frame"].gen(), it["rotated"].gen(), it["source"].gen(), it["spriteSourceSize"].gen(), it["trimmed"].gen()) }
		Mapper.registerType { AtlasInfo(it["frames"].genMap(), it["meta"].gen()) }
	}

	suspend override fun register(views: Views) {
		views.injector
			.mapFactory(Atlas::class) {
				//AnLibrary.Factory(getOrNull(), getOrNull(), get(), get(), get()) // @TODO: Kotlin.js bug
				Atlas.Factory(
					getOrNull(Path::class),
					getOrNull(VPath::class),
					get(Views::class),
					get(ResourcesRoot::class)
				)
			}
	}
}

//e: java.lang.UnsupportedOperationException: Class literal annotation arguments are not yet supported: Factory
//@AsyncFactoryClass(Atlas.Factory::class)
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
		val path: Path?,
		val vpath: VPath?,
		val views: Views,
		val resourcesRoot: ResourcesRoot
	) : AsyncFactory<Atlas> {
		suspend override fun create(): Atlas {
			val rpath = path?.path ?: vpath?.path ?: ""
			if (rpath.endsWith(".atlas")) {
				return resourcesRoot[rpath].appendExtension("json").readAtlas(views)
			} else {
				return resourcesRoot[rpath].readAtlas(views)
			}
		}
	}
}

suspend fun VfsFile.readAtlas(views: Views): Atlas {
	return Atlas(AtlasInfo.loadJsonSpriter(this.readString())).load(views, this.parent)
}

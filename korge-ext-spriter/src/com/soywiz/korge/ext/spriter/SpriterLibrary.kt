package com.soywiz.korge.ext.spriter

import com.soywiz.korge.atlas.Atlas
import com.soywiz.korge.ext.spriter.com.brashmonkey.spriter.Data
import com.soywiz.korge.ext.spriter.com.brashmonkey.spriter.SCMLReader
import com.soywiz.korge.render.TransformedTexture
import com.soywiz.korge.render.readTexture
import com.soywiz.korge.resources.Path
import com.soywiz.korge.resources.ResourcesRoot
import com.soywiz.korge.view.Views
import com.soywiz.korio.inject.AsyncFactory
import com.soywiz.korio.inject.AsyncFactoryClass
import com.soywiz.korio.vfs.VfsFile
import kotlin.collections.set

@AsyncFactoryClass(SpriterLibrary.Loader::class)
class SpriterLibrary(val views: Views, val data: Data, val atlas: Map<String, TransformedTexture>) {
	val entityNames = data.entities.map { it.name }

	fun create(entityName: String, animationName1: String? = null, animationName2: String? = animationName1): SpriterView {
		val entity = data.getEntity(entityName)
		return SpriterView(views, this, entity!!, animationName1 ?: entity.getAnimation(0).name, animationName2 ?: entity.getAnimation(0).name)
	}

	class Loader(
		val views: Views,
		val path: Path,
		val resourcesRoot: ResourcesRoot
	) : AsyncFactory<SpriterLibrary> {
		suspend override fun create(): SpriterLibrary = resourcesRoot[path].readSpriterLibrary(views)
	}
}

suspend fun VfsFile.readSpriterLibrary(views: Views): SpriterLibrary {
	val file = this
	val scmlContent = file.readString()
	val reader = SCMLReader(scmlContent)
	val data = reader.data

	// @TODO: Atlas reading!
	val images = hashMapOf<String, TransformedTexture>()

	for (atlasName in data.atlases) {
		val atlasFile = file.parent[atlasName]
		val atlas = Atlas.loadJsonSpriter(atlasFile.readString())
		val atlasTex = atlasFile.parent[atlas.image].readTexture(views.ag)
		for (frame in atlas.frames.values) {
			images[frame.name] = TransformedTexture(
				atlasTex.slice(frame.frame),
				frame.spriteSourceSize.x.toFloat(), frame.spriteSourceSize.y.toFloat(),
				frame.rotated
			)
		}
		//println(atlas)
	}

	for (folder in data.folders) {
		for (f in folder.files) {
			if (f.name in images) continue
			val image = file.parent[f.name]
			val tex = image.readTexture(views.ag)
			images[f.name] = TransformedTexture(tex)
			//println("${f.name}: ${tex.width}x${tex.height} = ${f.size.width}x${f.size.height}")

		}
	}

	return SpriterLibrary(views, data, images)
}

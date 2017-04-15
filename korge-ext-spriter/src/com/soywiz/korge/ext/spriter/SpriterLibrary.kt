package com.soywiz.korge.ext.spriter

import com.brashmonkey.spriter.Data
import com.brashmonkey.spriter.SCMLReader
import com.soywiz.korge.render.Texture
import com.soywiz.korge.render.readTexture
import com.soywiz.korge.resources.Path
import com.soywiz.korge.view.Views
import com.soywiz.korio.inject.AsyncFactory
import com.soywiz.korio.inject.AsyncFactoryClass
import com.soywiz.korio.vfs.ResourcesVfs
import kotlin.collections.set

@AsyncFactoryClass(SpriterLibrary.Loader::class)
class SpriterLibrary(val views: Views, val data: Data, val atlas: Map<String, Texture>) {
	val entityNames = data.entities.map { it.name }

	fun create(entityName: String, animationName1: String? = null, animationName2: String? = animationName1): SpriterView {
		val entity = data.getEntity(entityName)
		return SpriterView(views, this, entity!!, animationName1 ?: entity.getAnimation(0).name, animationName2 ?: entity.getAnimation(0).name)
	}

	class Loader(
		val views: Views,
		val path: Path
	) : AsyncFactory<SpriterLibrary> {
		suspend override fun create(): SpriterLibrary {
			val file = ResourcesVfs[path.path]
			val scmlContent = file.readString()
			val reader = SCMLReader(scmlContent)
			val data = reader.data

			val images = hashMapOf<String, Texture>()
			for (folder in data.folders) {
				for (f in folder.files) {
					val image = file.parent[f!!.name]
					val tex = image.readTexture(views.ag)
					images[f.name] = tex
					//println("${f.name}: ${tex.width}x${tex.height} = ${f.size.width}x${f.size.height}")

				}
			}

			return SpriterLibrary(views, data, images)
		}
	}

}

package com.soywiz.korge.ext.spriter

import com.brashmonkey.spriter.*
import com.soywiz.korge.render.RenderContext
import com.soywiz.korge.render.Texture
import com.soywiz.korge.render.readTexture
import com.soywiz.korge.resources.Path
import com.soywiz.korge.view.View
import com.soywiz.korge.view.Views
import com.soywiz.korio.inject.AsyncFactory
import com.soywiz.korio.inject.AsyncFactoryClass
import com.soywiz.korio.vfs.ResourcesVfs
import com.soywiz.korma.Matrix2d
import kotlin.collections.set

@AsyncFactoryClass(SpriterLibraryLoader::class)
class SpriterLibrary(val views: Views, val data: Data, val atlas: Map<String, Texture>) {
	val entityNames = data.entities.map { it.name }

	fun create(entityName: String, animationName: String? = null): SpriterView {
		val entity = data.getEntity(entityName)
		val animation = entity.getAnimation(animationName) ?: entity.getAnimation(0)
		return SpriterView(views, this, entity, animation)
	}
}

class SpriterView(views: Views, val library: SpriterLibrary, val entity: Entity, var animation: Animation) : View(views) {
	val player = Player(entity)

	init {
		updateInternal(0)
	}

	override fun updateInternal(dtMs: Int) {
		super.updateInternal(dtMs)
		player.speed = dtMs
		player.update()
	}

	private val t1: Matrix2d = Matrix2d()
	private val t2: Matrix2d = Matrix2d()
	private val m: Matrix2d = Matrix2d()

	override fun render(ctx: RenderContext) {
		val data = library.data
		val batch = ctx.batch
		for (obj in player.objectIterator()) {
			val file = library.data.getFile(obj.ref)
			val tex = library.atlas[file.name] ?: views.dummyTexture

			t1.setTransform(
				obj.position.x.toDouble(), obj.position.y.toDouble(),
				obj.scale.x.toDouble(), -obj.scale.y.toDouble(),
				-Math.toRadians(obj.angle.toDouble()),
				0.0, 0.0
			)
			//t2.setToIdentity()
			t2.copyFrom(globalMatrix)
			//t2.premulitply(globalMatrix)
			t2.prescale(1.0, -1.0)
			t2.premulitply(t1)
			val px = obj.pivot.x * tex.width.toFloat()
			val py = (1.0 - obj.pivot.y) * tex.height.toFloat()
			//file
			//println("$sw, $sh")
			//println("${file.pivot} : ${obj.pivot}")
			batch.addQuad(tex, -px.toFloat(), -py.toFloat(), tex.width.toFloat(), tex.height.toFloat(), t2)
		}
	}

}

class SpriterLibraryLoader(
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
				val image = file.parent[f.name]
				val tex = image.readTexture(views.ag)
				images[f.name] = tex
				//println("${f.name}: ${tex.width}x${tex.height} = ${f.size.width}x${f.size.height}")

			}
		}

		return SpriterLibrary(views, data, images)
	}
}

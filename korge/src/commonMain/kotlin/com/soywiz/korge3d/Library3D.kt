package com.soywiz.korge3d

import com.soywiz.kds.*
import com.soywiz.korge3d.animation.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*
import com.soywiz.korma.geom.*

@Korge3DExperimental
data class Library3D(
	val cameraDefs: FastStringMap<CameraDef> = FastStringMap(),
	val lightDefs: FastStringMap<LightDef> = FastStringMap(),
	val materialDefs: FastStringMap<MaterialDef> = FastStringMap(),
	val effectDefs: FastStringMap<EffectDef> = FastStringMap(),
	val imageDefs: FastStringMap<ImageDef> = FastStringMap(),
	val geometryDefs: FastStringMap<GeometryDef> = FastStringMap(),
	val skinDefs: FastStringMap<SkinDef> = FastStringMap(),
	val animationDefs: FastStringMap<Animation3D> = FastStringMap()
) {
	suspend fun loadTextures() {
		imageDefs.fastValueForEach { image ->
			image.texure = resourcesVfs[image.initFrom].readBitmap()
		}
	}

	val boneDefs = FastStringMap<BoneDef>()

	fun instantiateMaterials() {
		geometryDefs.fastValueForEach { geom ->
			for (bone in geom.skin?.bones ?: listOf()) {
				boneDefs[bone.name] = bone
			}
			geom.mesh.material = geom.material?.instantiate()
		}
	}

	val library = this

	val mainScene = Scene3D(this).apply {
		id = "MainScene"
		name = "MainScene"
	}
	var scenes = FastStringMap<Scene3D>()

	open class Instance3D(val library: Library3D) {
		val transform = Matrix3D()
		var def: Def? = null
		val children = arrayListOf<Instance3D>()
		var id: String = ""
		var sid: String? = null
		var name: String = ""
		var type: String = ""
		var skin: SkinDef? = null
		var skeleton: Instance3D? = null
		var skeletonId: String? = null
	}

	open class Scene3D(library: Library3D) : Instance3D(library) {
	}

	open class Def

	open class Pong

	open class ImageDef(val id: String, val name: String, val initFrom: String, var texure: Bitmap? = null) : Def() {

	}

	open class ObjectDef : Def()

	open class MaterialDef(val id: String, val name: String, val effects: List<EffectDef>) : Def()

	open class LightDef : ObjectDef()

	open class CameraDef : ObjectDef()
	data class PerspectiveCameraDef(val xfov: Angle, val zmin: Double, val zmax: Double) : CameraDef()

	interface LightKindDef {
		val sid: String
	}

	open class LightTexDef(override val sid: String, val texture: EffectParamSampler2D?, val lightKind: String) :
		LightKindDef

	open class LightColorDef(override val sid: String, val color: RGBA, val lightKind: String) : LightKindDef

	data class EffectParamSurface(val surfaceType: String, val initFrom: ImageDef?)
	data class EffectParamSampler2D(val surface: EffectParamSurface?)

	open class EffectDef() : Def()

	data class StandardEffectDef(
		val id: String,
		val name: String,
		val emission: LightKindDef?,
		val ambient: LightKindDef?,
		val diffuse: LightKindDef?,
		val specular: LightKindDef?,
		val shiness: Float?,
		val index_of_refraction: Float?
	) : EffectDef()

	data class GeometryDef(
		val mesh: Mesh3D,
		val skin: SkinDef? = null,
		val material: MaterialDef? = null
	) : ObjectDef()

	data class BoneDef constructor(val index: Int, val name: String, val invBindMatrix: Matrix3D) : Def() {
		lateinit var skin: SkinDef
		fun toBone() = Bone3D(index, name, invBindMatrix.clone())
	}

	data class SkinDef(
		val controllerId: String,
		val controllerName: String,
		val bindShapeMatrix: Matrix3D,
		val skinSource: String,
		val bones: List<BoneDef>
	) : Def() {
		fun toSkin() = Skin3D(bindShapeMatrix, bones.map { it.toBone() })
	}


	class PointLightDef(
		val color: RGBA,
		val constantAttenuation: Double,
		val linearAttenuation: Double,
		val quadraticAttenuation: Double
	) : LightDef()

	class AmbientLightDef(
		val color: RGBA
	) : LightDef()
}

@Korge3DExperimental
class LibraryInstantiateContext {
	val viewsById = FastStringMap<View3D>()
}

@Korge3DExperimental
fun Library3D.Instance3D.instantiate(jointParent: Joint3D? = null, ctx: LibraryInstantiateContext = LibraryInstantiateContext()): View3D {
	val def = this.def
	val view: View3D = when (def) {
		null -> {
			if (type.equals("JOINT", ignoreCase = true)) {
				val it = Joint3D(id, name, sid ?: "unknownSid", jointParent, this.transform)
                jointParent?.childJoints?.add(it)
                jointParent?.children?.add(it)
                it
			} else {
				Container3D()
			}
		}
		is Library3D.GeometryDef -> {
			val skeletonInstance = if (skeletonId != null) ctx.viewsById[skeletonId!!] as? Joint3D? else null
			ViewWithMesh3D(def.mesh, skeletonInstance?.let { Skeleton3D(def.mesh.skin!!, it) })
		}
		is Library3D.PerspectiveCameraDef -> {
			Camera3D.Perspective(def.xfov, def.zmin, def.zmax)
		}
		is Library3D.PointLightDef -> {
			Light3D(def.color, def.constantAttenuation, def.linearAttenuation, def.quadraticAttenuation)
		}
		is Library3D.AmbientLightDef -> {
			Light3D(def.color, 0.00001, 0.00001, 0.00001)
		}
		else -> TODO("def=$def")
	}
	view.id = this.id
	ctx.viewsById[this.id] = view
	view.name = this.name
	//view.localTransform.setMatrix(this.transform.clone().transpose())
	view.transform.setMatrix(this.transform)
	if (view is Joint3D) {
		for (child in children) {
			child.instantiate(view, ctx)
		}
	} else if (view is Container3D) {
		for (child in children) {
			view.addChild(child.instantiate(null, ctx))
		}
	}
	return view
}

@Korge3DExperimental
fun Library3D.LightKindDef.instantiate(): Material3D.Light {
	return when (this) {
		is Library3D.LightTexDef -> Material3D.LightTexture(this.texture?.surface?.initFrom?.texure)
		is Library3D.LightColorDef -> Material3D.LightColor(this.color)
		else -> error("Unsupported $this")
	}
}

@Korge3DExperimental
fun Library3D.MaterialDef.instantiate(): Material3D {
	val effect = this.effects.firstOrNull() as? Library3D.StandardEffectDef?
	return Material3D(
		emission = effect?.emission?.instantiate() ?: Material3D.LightColor(Colors.BLACK),
		ambient = effect?.ambient?.instantiate() ?: Material3D.LightColor(Colors.BLACK),
		diffuse = effect?.diffuse?.instantiate() ?: Material3D.LightColor(Colors.BLACK),
		specular = effect?.specular?.instantiate() ?: Material3D.LightColor(Colors.BLACK),
		shiness = effect?.shiness ?: 0.5f,
		indexOfRefraction = effect?.index_of_refraction ?: 1f
	)
}

package com.soywiz.korge3d.experimental

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.kmem.*
import com.soywiz.korag.*
import com.soywiz.korag.shader.*
import com.soywiz.korge.render.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*

@Korge3DExperimental
inline fun Container.scene3D(views: Views3D = Views3D(), callback: Stage3D.() -> Unit = {}): Stage3DView =
	Stage3DView(Stage3D(views).apply(callback)).addTo(this)

@Korge3DExperimental
class Views3D {
}

@Korge3DExperimental
fun Container3D.light(callback: Light3D.() -> Unit = {}) = Light3D().apply(callback).addTo(this)

@Korge3DExperimental
open class Light3D(
	var color: RGBA = Colors.WHITE,
	var constantAttenuation: Double = 1.0,
	var linearAttenuation: Double = 0.0,
	var quadraticAttenuation: Double = 0.00111109
) : View3D() {
	internal val colorVec = Vector3D()
	internal val attenuationVec = Vector3D()

	fun setTo(
		color: RGBA = Colors.WHITE,
		constantAttenuation: Double = 1.0,
		linearAttenuation: Double = 0.0,
		quadraticAttenuation: Double = 0.00111109
	) = this.apply {
		this.color = color
		this.constantAttenuation = constantAttenuation
		this.linearAttenuation = linearAttenuation
		this.quadraticAttenuation = quadraticAttenuation
	}

	override fun render(ctx: RenderContext3D) {
	}
}

@Korge3DExperimental
class Stage3D(val views: Views3D) : Container3D() {
	lateinit var view: Stage3DView
	//var ambientColor: RGBA = Colors.WHITE
	var ambientColor: RGBA = Colors.BLACK // No ambient light
	var ambientPower: Double = 0.3
	var camera: Camera3D = Camera3D.Perspective().apply {
		positionLookingAt(0, 1, -10, 0, 0, 0)
	}
}

@Korge3DExperimental
class Stage3DView(val stage3D: Stage3D) : View() {
	init {
		stage3D.view = this
	}

	private val ctx3D = RenderContext3D()
	override fun renderInternal(ctx: RenderContext) {
		ctx.flush()
		ctx.ag.clear(depth = 1f, clearColor = false)
		//ctx.ag.clear(color = Colors.RED)
		ctx3D.ag = ctx.ag
		ctx3D.rctx = ctx
		ctx3D.projMat.copyFrom(stage3D.camera.getProjMatrix(ctx.ag.backWidth.toDouble(), ctx.ag.backHeight.toDouble()))
		ctx3D.cameraMat.copyFrom(stage3D.camera.transform.matrix)
		ctx3D.ambientColor.setToColorPremultiplied(stage3D.ambientColor).scale(stage3D.ambientPower)
		ctx3D.cameraMatInv.invert(stage3D.camera.transform.matrix)
		ctx3D.projCameraMat.multiply(ctx3D.projMat, ctx3D.cameraMatInv)
		ctx3D.lights.clear()
		stage3D.foreachDescendant {
			if (it is Light3D) {
				if (it.active) ctx3D.lights.add(it)
			}
		}
		stage3D.render(ctx3D)
	}
}

@Korge3DExperimental
fun View3D?.foreachDescendant(handler: (View3D) -> Unit) {
	if (this != null) {
		handler(this)
		if (this is Container3D) {
			this.children.fastForEach { child ->
				child.foreachDescendant(handler)
			}
		}
	}
}

@Korge3DExperimental
class RenderContext3D() {
	lateinit var ag: AG
	lateinit var rctx: RenderContext
	val shaders = Shaders3D()
	val textureUnit = AG.TextureUnit()
	val bindMat4 = Matrix3D()
	val bones = Array(128) { Matrix3D() }
	val tmepMat = Matrix3D()
	val projMat: Matrix3D = Matrix3D()
	val lights = arrayListOf<Light3D>()
	val projCameraMat: Matrix3D = Matrix3D()
	val cameraMat: Matrix3D = Matrix3D()
	val cameraMatInv: Matrix3D = Matrix3D()
	val dynamicVertexBufferPool = Pool { ag.createVertexBuffer() }
	val ambientColor: Vector3D = Vector3D()
}

@Korge3DExperimental
abstract class View3D {
	var active = true
	var id: String? = null
	var name: String? = null
	val transform = Transform3D()

	///////

	var x: Double
		set(localX) = run { transform.setTranslation(localX, y, z, localW) }
		get() = transform.translation.x.toDouble()

	var y: Double
		set(localY) = run { transform.setTranslation(x, localY, z, localW) }
		get() = transform.translation.y.toDouble()

	var z: Double
		set(localZ) = run { transform.setTranslation(x, y, localZ, localW) }
		get() = transform.translation.z.toDouble()

	var localW: Double
		set(localW) = run { transform.setTranslation(x, y, z, localW) }
		get() = transform.translation.w.toDouble()

	///////

	var scaleX: Double
		set(scaleX) = run { transform.setScale(scaleX, scaleY, scaleZ, localScaleW) }
		get() = transform.scale.x.toDouble()

	var scaleY: Double
		set(scaleY) = run { transform.setScale(scaleX, scaleY, scaleZ, localScaleW) }
		get() = transform.scale.y.toDouble()

	var scaleZ: Double
		set(scaleZ) = run { transform.setScale(scaleX, scaleY, scaleZ, localScaleW) }
		get() = transform.scale.z.toDouble()

	var localScaleW: Double
		set(scaleW) = run { transform.setScale(scaleX, scaleY, scaleZ, scaleW) }
		get() = transform.scale.w.toDouble()

	///////

	var rotationX: Angle
		set(rotationX) = run { transform.setRotation(rotationX, rotationY, rotationZ) }
		get() = transform.rotationEuler.x

	var rotationY: Angle
		set(rotationY) = run { transform.setRotation(rotationX, rotationY, rotationZ) }
		get() = transform.rotationEuler.y

	var rotationZ: Angle
		set(rotationZ) = run { transform.setRotation(rotationX, rotationY, rotationZ) }
		get() = transform.rotationEuler.z

	///////

	var rotationQuatX: Double
		set(rotationQuatX) = run { transform.setRotation(rotationQuatX, rotationQuatY, rotationQuatZ, rotationQuatW) }
		get() = transform.rotation.x

	var rotationQuatY: Double
		set(rotationQuatY) = run { transform.setRotation(rotationQuatX, rotationQuatY, rotationQuatZ, rotationQuatW) }
		get() = transform.rotation.y

	var rotationQuatZ: Double
		set(rotationQuatZ) = run { transform.setRotation(rotationQuatX, rotationQuatY, rotationQuatZ, rotationQuatW) }
		get() = transform.rotation.z

	var rotationQuatW: Double
		set(rotationQuatW) = run { transform.setRotation(rotationQuatX, rotationQuatY, rotationQuatZ, rotationQuatW) }
		get() = transform.rotation.w

	///////

	internal var _parent: Container3D? = null

	var parent: Container3D?
		set(value) {
			_parent = value
			_parent?.addChild(this)
		}
		get() = _parent

	val modelMat = Matrix3D()
	//val position = Vector3D()

	abstract fun render(ctx: RenderContext3D)
}

@Korge3DExperimental
class Skeleton3D(val skin: Skin3D, val headJoint: Joint3D) : View3D() {
	val allJoints = headJoint.descendantsAndThis
	val jointsByName = allJoints.associateBy { it.jname }.toFast()
	val jointsBySid = allJoints.associateBy { it.jsid }.toFast()

	override fun render(ctx: RenderContext3D) {
	}
}

private fun <T : Any> Map<String, T>.toFast() = FastStringMap<T>().apply {
	@Suppress("MapGetWithNotNullAssertionOperator")
	for (k in this@toFast.keys) {
		this[k] = this@toFast[k]!!
	}
}

@Korge3DExperimental
open class Joint3D constructor(
	val jid: String,
	val jname: String,
	val jsid: String,
	val jointParent: Joint3D? = null,
	initialMatrix: Matrix3D
) : Container3D() {
	init {
		this.transform.setMatrix(initialMatrix)
		this.name = jname
		this.id = jid
		if (jointParent != null) {
			this.parent = jointParent
		}

	}

	val poseMatrix = this.transform.globalMatrix.clone()
	val poseMatrixInv = poseMatrix.clone().invert()

	val childJoints = arrayListOf<Joint3D>()
	val descendants: List<Joint3D> get() = childJoints.flatMap { it.descendantsAndThis }
	val descendantsAndThis: List<Joint3D> get() = listOf(this) + descendants

	//val jointTransform = Transform3D()

	override fun render(ctx: RenderContext3D) {
	}

	override fun toString(): String = "Joint3D(id=$jid, name=$name, sid=$jsid)"
}

@Korge3DExperimental
open class Container3D : View3D() {
	val children = arrayListOf<View3D>()

	fun removeChild(child: View3D) {
		children.remove(child)
	}

	fun addChild(child: View3D) {
		child.removeFromParent()
		children += child
		child._parent = this
		child.transform.parent = this.transform
	}

	operator fun plusAssign(child: View3D) = addChild(child)

	override fun render(ctx: RenderContext3D) {
		children.fastForEach {
			it.render(ctx)
		}
	}
}

@Korge3DExperimental
fun View3D.removeFromParent() {
	parent?.removeChild(this)
	parent = null
}

@Korge3DExperimental
inline fun <reified T : View3D> View3D?.findByType() = sequence<T> {
	for (it in descendants()) {
		if (it is T) yield(it)
	}
}

@Korge3DExperimental
inline fun <reified T : View3D> View3D?.findByTypeWithName(name: String) = sequence<T> {
	for (it in descendants()) {
		if (it is T && it.name == name) yield(it)
	}
}

@Korge3DExperimental
fun View3D?.descendants(): Sequence<View3D> = sequence<View3D> {
	val view = this@descendants ?: return@sequence
	yield(view)
	if (view is Container3D) {
		view.children.fastForEach {
			yieldAll(it.descendants())
		}
	}
}

@Korge3DExperimental
operator fun View3D?.get(name: String): View3D? {
	if (this?.id == name) return this
	if (this?.name == name) return this
	if (this is Container3D) {
		this.children.fastForEach {
			val result = it[name]
			if (result != null) return result
		}
	}
	return null
}

@Korge3DExperimental
fun <T : View3D> T.name(name: String) = this.apply { this.name = name }

@Korge3DExperimental
inline fun <T : View3D> T.position(x: Number, y: Number, z: Number, w: Number = 1f): T = this.apply {
	transform.setTranslation(x, y, z, w)
}

@Korge3DExperimental
inline fun <T : View3D> T.rotation(x: Angle = 0.degrees, y: Angle = 0.degrees, z: Angle = 0.degrees): T = this.apply {
	transform.setRotation(x, y, z)
}

@Korge3DExperimental
inline fun <T : View3D> T.scale(x: Number = 1, y: Number = 1, z: Number = 1, w: Number = 1): T = this.apply {
	transform.setScale(x, y, z, w)
}

@Korge3DExperimental
inline fun <T : View3D> T.lookAt(x: Number, y: Number, z: Number): T = this.apply {
	transform.lookAt(x, y, z)
}

@Korge3DExperimental
inline fun <T : View3D> T.positionLookingAt(px: Number, py: Number, pz: Number, tx: Number, ty: Number, tz: Number): T =
	this.apply {
		transform.setTranslationAndLookAt(px, py, pz, tx, ty, tz)
	}

@Korge3DExperimental
fun <T : View3D> T.addTo(container: Container3D) = this.apply {
	container.addChild(this)
}

@Korge3DExperimental
data class Bone3D constructor(
	val index: Int,
	val name: String,
	val invBindMatrix: Matrix3D
)

@Korge3DExperimental
data class Skin3D(val invBindShapeMatrix: Matrix3D, val bones: List<Bone3D>) {
	val bindShapeMatrix = invBindShapeMatrix.clone().invert()
	val matrices = Array(bones.size) { Matrix3D() }
}

@Korge3DExperimental
data class Material3D(
	val emission: Material3D.Light,
	val ambient: Material3D.Light,
	val diffuse: Material3D.Light,
	val specular: Material3D.Light,
	val shiness: Float,
	val indexOfRefraction: Float
) {
	@Korge3DExperimental
	open class Light(val kind: String)

	@Korge3DExperimental
	data class LightColor(val color: RGBA) : Light("color") {
		val colorVec = Vector3D().setToColor(color)
	}

	@Korge3DExperimental
	data class LightTexture(val bitmap: Bitmap?) : Light("texture") {
		val textureUnit = AG.TextureUnit()
	}

	val kind: String = "${emission.kind}_${ambient.kind}_${diffuse.kind}_${specular.kind}"
}

@Korge3DExperimental
class Mesh3D constructor(
	val data: FloatArray,
	val layout: VertexLayout,
	val program: Program?,
	val drawType: AG.DrawType,
	val hasTexture: Boolean = false,
	val maxWeights: Int = 0
) {
	var skin: Skin3D? = null

	val fbuffer by lazy {
		FBuffer.alloc(data.size * 4).apply {
			setAlignedArrayFloat32(0, this@Mesh3D.data, 0, this@Mesh3D.data.size)
		}
		//FBuffer.wrap(MemBufferAlloc(data.size * 4)).apply {
		//	arraycopy(this@Mesh3D.data, 0, this@apply.mem, 0, this@Mesh3D.data.size) // Bug in kmem-js?
		//}
	}

	var material: Material3D? = null

	//val modelMat = Matrix3D()
	val vertexSizeInBytes = layout.totalSize
	val vertexSizeInFloats = vertexSizeInBytes / 4
	val vertexCount = data.size / vertexSizeInFloats

	init {
		//println("vertexCount: $vertexCount, vertexSizeInFloats: $vertexSizeInFloats, data.size: ${data.size}")
	}
}

@Korge3DExperimental
inline fun Container3D.box(
	width: Number = 1,
	height: Number = width,
	depth: Number = height,
	callback: Cube.() -> Unit = {}
): Cube {
	return Cube(width.toDouble(), height.toDouble(), depth.toDouble()).apply(callback).addTo(this)
}

@Korge3DExperimental
class Cube(var width: Double, var height: Double, var depth: Double) : ViewWithMesh3D(mesh) {
	override fun prepareExtraModelMatrix(mat: Matrix3D) {
		mat.identity().scale(width, height, depth)
	}

	companion object {
		private val cubeSize = .5f

		private val vertices = floatArrayOf(
			-cubeSize, -cubeSize, -cubeSize, 1f, 0f, 0f,  //p1
			-cubeSize, -cubeSize, +cubeSize, 1f, 0f, 0f,  //p2
			-cubeSize, +cubeSize, +cubeSize, 1f, 0f, 0f,  //p3
			-cubeSize, -cubeSize, -cubeSize, 1f, 0f, 0f,  //p1
			-cubeSize, +cubeSize, +cubeSize, 1f, 0f, 0f,  //p3
			-cubeSize, +cubeSize, -cubeSize, 1f, 0f, 0f,  //p4

			+cubeSize, +cubeSize, -cubeSize, 0f, 1f, 0f,  //p5
			-cubeSize, -cubeSize, -cubeSize, 0f, 1f, 0f,  //p1
			-cubeSize, +cubeSize, -cubeSize, 0f, 1f, 0f,  //p4
			+cubeSize, +cubeSize, -cubeSize, 0f, 1f, 0f,  //p5
			+cubeSize, -cubeSize, -cubeSize, 0f, 1f, 0f,  //p7
			-cubeSize, -cubeSize, -cubeSize, 0f, 1f, 0f,  //p1

			+cubeSize, -cubeSize, +cubeSize, 0f, 0f, 1f,  //p6
			-cubeSize, -cubeSize, -cubeSize, 0f, 0f, 1f,  //p1
			+cubeSize, -cubeSize, -cubeSize, 0f, 0f, 1f,  //p7
			+cubeSize, -cubeSize, +cubeSize, 0f, 0f, 1f,  //p6
			-cubeSize, -cubeSize, +cubeSize, 0f, 0f, 1f,  //p2
			-cubeSize, -cubeSize, -cubeSize, 0f, 0f, 1f,  //p1

			+cubeSize, +cubeSize, +cubeSize, 0f, 1f, 1f,  //p8
			+cubeSize, +cubeSize, -cubeSize, 0f, 1f, 1f,  //p5
			-cubeSize, +cubeSize, -cubeSize, 0f, 1f, 1f,  //p4
			+cubeSize, +cubeSize, +cubeSize, 0f, 1f, 1f,  //p8
			-cubeSize, +cubeSize, -cubeSize, 0f, 1f, 1f,  //p4
			-cubeSize, +cubeSize, +cubeSize, 0f, 1f, 1f,  //p3

			+cubeSize, +cubeSize, +cubeSize, 1f, 1f, 0f,  //p8
			-cubeSize, +cubeSize, +cubeSize, 1f, 1f, 0f,  //p3
			+cubeSize, -cubeSize, +cubeSize, 1f, 1f, 0f,  //p6
			-cubeSize, +cubeSize, +cubeSize, 1f, 1f, 0f,  //p3
			-cubeSize, -cubeSize, +cubeSize, 1f, 1f, 0f,  //p2
			+cubeSize, -cubeSize, +cubeSize, 1f, 1f, 0f,  //p6

			+cubeSize, +cubeSize, +cubeSize, 1f, 0f, 1f,  //p8
			+cubeSize, -cubeSize, -cubeSize, 1f, 0f, 1f,  //p7
			+cubeSize, +cubeSize, -cubeSize, 1f, 0f, 1f,  //p5
			+cubeSize, -cubeSize, -cubeSize, 1f, 0f, 1f,  //p7
			+cubeSize, +cubeSize, +cubeSize, 1f, 0f, 1f,  //p8
			+cubeSize, -cubeSize, +cubeSize, 1f, 0f, 1f   //p6
		)

		val mesh = Mesh3D(
			vertices,
			Shaders3D.layoutPosCol,
			Shaders3D.programColor3D,
			AG.DrawType.TRIANGLES,
			hasTexture = false
		)
	}
}

@Korge3DExperimental
inline fun Container3D.mesh(mesh: Mesh3D, callback: ViewWithMesh3D.() -> Unit = {}): ViewWithMesh3D {
	return ViewWithMesh3D(mesh).apply(callback).addTo(this)
}

@Korge3DExperimental
open class ViewWithMesh3D(
	var mesh: Mesh3D,
	var skeleton: Skeleton3D? = null
) : View3D() {

	private val uniformValues = AG.UniformValues()
	private val rs = AG.RenderState(depthFunc = AG.CompareMode.LESS_EQUAL)
	//private val rs = AG.RenderState(depthFunc = AG.CompareMode.ALWAYS)

	private val tempMat1 = Matrix3D()
	private val tempMat2 = Matrix3D()
	private val tempMat3 = Matrix3D()

	protected open fun prepareExtraModelMatrix(mat: Matrix3D) {
		mat.identity()
	}

	fun AG.UniformValues.setMaterialLight(
		ctx: RenderContext3D,
		uniform: Shaders3D.MaterialLightUniform,
		actual: Material3D.Light
	) {
		when (actual) {
			is Material3D.LightColor -> {
				this[uniform.u_color] = actual.colorVec
			}
			is Material3D.LightTexture -> {
				actual.textureUnit.texture =
					actual.bitmap?.let { ctx.rctx.agBitmapTextureManager.getTextureBase(it).base }
				actual.textureUnit.linear = true
				this[uniform.u_texUnit] = actual.textureUnit
			}
		}
	}

	private val identity = Matrix3D()
	private val identityInv = identity.clone().invert()

	override fun render(ctx: RenderContext3D) {
		val ag = ctx.ag

		ctx.dynamicVertexBufferPool.alloc { vertexBuffer ->
			//vertexBuffer.upload(mesh.data)
			vertexBuffer.upload(mesh.fbuffer)

			//tempMat2.invert()
			//tempMat3.multiply(ctx.cameraMatInv, this.localTransform.matrix)
			//tempMat3.multiply(ctx.cameraMatInv, Matrix3D().invert(this.localTransform.matrix))
			//tempMat3.multiply(this.localTransform.matrix, ctx.cameraMat)

			Shaders3D.apply {
				val meshMaterial = mesh.material
				ag.draw(
					vertexBuffer,
					type = mesh.drawType,
					program = mesh.program ?: ctx.shaders.getProgram3D(
						ctx.lights.size.clamp(0, 4),
						mesh.maxWeights,
						meshMaterial,
						mesh.hasTexture
					),
					vertexLayout = mesh.layout,
					vertexCount = mesh.vertexCount,
					blending = AG.Blending.NONE,
					//vertexCount = 6 * 6,
					uniforms = uniformValues.apply {
						this[u_ProjMat] = ctx.projCameraMat
						this[u_ViewMat] = transform.globalMatrix
						this[u_ModMat] = tempMat2.multiply(tempMat1.apply { prepareExtraModelMatrix(this) }, modelMat)
						//this[u_NormMat] = tempMat3.multiply(tempMat2, localTransform.matrix).invert().transpose()
						this[u_NormMat] = tempMat3.multiply(tempMat2, transform.globalMatrix).invert()

						this[u_Shiness] = meshMaterial?.shiness ?: 0.5f
						this[u_IndexOfRefraction] = meshMaterial?.indexOfRefraction ?: 1f

						if (meshMaterial != null) {
							setMaterialLight(ctx, ambient, meshMaterial.ambient)
							setMaterialLight(ctx, diffuse, meshMaterial.diffuse)
							setMaterialLight(ctx, emission, meshMaterial.emission)
							setMaterialLight(ctx, specular, meshMaterial.specular)
						}

						val skeleton = this@ViewWithMesh3D.skeleton
						val skin = mesh.skin
						this[u_BindShapeMatrix] = identity
						this[u_InvBindShapeMatrix] = identityInv
						//println("skeleton: $skeleton, skin: $skin")
						if (skeleton != null && skin != null) {
							skin.bones.fastForEach { bone ->
								val jointsBySid = skeleton.jointsBySid
								val joint = jointsBySid[bone.name]
								if (joint != null) {
									skin.matrices[bone.index].multiply(
										joint.transform.globalMatrix,
										joint.poseMatrixInv
									)
								} else {
									error("Can't find joint with name '${bone.name}'")
								}

							}
							this[u_BindShapeMatrix] = skin.bindShapeMatrix
							this[u_InvBindShapeMatrix] = skin.invBindShapeMatrix
							this[u_BoneMats] = skin.matrices
						}

						this[u_AmbientColor] = ctx.ambientColor

						ctx.lights.fastForEachWithIndex { index, light: Light3D ->
							val lightColor = light.color
							this[lights[index].u_sourcePos] = light.transform.translation
							this[lights[index].u_color] =
								light.colorVec.setTo(lightColor.rf, lightColor.gf, lightColor.bf, 1f)
							this[lights[index].u_attenuation] = light.attenuationVec.setTo(
								light.constantAttenuation,
								light.linearAttenuation,
								light.quadraticAttenuation
							)
						}
					},
					renderState = rs
				)
			}
		}
	}
}

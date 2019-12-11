package com.soywiz.korge3d

import com.soywiz.kds.iterators.*
import com.soywiz.korma.geom.*

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
fun View3D.removeFromParent() {
	parent?.removeChild(this)
	parent = null
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

package com.soywiz.korge3d

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korge.baseview.BaseView
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.MMatrix3D
import com.soywiz.korma.geom.degrees

@Korge3DExperimental
abstract class View3D : BaseView() {
    //TODO: I don't think that a Camera, Container, Light, ViewWithMesh, Text3D should all have this as supertype
    // they are not all 'types' of View ?

    var active = true
	var id: String? = null
	var name: String? = null
	val transform = Transform3D()

	///////

    override fun invalidateRender() {
        val stage3D = root as? Stage3D?
        //println("View3D.invalidateRender: stage3D=$stage3D")
        stage3D?.views?.views?.invalidatedView(this)
    }

    var x: Double
		set(localX) {
            transform.setTranslation(localX, y, z, localW)
            invalidateRender()
        }
		get() = transform.translation.x.toDouble()

	var y: Double
		set(localY) {
            transform.setTranslation(x, localY, z, localW)
            invalidateRender()
        }
		get() = transform.translation.y.toDouble()

	var z: Double
		set(localZ) { transform.setTranslation(x, y, localZ, localW); invalidateRender() }
		get() = transform.translation.z.toDouble()

	var localW: Double
		set(localW) { transform.setTranslation(x, y, z, localW); invalidateRender() }
		get() = transform.translation.w.toDouble()

	///////

	var scaleX: Double
		set(scaleX) { transform.setScale(scaleX, scaleY, scaleZ, localScaleW); invalidateRender() }
		get() = transform.scale.x.toDouble()

	var scaleY: Double
		set(scaleY) { transform.setScale(scaleX, scaleY, scaleZ, localScaleW); invalidateRender() }
		get() = transform.scale.y.toDouble()

	var scaleZ: Double
		set(scaleZ) { transform.setScale(scaleX, scaleY, scaleZ, localScaleW); invalidateRender() }
		get() = transform.scale.z.toDouble()

	var localScaleW: Double
		set(scaleW) { transform.setScale(scaleX, scaleY, scaleZ, scaleW); invalidateRender() }
		get() = transform.scale.w.toDouble()

	///////

	var rotationX: Angle
		set(rotationX) { transform.setRotation(rotationX, rotationY, rotationZ); invalidateRender() }
		get() = transform.rotationEuler.x

	var rotationY: Angle
		set(rotationY) { transform.setRotation(rotationX, rotationY, rotationZ); invalidateRender() }
		get() = transform.rotationEuler.y

	var rotationZ: Angle
		set(rotationZ) { transform.setRotation(rotationX, rotationY, rotationZ); invalidateRender() }
		get() = transform.rotationEuler.z

	///////

	var rotationQuatX: Double
		set(rotationQuatX) { transform.setRotation(rotationQuatX, rotationQuatY, rotationQuatZ, rotationQuatW); invalidateRender() }
		get() = transform.rotation.x

	var rotationQuatY: Double
		set(rotationQuatY) { transform.setRotation(rotationQuatX, rotationQuatY, rotationQuatZ, rotationQuatW); invalidateRender() }
		get() = transform.rotation.y

	var rotationQuatZ: Double
		set(rotationQuatZ) { transform.setRotation(rotationQuatX, rotationQuatY, rotationQuatZ, rotationQuatW); invalidateRender() }
		get() = transform.rotation.z

	var rotationQuatW: Double
		set(rotationQuatW) { transform.setRotation(rotationQuatX, rotationQuatY, rotationQuatZ, rotationQuatW); invalidateRender() }
		get() = transform.rotation.w

	///////

	internal var _parent: Container3D? = null
    override val baseParent: Container3D? get() = parent

    open val root: View3D get() = parent?.root ?: this

    var parent: Container3D?
		set(value) {
			_parent = value
			_parent?.addChild(this)
            invalidateRender()
		}
		get() = _parent

	val modelMat = MMatrix3D()
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
fun <T : View3D> T.name(name: String): T {
    this.name = name
    return this
}

@Korge3DExperimental
fun <T : View3D> T.position(x: Float, y: Float, z: Float, w: Float = 1f): T {
    transform.setTranslation(x, y, z, w)
    invalidateRender()
    return this
}
@Korge3DExperimental
fun <T : View3D> T.position(x: Double, y: Double, z: Double, w: Double = 1.0): T = position(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
@Korge3DExperimental
fun <T : View3D> T.position(x: Int, y: Int, z: Int, w: Int = 1): T = position(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

@Korge3DExperimental
fun <T : View3D> T.rotation(x: Angle = 0.degrees, y: Angle = 0.degrees, z: Angle = 0.degrees): T {
	transform.setRotation(x, y, z)
    invalidateRender()
    return this
}

@Korge3DExperimental
fun <T : View3D> T.scale(x: Float = 1f, y: Float = 1f, z: Float = 1f, w: Float = 1f): T {
    transform.setScale(x, y, z, w)
    return this
}

@Korge3DExperimental
inline fun <T : View3D> T.scale(x: Double = 1.0, y: Double = 1.0, z: Double = 1.0, w: Double = 1.0): T = scale(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
@Korge3DExperimental
inline fun <T : View3D> T.scale(x: Int = 1, y: Int = 1, z: Int = 1, w: Int = 1): T = scale(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

@Korge3DExperimental
fun <T : View3D> T.lookAt(x: Float, y: Float, z: Float): T {
    transform.lookAt(x, y, z)
    return this
}

@Korge3DExperimental
inline fun <T : View3D> T.lookAt(x: Double, y: Double, z: Double): T = lookAt(x.toFloat(), y.toFloat(), z.toFloat())
@Korge3DExperimental
inline fun <T : View3D> T.lookAt(x: Int, y: Int, z: Int): T = lookAt(x.toFloat(), y.toFloat(), z.toFloat())


@Korge3DExperimental
fun <T : View3D> T.positionLookingAt(px: Float, py: Float, pz: Float, tx: Float, ty: Float, tz: Float): T {
    transform.setTranslationAndLookAt(px, py, pz, tx, ty, tz)
    invalidateRender()
    return this
}
@Korge3DExperimental
fun <T : View3D> T.positionLookingAt(px: Double, py: Double, pz: Double, tx: Double, ty: Double, tz: Double): T = positionLookingAt(px.toFloat(), py.toFloat(), pz.toFloat(), tx.toFloat(), ty.toFloat(), tz.toFloat())
@Korge3DExperimental
fun <T : View3D> T.positionLookingAt(px: Int, py: Int, pz: Int, tx: Int, ty: Int, tz: Int): T = positionLookingAt(px.toFloat(), py.toFloat(), pz.toFloat(), tx.toFloat(), ty.toFloat(), tz.toFloat())

@Korge3DExperimental
fun <T : View3D> T.addTo(container: Container3D): T {
	container.addChild(this)
    return this
}

inline fun <T : View3D> T.addTo(container: Container3D, callback: T.() -> Unit): T = addTo(container).apply(callback)

package com.soywiz.korge3d.experimental

import com.soywiz.korma.geom.*

@Korge3DExperimental
abstract class Camera3D : View3D() {
	private var projMat = Matrix3D()
	private var width: Double = 0.0
	private var height: Double = 0.0
	protected var dirty = true

	protected inline fun dirty(cond: () -> Boolean = { true }, callback: () -> Unit) {
		if (cond()) {
			this.dirty = true
			callback()
		}
	}

	fun getProjMatrix(width: Double, height: Double): Matrix3D {
		if (this.width != width || this.height != height) {
			this.dirty = true
			this.width = width
			this.height = height
		}
		if (dirty) {
			dirty = false
			updateMatrix(projMat, this.width, this.height)
		}
		return projMat
	}

	protected abstract fun updateMatrix(mat: Matrix3D, width: Double, height: Double)

	override fun render(ctx: RenderContext3D) {
		// Do nothing except when debugging
	}

	abstract fun clone(): Camera3D

	class Perspective(
		fov: Angle = 60.degrees,
		near: Double = 0.3,
		far: Double = 1000.0
	) : Camera3D() {
		var fov: Angle = fov; set(value) = dirty({ field != value }) { field = value }
		var near: Double = near; set(value) = dirty({ field != value }) { field = value }
		var far: Double = far; set(value) = dirty({ field != value }) { field = value }

		fun set(fov: Angle = this.fov, near: Double = this.near, far: Double = this.far) = this.apply {
			this.fov = fov
			this.near = near
			this.far = far
		}

		override fun updateMatrix(mat: Matrix3D, width: Double, height: Double) {
			mat.setToPerspective(fov, if (height != 0.0) width / height else 1.0, near, far)
		}

		override fun clone(): Perspective = Perspective(fov, near, far).apply {
			this.transform.copyFrom(this@Perspective.transform)
		}
	}
}

// @TODO: Move to KorMA
private val tempMatrix3D = Matrix3D()

class Transform3D {
	@PublishedApi
	internal var matrixDirty = false
	@PublishedApi
	internal var transformDirty = false

	companion object {
		private val identityMat = Matrix3D()
	}

	val globalMatrixUncached: Matrix3D = Matrix3D()
		get() = run {
			val parent = parent?.globalMatrixUncached ?: identityMat
			field.multiply(parent, matrix)
			field
		}

	val globalMatrix: Matrix3D
		get() = run {
			// @TODO: Cache!
			globalMatrixUncached
		}

	val matrix: Matrix3D = Matrix3D()
		get() = run {
			if (matrixDirty) {
				matrixDirty = false
				field.setTRS(translation, rotation, scale)
			}
			field
		}

	var children: ArrayList<Transform3D> = arrayListOf()

	var parent: Transform3D? = null
		set(value) {
			field?.children?.remove(this)
			field = value
			field?.children?.add(this)
		}

	private val _translation = Position3D(0, 0, 0)
	private val _rotation = Quaternion()
	private val _scale = Scale3D(1, 1, 1)
	@PublishedApi
	internal var _eulerRotationDirty: Boolean = true

	private fun updateTRS() {
		transformDirty = false
		matrix.getTRS(_translation, rotation, _scale)
		_eulerRotationDirty = true
		transformDirty = false
	}

	@PublishedApi
	internal fun updateTRSIfRequired() = this.apply {
		if (transformDirty) updateTRS()
	}

	val translation: Position3D get() = updateTRSIfRequired()._translation
	val rotation: Quaternion get() = updateTRSIfRequired()._rotation
	val scale: Scale3D get() = updateTRSIfRequired()._scale

	var rotationEuler: EulerRotation = EulerRotation()
		private set
		get() {
			if (_eulerRotationDirty) {
				_eulerRotationDirty = false
				field.setQuaternion(rotation)
			}
			return field
		}

	/////////////////
	/////////////////

	fun setMatrix(mat: Matrix3D) = this.apply {
		transformDirty = true
		this.matrix.copyFrom(mat)
	}

	inline fun setTranslation(x: Number, y: Number, z: Number, w: Number = 1f) = updatingTRS {
		updateTRSIfRequired()
		matrixDirty = true
		translation.setTo(x, y, z, w)
	}

	fun setRotation(quat: Quaternion) = updatingTRS {
		updateTRSIfRequired()
		matrixDirty = true
		_eulerRotationDirty = true
		rotation.setTo(quat)
	}

	inline fun setRotation(x: Number, y: Number, z: Number, w: Number) = updatingTRS {
		_eulerRotationDirty = true
		rotation.setTo(x, y, z, w)
	}

	fun setRotation(euler: EulerRotation) = updatingTRS {
		_eulerRotationDirty = true
		rotation.setEuler(euler)
	}

	fun setRotation(x: Angle, y: Angle, z: Angle) = updatingTRS {
		_eulerRotationDirty = true
		rotation.setEuler(x, y, z)
	}

	inline fun setScale(x: Number = 1f, y: Number = 1f, z: Number = 1f, w: Number = 1f) = updatingTRS {
		scale.setTo(x, y, z, w)
	}

	@PublishedApi
	internal inline fun updatingTRS(callback: () -> Unit) = this.apply {
		updateTRSIfRequired()
		matrixDirty = true
		callback()
	}

	/////////////////
	/////////////////

	@PublishedApi
	internal val UP = Vector3D(0f, 1f, 0f)

	@PublishedApi
	internal val tempMat1 = Matrix3D()
	@PublishedApi
	internal val tempMat2 = Matrix3D()
	@PublishedApi
	internal val tempVec1 = Vector3D()
	@PublishedApi
	internal val tempVec2 = Vector3D()

	inline fun lookAt(
		tx: Number, ty: Number, tz: Number,
		up: Vector3D = UP
	) = this.apply {
		tempMat1.setToLookAt(translation, tempVec1.setTo(tx, ty, tz, 1f), up)
		rotation.setFromRotationMatrix(tempMat1)
	}

	inline fun setTranslationAndLookAt(
		px: Number, py: Number, pz: Number,
		tx: Number, ty: Number, tz: Number,
		up: Vector3D = UP
	) = this.apply {
		//setTranslation(px, py, pz)
		//lookUp(tx, ty, tz, up)
		setMatrix(
			matrix.multiply(
				tempMat1.setToTranslation(px, py, pz),
				tempMat2.setToLookAt(tempVec1.setTo(px, py, pz), tempVec2.setTo(tx, ty, tz), up)
			)
		)
	}

	private val tempEuler = EulerRotation()
	fun rotate(x: Angle, y: Angle, z: Angle) = this.apply {
		tempEuler.setQuaternion(this.rotation)
		setRotation(tempEuler.x + x, tempEuler.y + y, tempEuler.z + z)
	}

	fun copyFrom(localTransform: Transform3D) {
		this.setMatrix(localTransform.matrix)
	}

	fun setToInterpolated(a: Transform3D, b: Transform3D, t: Double): Transform3D {
		_translation.setToInterpolated(a.translation, b.translation, t)
		_rotation.setToInterpolated(a.rotation, b.rotation, t)
		_scale.setToInterpolated(a.scale, b.scale, t)
		matrixDirty = true
		return this
	}

	override fun toString(): String = "Transform3D(translation=$translation,rotation=$rotation,scale=$scale)"
	fun clone(): Transform3D = Transform3D().setMatrix(this.matrix)
}

typealias PerspectiveCamera3D = Camera3D.Perspective

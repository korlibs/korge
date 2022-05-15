package com.soywiz.korge3d

import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.EulerRotation
import com.soywiz.korma.geom.Matrix3D
import com.soywiz.korma.geom.Position3D
import com.soywiz.korma.geom.Quaternion
import com.soywiz.korma.geom.Scale3D
import com.soywiz.korma.geom.Vector3D
import com.soywiz.korma.geom.getTRS
import com.soywiz.korma.geom.plus
import com.soywiz.korma.geom.setTRS
import com.soywiz.korma.geom.setToLookAt

class Transform3D {
    @PublishedApi
    internal var matrixDirty = false
    @PublishedApi
    internal var transformDirty = false

    companion object {
        private val identityMat = Matrix3D()
    }

    val globalMatrixUncached: Matrix3D = Matrix3D()
        get() {
            val parent = parent?.globalMatrixUncached ?: identityMat
            field.multiply(parent, matrix)
            return field
        }

    val globalMatrix: Matrix3D
        get() = globalMatrixUncached // @TODO: Cache!

    val matrix: Matrix3D = Matrix3D()
        get() {
            if (matrixDirty) {
                matrixDirty = false
                field.setTRS(translation, rotation, scale)
            }
            return field
        }

    var children: ArrayList<Transform3D> = arrayListOf()

    var parent: Transform3D? = null
        set(value) {
            field?.children?.remove(this)
            field = value
            field?.children?.add(this)
        }

    private val _translation = Vector3D(0, 0, 0)
    private val _rotation = Quaternion()
    private val _scale = Vector3D(1, 1, 1)
    @PublishedApi
    internal var _eulerRotationDirty: Boolean = true

    private fun updateTRS() {
        transformDirty = false
        matrix.getTRS(_translation, rotation, _scale)
        _eulerRotationDirty = true
        transformDirty = false
    }

    @PublishedApi
    internal fun updateTRSIfRequired(): Transform3D {
        if (transformDirty) updateTRS()
        return this
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

    fun setMatrix(mat: Matrix3D): Transform3D {
        transformDirty = true
        this.matrix.copyFrom(mat)
        return this
    }

    fun setTranslation(x: Float, y: Float, z: Float, w: Float = 1f) = updatingTRS {
        updateTRSIfRequired()
        matrixDirty = true
        translation.setTo(x, y, z, w)
    }
    fun setTranslation(x: Double, y: Double, z: Double, w: Double = 1.0) = setTranslation(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
    fun setTranslation(x: Int, y: Int, z: Int, w: Int = 1) = setTranslation(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

    fun setRotation(quat: Quaternion) = updatingTRS {
        updateTRSIfRequired()
        matrixDirty = true
        _eulerRotationDirty = true
        rotation.setTo(quat)
    }

    fun setRotation(x: Float, y: Float, z: Float, w: Float) = updatingTRS {
        _eulerRotationDirty = true
        rotation.setTo(x, y, z, w)
    }

    fun setRotation(x: Double, y: Double, z: Double, w: Double) = setRotation(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
    fun setRotation(x: Int, y: Int, z: Int, w: Int) = setRotation(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

    fun setRotation(euler: EulerRotation) = updatingTRS {
        _eulerRotationDirty = true
        rotation.setEuler(euler)
    }

    fun setRotation(x: Angle, y: Angle, z: Angle) = updatingTRS {
        _eulerRotationDirty = true
        rotation.setEuler(x, y, z)
    }

    fun setScale(x: Float = 1f, y: Float = 1f, z: Float = 1f, w: Float = 1f) = updatingTRS {
        scale.setTo(x, y, z, w)
    }
    fun setScale(x: Double, y: Double, z: Double, w: Double) = setScale(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
    fun setScale(x: Int, y: Int, z: Int, w: Int) = setScale(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())

    @PublishedApi
    internal inline fun updatingTRS(callback: () -> Unit): Transform3D {
        updateTRSIfRequired()
        matrixDirty = true
        callback()
        return this
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

    fun lookAt(tx: Float, ty: Float, tz: Float, up: Vector3D = UP): Transform3D {
        tempMat1.setToLookAt(translation, tempVec1.setTo(tx, ty, tz, 1f), up)
        rotation.setFromRotationMatrix(tempMat1)
        return this
    }
    fun lookAt(tx: Double, ty: Double, tz: Double, up: Vector3D = UP) = lookAt(tx.toFloat(), ty.toFloat(), tz.toFloat(), up)
    fun lookAt(tx: Int, ty: Int, tz: Int, up: Vector3D = UP) = lookAt(tx.toFloat(), ty.toFloat(), tz.toFloat(), up)

    //setTranslation(px, py, pz)
    //lookUp(tx, ty, tz, up)
    fun setTranslationAndLookAt(
        px: Float, py: Float, pz: Float,
        tx: Float, ty: Float, tz: Float,
        up: Vector3D = UP
    ): Transform3D = setMatrix(
        matrix.multiply(
            tempMat1.setToTranslation(px, py, pz),
            tempMat2.setToLookAt(tempVec1.setTo(px, py, pz), tempVec2.setTo(tx, ty, tz), up)
        )
    )
    fun setTranslationAndLookAt(
        px: Double, py: Double, pz: Double,
        tx: Double, ty: Double, tz: Double,
        up: Vector3D = UP
    ) = setTranslationAndLookAt(px.toFloat(), py.toFloat(), pz.toFloat(), tx.toFloat(), ty.toFloat(), tz.toFloat(), up)

    private val tempEuler = EulerRotation()
    fun rotate(x: Angle, y: Angle, z: Angle): Transform3D {
        val re = this.rotationEuler
        tempEuler.setTo(re.x+x,re.y+y, re.z+z)
        setRotation(tempEuler)
        return this
    }

    fun translate(vec:Vector3D) : Transform3D {
        this.setTranslation( this.translation.x + vec.x, this.translation.y + vec.y, this.translation.z+vec.z )
        return this
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

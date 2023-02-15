package com.soywiz.korge3d

import com.soywiz.korma.geom.*

class Transform3D {
    @PublishedApi
    internal var matrixDirty = false
    @PublishedApi
    internal var transformDirty = false

    companion object {
        private val identityMat = MMatrix3D()
    }

    val globalMatrixUncached: MMatrix3D = MMatrix3D()
        get() {
            val parent = parent?.globalMatrixUncached ?: identityMat
            field.multiply(parent, matrix)
            return field
        }

    val globalMatrix: MMatrix3D
        get() = globalMatrixUncached // @TODO: Cache!

    val matrix: MMatrix3D = MMatrix3D()
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

    private val _translation = MVector4(0, 0, 0)
    private val _rotation = MQuaternion()
    private val _scale = MVector4(1, 1, 1)
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
    val rotation: MQuaternion get() = updateTRSIfRequired()._rotation
    val scale: Scale3D get() = updateTRSIfRequired()._scale

    var rotationEuler: MEulerRotation = MEulerRotation()
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

    fun setMatrix(mat: MMatrix3D): Transform3D {
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

    fun setRotation(quat: MQuaternion) = updatingTRS {
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

    fun setRotation(euler: MEulerRotation) = updatingTRS {
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
    internal val UP = MVector4(0f, 1f, 0f)

    @PublishedApi
    internal val tempMat1 = MMatrix3D()
    @PublishedApi
    internal val tempMat2 = MMatrix3D()
    @PublishedApi
    internal val tempVec1 = MVector4()
    @PublishedApi
    internal val tempVec2 = MVector4()

    fun lookAt(tx: Float, ty: Float, tz: Float, up: MVector4 = UP): Transform3D {
        tempMat1.setToLookAt(translation, tempVec1.setTo(tx, ty, tz, 1f), up)
        rotation.setFromRotationMatrix(tempMat1)
        return this
    }
    fun lookAt(tx: Double, ty: Double, tz: Double, up: MVector4 = UP) = lookAt(tx.toFloat(), ty.toFloat(), tz.toFloat(), up)
    fun lookAt(tx: Int, ty: Int, tz: Int, up: MVector4 = UP) = lookAt(tx.toFloat(), ty.toFloat(), tz.toFloat(), up)

    //setTranslation(px, py, pz)
    //lookUp(tx, ty, tz, up)
    fun setTranslationAndLookAt(
        px: Float, py: Float, pz: Float,
        tx: Float, ty: Float, tz: Float,
        up: MVector4 = UP
    ): Transform3D = setMatrix(
        matrix.multiply(
            tempMat1.setToTranslation(px, py, pz),
            tempMat2.setToLookAt(tempVec1.setTo(px, py, pz), tempVec2.setTo(tx, ty, tz), up)
        )
    )
    fun setTranslationAndLookAt(
        px: Double, py: Double, pz: Double,
        tx: Double, ty: Double, tz: Double,
        up: MVector4 = UP
    ) = setTranslationAndLookAt(px.toFloat(), py.toFloat(), pz.toFloat(), tx.toFloat(), ty.toFloat(), tz.toFloat(), up)

    private val tempEuler = MEulerRotation()
    fun rotate(x: Angle, y: Angle, z: Angle): Transform3D {
        val re = this.rotationEuler
        tempEuler.setTo(re.x+x,re.y+y, re.z+z)
        setRotation(tempEuler)
        return this
    }

    fun translate(vec:MVector4) : Transform3D {
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

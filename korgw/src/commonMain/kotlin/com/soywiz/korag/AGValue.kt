package com.soywiz.korag

import com.soywiz.kds.lock.*
import com.soywiz.kmem.*
import com.soywiz.korag.shader.*
import com.soywiz.korim.color.*
import com.soywiz.korma.geom.*
import kotlin.math.*

class AGUniformValue(val uniform: Uniform) : AGValue(uniform)

open class AGValue(val type: VarType, val arrayCount: Int) {
    constructor(variable: Variable) : this(variable.type, variable.arrayCount)

    val kind: VarKind = type.kind
    val stride: Int = type.elementCount
    val totalElements: Int = stride * arrayCount
    val totalBytes: Int = type.bytesSize * arrayCount

    //val data: Buffer = Buffer(type.bytesSize * arrayCount * 4)
    val data: Buffer = Buffer(type.elementCount * arrayCount * 4)
    val i32: Int32Buffer = data.i32
    val f32: Float32Buffer = data.f32

    //fun set(data: Buffer) {
    //    arraycopy(data, 0, this.data, 0, min(totalBytes, data.size))
    //}

    @Deprecated("")
    fun set(value: Any?) {
        when (value) {
            null -> set(Unit)
            is AGTextureUnit -> set(value)
            is Unit -> set(Unit)
            is Boolean -> set(value)
            is BooleanArray -> set(value)
            is Int -> set(value)
            is IntArray -> set(value)
            is Float -> set(value)
            is FloatArray -> set(value)
            is Double -> set(value.toFloat())
            is Matrix3D -> set(value)
            is IPoint -> set(value)
            is Vector3D -> set(value)
            is RGBAf -> set(value)
            is RGBA -> set(value)
            is RGBAPremultiplied -> set(value)
            is Array<*> -> {
                if (value.size > 0) {
                    val item = value[0]
                    when (item) {
                        is Vector3D -> set(value as Array<Vector3D>)
                        is Matrix3D -> set(value as Array<Matrix3D>)
                        is FloatArray -> set(value as Array<FloatArray>)
                        else -> TODO()
                    }
                }
            }
            else -> TODO("$value : ${value::class}")
        }
    }
    /*
                arrayCount = min(declArrayCount, value.size)
                for (n in 0 until value.size) {
                    val vector = value[n] as Vector3D
                    tempBuffer.setArrayFloat32(n * stride, vector.data, 0, stride)
                }

                set(value as Array<Matrix3D>)

     */

    fun set(value: Unit) {
    }

    fun set(value: Boolean) {
        set(value.toInt())
    }

    fun set(value: AGTextureUnit) {
        set(value.index)
    }

    fun set(value: Int) {
        when (kind) {
            //VarKind.TBOOL, VarKind.TBYTE, VarKind.TUNSIGNED_BYTE -> data.setUInt8(0, value)
            //VarKind.TSHORT, VarKind.TUNSIGNED_SHORT -> data.setInt16(0, value.toShort())
            //VarKind.TINT -> data.setInt32(0, value)
            //VarKind.TFLOAT -> data.setFloat32(0, value.toFloat())
            VarKind.TFLOAT -> f32[0] = value.toFloat()
            else -> i32[0] = value
        }
    }

    fun set(value: Float) {
        when (kind) {
            VarKind.TFLOAT -> data.setFloat32(0, value)
            else -> set(value.toInt())
        }
    }

    fun set(value: BooleanArray, offset: Int = 0, size: Int = value.size - offset) {
        tempIntsLock {
            for (n in 0 until size) tempInts[n] = value[offset + n].toInt()
            set(tempInts, 0, size)
        }
    }

    fun set(value: IntArray, offset: Int = 0, size: Int = value.size - offset) {
        when (kind) {
            //VarKind.TBOOL, VarKind.TBYTE, VarKind.TUNSIGNED_BYTE -> for (n in 0 until size) data.setUInt8(n, value[offset + n])
            //VarKind.TSHORT, VarKind.TUNSIGNED_SHORT -> for (n in 0 until size) data.setUInt16(n, value[offset + n])
            //VarKind.TINT -> for (n in 0 until size) data.setInt32(n, value[offset + n])
            VarKind.TFLOAT -> for (n in 0 until size) data.setFloat32(n, value[offset + n].toFloat())
            else -> data.setArrayInt32(0, value, offset, size)
        }
    }

    fun set(value: FloatArray, offset: Int = 0, size: Int = value.size - offset) {
        when (kind) {
            //VarKind.TBOOL, VarKind.TBYTE, VarKind.TUNSIGNED_BYTE -> for (n in 0 until size) data.setUInt8(n, value[offset + n].toInt())
            //VarKind.TSHORT, VarKind.TUNSIGNED_SHORT -> for (n in 0 until size) data.setUInt16(n, value[offset + n].toInt())
            //VarKind.TINT -> for (n in 0 until size) data.setInt32(n, value[offset + n].toInt())
            VarKind.TFLOAT -> data.setArrayFloat32(0, value, offset, size)
            else -> for (n in 0 until size) i32[n] = value[offset + n].toInt()
        }
    }

    fun set(value: Array<FloatArray>) {
        for (n in 0 until min(value.size, arrayCount)) {
            val data = value[n]
            f32.setArray(n * stride, data, 0, min(stride, data.size))
        }
    }

    fun set(v0: Float, v1: Float) {
        tempFloatsLock {
            tempFloats[0] = v0
            tempFloats[1] = v1
            set(tempFloats, 0, 2)
        }
    }

    fun set(v0: Float, v1: Float, v2: Float) {
        tempFloatsLock {
            tempFloats[0] = v0
            tempFloats[1] = v1
            tempFloats[2] = v2
            set(tempFloats, 0, 3)
        }
    }

    fun set(v0: Float, v1: Float, v2: Float, v3: Float) {
        tempFloatsLock {
            tempFloats[0] = v0
            tempFloats[1] = v1
            tempFloats[2] = v2
            tempFloats[3] = v3
            set(tempFloats, 0, 4)
        }
    }

    fun set(value: Vector3D) {
        set(value.data, 0, stride)
    }

    fun set(value: RGBAf) {
        set(value.data, 0, 4)
    }

    fun set(value: IPoint) {
        set(value.x.toFloat(), value.y.toFloat())
    }

    fun set(value: Margin) {
        set(value.top.toFloat(), value.right.toFloat(), value.bottom.toFloat(), value.left.toFloat())
    }

    fun set(value: RectCorners) {
        set(value.topLeft.toFloat(), value.topRight.toFloat(), value.bottomRight.toFloat(), value.bottomLeft.toFloat())
    }

    fun set(value: RGBA) {
        set(value.rf, value.gf, value.bf, value.af)
    }

    fun set(value: RGBAPremultiplied) {
        set(value.rf, value.gf, value.bf, value.af)
    }

    fun set(mat: Matrix3D) {
        tempMatrixLock {
            tempMatrix[0] = mat
            set(tempMatrix)
        }
    }

    fun set(matArray: Array<Matrix3D>) {
        val arrayCount = min(arrayCount, matArray.size)
        val matSize = when (type) {
            VarType.Mat2 -> 2; VarType.Mat3 -> 3; VarType.Mat4 -> 4; else -> -1
        }
        tempFloatsLock {
            for (n in 0 until arrayCount) {
                matArray[n].copyToFloatWxH(tempFloats, matSize, matSize, MajorOrder.COLUMN, n * stride)
            }
            data.setArrayFloat32(0, tempFloats, 0, stride * arrayCount)
        }
    }

    fun set(vectors: Array<Vector3D>) {
        for (n in 0 until min(vectors.size, arrayCount)) {
            val data = vectors[n].data
            f32.setArray(n * stride, data, 0, min(data.size, stride))
        }
    }

    companion object {
        private val tempIntsLock = NonRecursiveLock()
        private val tempInts = IntArray(64 * (4 * 4))
        private val tempFloatsLock = NonRecursiveLock()
        private val tempFloats = FloatArray(64 * (4 * 4))
        private val tempMatrixLock = NonRecursiveLock()
        private val tempMatrix = Array<Matrix3D>(1) { Matrix3D() }
    }
}

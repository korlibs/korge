package com.soywiz.korag

import com.soywiz.kds.*
import com.soywiz.kds.lock.*
import com.soywiz.kmem.*
import com.soywiz.korag.shader.*
import com.soywiz.korim.color.*
import com.soywiz.korio.lang.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.min
import kotlinx.coroutines.internal.*
import kotlin.math.*

class AGValue(val type: VarType, val arrayCount: Int) {
    val kind: VarKind = type.kind
    val stride: Int = type.elementCount
    val totalElements: Int = stride * arrayCount
    val totalBytes: Int = type.bytesSize * arrayCount

    val data: Buffer = Buffer(type.bytesSize * arrayCount)

    fun set(data: Buffer) {
        arraycopy(data, 0, this.data, 0, min(totalBytes, data.size))
    }

    fun set(value: Boolean) {
        set(value.toInt())
    }

    fun set(value: Int) {
        when (kind) {
            VarKind.TBOOL, VarKind.TBYTE, VarKind.TUNSIGNED_BYTE -> data.setUInt8(0, value)
            VarKind.TSHORT, VarKind.TUNSIGNED_SHORT -> data.setInt16(0, value.toShort())
            VarKind.TINT -> data.setInt32(0, value)
            VarKind.TFLOAT -> data.setFloat32(0, value.toFloat())
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
            VarKind.TBOOL, VarKind.TBYTE, VarKind.TUNSIGNED_BYTE -> for (n in 0 until size) data.setUInt8(n, value[offset + n])
            VarKind.TSHORT, VarKind.TUNSIGNED_SHORT -> for (n in 0 until size) data.setUInt16(n, value[offset + n])
            VarKind.TINT -> for (n in 0 until size) data.setInt32(n, value[offset + n])
            VarKind.TFLOAT -> for (n in 0 until size) data.setFloat32(n, value[offset + n].toFloat())
        }
    }

    fun set(value: FloatArray, offset: Int = 0, size: Int = value.size - offset) {
        when (kind) {
            VarKind.TBOOL, VarKind.TBYTE, VarKind.TUNSIGNED_BYTE -> for (n in 0 until size) data.setUInt8(n, value[offset + n].toInt())
            VarKind.TSHORT, VarKind.TUNSIGNED_SHORT -> for (n in 0 until size) data.setUInt16(n, value[offset + n].toInt())
            VarKind.TINT -> for (n in 0 until size) data.setInt32(n, value[offset + n].toInt())
            VarKind.TFLOAT -> data.setArrayFloat32(0, value, offset, min(size, stride))
        }
    }

    fun set(value: Array<FloatArray>) {
        tempFloatsLock {
            for (n in value.indices) arraycopy(value[n], 0, tempFloats, n * stride, value[n].size)
            set(tempFloats, 0, totalElements)
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

    fun set(value: Point) {
        set(value.xf, value.yf)
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

    companion object {
        private val tempIntsLock = NonRecursiveLock()
        private val tempInts = IntArray(64 * (4 * 4))
        private val tempFloatsLock = NonRecursiveLock()
        private val tempFloats = FloatArray(64 * (4 * 4))
        private val tempMatrixLock = NonRecursiveLock()
        private val tempMatrix = Array<Matrix3D>(1) { Matrix3D() }
    }
}

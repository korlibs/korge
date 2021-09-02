package com.soywiz.korlibs.tensork.backend

import com.soywiz.korlibs.tensork.*

open class ShapedTensorBuffer(val buffer: TensorBuffer, val shape: TensorShape) {
    val backend get() = buffer.backend

    private fun toStringPart(sb: StringBuilder, data: FloatArray, vararg pos: Int): StringBuilder {
        val nextSize = shape.dims[pos.size]
        val isLeaf = pos.size >= shape.dims.size - 1
        sb.append('[')
        for (n in 0 until nextSize) {
            if (n != 0) sb.append(',')
            if (isLeaf) {
                sb.append(data[shape.index(n, *pos)])
            } else {
                toStringPart(sb, data, n, *pos)
            }
        }
        sb.append(']')
        return sb
    }

    override fun toString(): String {
        return toStringPart(StringBuilder(), buffer.readSync(false)).toString()
    }
}

interface TensorBuffer {
    val backend: TensorBackend
    fun set(shape: TensorShape, vararg pos: Int, value: Float)
    fun get(shape: TensorShape, vararg pos: Int): Float
    fun clone(): TensorBuffer
    fun readSync(copy: Boolean = true): FloatArray
    suspend fun read(copy: Boolean = true): FloatArray = readSync(copy)
}

abstract class TensorBackend {
    companion object {
        //var Default = CpuTensorBackend
    }
    abstract fun createBuffer(data: FloatArray) : TensorBuffer
    abstract fun executeOp(name: TensorOp, vararg tensors: ShapedTensorBuffer): ShapedTensorBuffer
    fun createTensor(data: FloatArray, shape: TensorShape) = ShapedTensorBuffer(createBuffer(data), shape)
    fun createTensorZero(shape: TensorShape) = createTensor(FloatArray(shape.elementCount), shape)
}

val TensorBackend.Companion.Default get() = CpuTensorBackend

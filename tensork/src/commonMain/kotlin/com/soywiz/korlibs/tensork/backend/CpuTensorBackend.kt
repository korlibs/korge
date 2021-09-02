package com.soywiz.korlibs.tensork.backend

import com.soywiz.korlibs.tensork.*
import kotlin.math.*

object CpuTensorBackend : TensorBackend() {
    class CpuBuffer(val values: FloatArray) : TensorBuffer {
        override val backend get() = CpuTensorBackend
        override fun set(shape: TensorShape, vararg pos: Int, value: Float) { values[shape.index(*pos)] = value }
        override fun get(shape: TensorShape, vararg pos: Int): Float = values[shape.index(*pos)]
        override fun clone(): TensorBuffer = CpuBuffer(values.copyOf())
        override fun readSync(copy: Boolean): FloatArray = if (copy) values.copyOf() else values
    }

    override fun createBuffer(data: FloatArray): TensorBuffer = CpuBuffer(data)

    open class CpuOperation(val op: TensorOp, val func: Array<out ShapedTensorBuffer>.() -> ShapedTensorBuffer)

    val ops = listOf(
        // Arithmetic
        CpuOperation(TensorOp.relu) { unary1DFunc { kotlin.math.max(0f, it) } },
        CpuOperation(TensorOp.abs) { unary1DFunc { it.absoluteValue } },
        CpuOperation(TensorOp.neg) { unary1DFunc { -it } },
        CpuOperation(TensorOp.cos) { unary1DFunc { cos(it) } },
        CpuOperation(TensorOp.sin) { unary1DFunc { sin(it) } },
        CpuOperation(TensorOp.acos) { unary1DFunc { acos(it) } },
        CpuOperation(TensorOp.asin) { unary1DFunc { asin(it) } },
        CpuOperation(TensorOp.max) { binary1DFunc { l, r -> max(l, r) } },
        CpuOperation(TensorOp.min) { binary1DFunc { l, r -> min(l, r) } },
        CpuOperation(TensorOp.add) { binary1DFunc { l, r -> l + r } },
        CpuOperation(TensorOp.sub) { binary1DFunc { l, r -> l - r } },
        CpuOperation(TensorOp.mul) { binary1DFunc { l, r -> l * r } },
        CpuOperation(TensorOp.div) { binary1DFunc { l, r -> l / r } },
        CpuOperation(TensorOp.divNoNan) { binary1DFunc { l, r -> if (r == 0f) 0f else l / r } },
        CpuOperation(TensorOp.floorDiv) { binary1DFunc { l, r -> floor(l / r) } },
        CpuOperation(TensorOp.mod) { binary1DFunc { l, r -> l % r } },
        CpuOperation(TensorOp.pow) { binary1DFunc { l, r -> l.pow(r) } },
        CpuOperation(TensorOp.squareDifference) { binary1DFunc { l, r -> (l - r) * (r - l) } },
        CpuOperation(TensorOp.acosh) { unary1DFunc { acosh(it) } },
        CpuOperation(TensorOp.asinh) { unary1DFunc { asinh(it) } },
        CpuOperation(TensorOp.softplus) { unary1DFunc { ln(exp(it + 1f)) } },
        CpuOperation(TensorOp.sinh) { unary1DFunc { sinh(it) } },
        CpuOperation(TensorOp.sqrt) { unary1DFunc { sqrt(it) } },
        CpuOperation(TensorOp.square) { unary1DFunc { it * it } },
        CpuOperation(TensorOp.step) { binary1DFunc { x, alpha -> if (x > 0f) 1f else alpha } },
        CpuOperation(TensorOp.tan) { unary1DFunc { tan(it) } },
        CpuOperation(TensorOp.tanh) { unary1DFunc { tanh(it) } },
        CpuOperation(TensorOp.atan) { unary1DFunc { atan(it) } },
        CpuOperation(TensorOp.atan2) { binary1DFunc { x, y -> atan2(x, y) } },
        CpuOperation(TensorOp.atanh) { unary1DFunc { atanh(it) } },
        CpuOperation(TensorOp.ceil) { unary1DFunc { ceil(it) } },
        CpuOperation(TensorOp.clipByValue) { ternary1DFunc { x, min, max -> x.coerceIn(min, max) } },
        CpuOperation(TensorOp.cosh) { unary1DFunc { cosh(it) } },
        CpuOperation(TensorOp.elu) { unary1DFunc { if (it > 0f) it else exp(it) - 1f } },
        CpuOperation(TensorOp.erf) { TODO() },
        CpuOperation(TensorOp.exp) { unary1DFunc { exp(it) } },
        CpuOperation(TensorOp.expm1) { unary1DFunc { exp(it) - 1f } },
        CpuOperation(TensorOp.floor) { unary1DFunc { floor(it) } },
        CpuOperation(TensorOp.isFinite) { unary1DFunc { if (it.isFinite()) 1f else 0f } },
        CpuOperation(TensorOp.isNaN) { unary1DFunc { if (it.isNaN()) 1f else 0f } },
        CpuOperation(TensorOp.leakyRelu) { TODO() },
        CpuOperation(TensorOp.log) { unary1DFunc { ln(it) } },
        CpuOperation(TensorOp.log1p) { unary1DFunc { ln(1f + it) } },
        CpuOperation(TensorOp.logSigmoid) { TODO() },
        CpuOperation(TensorOp.prelu) { TODO() } , //tensors.binary1DFunc {x, alpha -> if (x < 0f) }
        CpuOperation(TensorOp.reciprocal) { TODO() },
        CpuOperation(TensorOp.relu6) { TODO() },
        CpuOperation(TensorOp.round) { unary1DFunc { round(it) } },
        CpuOperation(TensorOp.rsqrt) { unary1DFunc { 1f / sqrt(it) } },
        CpuOperation(TensorOp.selu) { TODO() }, //tensors.unary1DFunc { if (it < 0f) scale * alpha * (exp(it) - 1f) else it }
        CpuOperation(TensorOp.sigmoid) { unary1DFunc { 1f / (1f + exp(-it)) } },
        CpuOperation(TensorOp.sign) { unary1DFunc { it.sign } },
        // Matrix,
        CpuOperation(TensorOp.dot) { TODO() },
        CpuOperation(TensorOp.matMul) { TODO() },
        CpuOperation(TensorOp.norm) { TODO() },
        CpuOperation(TensorOp.outerProd) { TODO() },
        CpuOperation(TensorOp.transpose) { TODO() },
    ).associate { it.op to it.func }


    override fun executeOp(name: TensorOp, vararg tensors: ShapedTensorBuffer): ShapedTensorBuffer {
        val func = ops[name] ?: error("Unknown operation '$name' in backend $this")
        return func(tensors)
    }

    val ShapedTensorBuffer.cpuBuffer get() = buffer as CpuBuffer

    inline fun Array<out ShapedTensorBuffer>.unary1DFunc(block: (Float) -> Float): ShapedTensorBuffer =
        unary1DFunc(this[0], block)

    inline fun Array<out ShapedTensorBuffer>.binary1DFunc(block: (Float, Float) -> Float): ShapedTensorBuffer =
        binary1DFunc(this[0], this[1], block)

    inline fun Array<out ShapedTensorBuffer>.ternary1DFunc(block: (Float, Float, Float) -> Float): ShapedTensorBuffer =
        ternary1DFunc(this[0], this[1], this[2], block)

    inline fun unary1DFunc(t1: ShapedTensorBuffer, block: (Float) -> Float): ShapedTensorBuffer {
        val out = createTensorZero(t1.shape)
        val vo = out.cpuBuffer.values
        val v1 = t1.cpuBuffer.values
        for (n in vo.indices) vo[n] = block(v1[n])
        return out
    }
    inline fun binary1DFunc(t1: ShapedTensorBuffer, t2: ShapedTensorBuffer, block: (Float, Float) -> Float): ShapedTensorBuffer {
        val out = createTensorZero(max(t1.shape, t2.shape))
        val vo = out.cpuBuffer.values
        val v1 = t1.cpuBuffer.values
        val v2 = t2.cpuBuffer.values
        when {
            v1.size == vo.size && v2.size == 1 -> {
                val vv2 = v2[0]
                for (n in vo.indices) vo[n] = block(v1[n], vv2)
            }
            v1.size == vo.size && v2.size == vo.size -> for (n in vo.indices) vo[n] = block(v1[n], v2[n])
            else -> for (n in vo.indices) vo[n] = block(v1[n % v1.size], v2[n % v2.size])
        }
        return out
    }
    inline fun ternary1DFunc(t1: ShapedTensorBuffer, t2: ShapedTensorBuffer, t3: ShapedTensorBuffer, block: (Float, Float, Float) -> Float): ShapedTensorBuffer {
        val out = createTensorZero(max(max(t1.shape, t2.shape), t3.shape))
        val vo = out.cpuBuffer.values
        val v1 = t1.cpuBuffer.values
        val v2 = t2.cpuBuffer.values
        val v3 = t3.cpuBuffer.values
        when {
            vo.size == v1.size && vo.size == v2.size -> for (n in vo.indices) vo[n] = block(v1[n], v2[n], v3[n])
            else -> for (n in vo.indices) vo[n] = block(v1[n % v1.size], v2[n % v2.size], v3[n % v3.size])
        }
        return out
    }
}

fun <T : Comparable<T>> min(a: T, b: T) = if (a < b) a else b
fun <T : Comparable<T>> max(a: T, b: T) = if (a > b) a else b

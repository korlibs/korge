package com.soywiz.korlibs.tensork

import com.soywiz.korlibs.tensork.backend.*
import com.soywiz.korlibs.tensork.graph.*

class Tensor internal constructor(private val graph: TensorGraph.Node, private val backend: TensorBackend) {
    val shape: TensorShape get() = graph.shape

    companion object {
        operator fun invoke(vararg values: Float, shape: TensorShape = TensorShape(values.size), backend: TensorBackend = TensorBackend.Default) =
            Tensor(TensorGraph.Terminal(backend.createTensor(values, shape)), backend)

        fun zero(shape: TensorShape) = Tensor(*FloatArray(shape.elementCount), shape = shape)
        fun scalar(value: Float) = Tensor(value, shape = TensorShape(1))
        fun complex(real: Tensor, imag: Tensor) {
            check(real.shape == imag.shape)
            TODO()
        }

        fun add(vararg tensors: Tensor): Tensor {
            var out = tensors[0]
            for (n in 1 until tensors.size) out += tensors[n]
            return out
        }
        fun add(a: Tensor, b: Tensor) = a.add(b)
        fun sub(a: Tensor, b: Tensor) = a.sub(b)
        fun mul(a: Tensor, b: Tensor) = a.mul(b)
    }

    private fun Tensor(graph: TensorGraph.Node) = Tensor(graph, backend)
    private fun Tensor(vararg values: Float) = Tensor(*values, backend = backend)

    val computed: ShapedTensorBuffer by lazy { graph.compute(backend) }

    fun reshape(shape: TensorShape) = Tensor(TensorGraph.Reshape(graph, shape))
    fun as2D(rows: Int, columns: Int) = reshape(TensorShape(rows, columns))

    fun toTypedArray() = computed.buffer.readSync()
    fun toBackend(backend: TensorBackend) = Tensor(graph, backend)
    fun toCpuBackend() = toBackend(CpuTensorBackend)

    //operator fun set(vararg pos: Int, value: Float) = buffer.set(shape, *pos, value = value)
    //operator fun get(vararg pos: Int): Float = buffer.get(shape, *pos)

    fun unop(op: TensorOp) = Tensor(TensorGraph.Unop(op, graph), backend)
    fun binop(op: TensorOp, right: Tensor) = Tensor(TensorGraph.Binop(op, graph, right.graph), backend)
    fun multiop(op: TensorOp, vararg other: Tensor) = Tensor(TensorGraph.Multiop(op, listOf(graph) + other.map { it.graph }), backend)

    // Arithmetic
    fun add(other: Tensor) = binop(TensorOp.add, other)
    fun sub(other: Tensor) = binop(TensorOp.sub, other)
    fun mul(other: Tensor) = binop(TensorOp.mul, other)
    private fun _div(other: Tensor) = binop(TensorOp.div, other)
    fun divNoNan(other: Tensor) = binop(TensorOp.divNoNan, other)
    fun floorDiv(other: Tensor) = binop(TensorOp.floorDiv, other)
    fun max(other: Tensor) = binop(TensorOp.max, other)
    fun min(other: Tensor) = binop(TensorOp.min, other)
    fun mod(other: Tensor) = binop(TensorOp.mod, other)
    infix fun pow(other: Tensor) = binop(TensorOp.pow, other)
    infix fun squareDifference(other: Tensor) = binop(TensorOp.squareDifference, other)

    // Basic math
    fun abs() = unop(TensorOp.abs)
    fun acos() = unop(TensorOp.acos)
    fun acosh() = unop(TensorOp.acosh)
    fun asin() = unop(TensorOp.asin)
    fun asinh() = unop(TensorOp.asinh)
    fun atan() = unop(TensorOp.atan)
    fun atan2(other: Tensor) = binop(TensorOp.atan2, other)
    fun atanh() = unop(TensorOp.atanh)
    fun ceil() = unop(TensorOp.ceil)
    fun clipByValue(min: Float, max: Float) = binop(TensorOp.clipByValue, Tensor(min, max))
    fun cos() = unop(TensorOp.cos)
    fun cosh() = unop(TensorOp.cosh)
    fun elu() = unop(TensorOp.elu)
    fun erf() = unop(TensorOp.erf)
    fun exp() = unop(TensorOp.exp)
    fun expm1() = unop(TensorOp.expm1)
    fun floor() = unop(TensorOp.floor)
    fun isFinite() = unop(TensorOp.isFinite)
    fun isNaN() = unop(TensorOp.isNaN)
    fun leakyRelu() = unop(TensorOp.leakyRelu)
    fun log() = unop(TensorOp.log)
    fun log1p() = unop(TensorOp.log1p)
    fun logSigmoid() = unop(TensorOp.logSigmoid)
    fun neg() = unop(TensorOp.neg)
    fun prelu() = unop(TensorOp.prelu)
    fun reciprocal() = unop(TensorOp.reciprocal)
    fun relu() = unop(TensorOp.relu)
    fun relu6() = unop(TensorOp.relu6)
    fun round() = unop(TensorOp.round)
    fun rsqrt() = unop(TensorOp.rsqrt)
    fun selu() = unop(TensorOp.selu)
    fun sigmoid() = unop(TensorOp.sigmoid)
    fun sign() = unop(TensorOp.sign)
    fun sin() = unop(TensorOp.sin)
    fun sinh() = unop(TensorOp.sinh)
    fun softplus() = unop(TensorOp.softplus)
    fun sqrt() = unop(TensorOp.sqrt)
    fun square() = unop(TensorOp.square)
    fun step(alpha: Float = 0f) = multiop(TensorOp.step, Tensor(alpha))
    fun tan() = unop(TensorOp.tan)
    fun tanh() = unop(TensorOp.tanh)

    // Matrices
    fun dot(other: Tensor) = binop(TensorOp.dot, other)
    fun matMul(other: Tensor, transposeA: Boolean, transposeB: Boolean) = binop(TensorOp.matMul, other)
    fun norm(ord: Any? = null, axis: IntArray? = null, keepDims: Boolean = false) = unop(TensorOp.norm)
    fun outerProd(other: Tensor) = binop(TensorOp.outerProd, other)
    fun transpose(perm: IntArray? = null) = unop(TensorOp.transpose)

    // Convolution


    operator fun unaryMinus() = neg()
    operator fun unaryPlus() = this
    operator fun plus(other: Tensor) = add(other)
    operator fun minus(other: Tensor) = sub(other)
    operator fun times(other: Tensor) = mul(other)
    operator fun div(other: Tensor) = _div(other)
    operator fun rem(other: Tensor) = mod(other)

    fun clone() = Tensor(graph, backend)
    override fun toString(): String = "Tensor($computed)"
}

package com.soywiz.korlibs.tensork.graph

import com.soywiz.korlibs.tensork.*
import com.soywiz.korlibs.tensork.backend.*

internal class TensorGraph {
    sealed class Node(val shape: TensorShape) {
        abstract fun compute(backend: TensorBackend): ShapedTensorBuffer
    }
    open class Terminal(val data: ShapedTensorBuffer) : Node(data.shape) {
        override fun compute(backend: TensorBackend): ShapedTensorBuffer = data
    }
    open class Multiop(val op: TensorOp, val nodes: List<Node>, shape: TensorShape = nodes.first().shape) : Node(shape) {
        override fun compute(backend: TensorBackend): ShapedTensorBuffer = backend.executeOp(op, *nodes.map { it.compute(backend) }.toTypedArray())
    }
    open class Unop(val op: TensorOp, val left: Node, shape: TensorShape = left.shape) : Node(shape) {
        override fun compute(backend: TensorBackend): ShapedTensorBuffer = backend.executeOp(op, left.compute(backend))
    }
    open class Binop(val op: TensorOp, val left: Node, val right: Node, shape: TensorShape = left.shape) : Node(shape) {
        override fun compute(backend: TensorBackend): ShapedTensorBuffer = backend.executeOp(op, left.compute(backend), right.compute(backend))
    }
    open class Reshape(val node: Node, shape: TensorShape) : Node(shape) {
        override fun compute(backend: TensorBackend): ShapedTensorBuffer = ShapedTensorBuffer(node.compute(backend).buffer, shape)
    }
}

package com.soywiz.korlibs.tensork

import kotlin.test.*

class TensorTest {
    @Test
    fun test() {
        //val tensor1 = Tensor(*FloatArray(16 * 1024 * 1024))
        //val tensor2 = Tensor(*FloatArray(16 * 1024 * 1024))
        //(tensor1 * tensor2).computed
        val tensor = Tensor(1f, 2f, 3f, 4f, shape = TensorShape(2, 2))
        val tensor2 = tensor.add(Tensor(1f, 2f, 3f, 4f, shape = TensorShape(2, 2)))
        val tensor3 = tensor2.mul(Tensor(2f, -1f))

        println(tensor.toString())
        println(tensor2.toString())
        println(tensor3.toString())
        println(Tensor(2f, -1f) + Tensor(100f, 10f))
    }
}

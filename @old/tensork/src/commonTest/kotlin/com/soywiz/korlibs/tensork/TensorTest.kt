package com.soywiz.korlibs.tensork

import kotlin.test.*

class TensorTest {
    @Test
    fun test() {
        val tensor = Tensor(1f, 2f, 3f, 4f, shape = TensorShape(2, 2))
        val tensor2 = tensor.add(Tensor(1f, 2f, 3f, 4f, shape = TensorShape(2, 2)))
        val tensor3 = tensor2.mul(Tensor(2f, -1f))

        assertEquals(
            """
                Tensor([[1.0,2.0],[3.0,4.0]])
                Tensor([[2.0,4.0],[6.0,8.0]])
                Tensor([[4.0,-4.0],[12.0,-8.0]])
                Tensor([102.0,9.0])
            """.trimIndent(),
            """
                $tensor
                $tensor2
                $tensor3
                ${Tensor(2f, -1f) + Tensor(100f, 10f)}
            """.trimIndent()
        )
    }
}

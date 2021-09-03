package org.tensorflow

import org.tensorflow.framework.activations.*
import org.tensorflow.framework.optimizers.*
import org.tensorflow.ndarray.*
import org.tensorflow.op.*
import org.tensorflow.op.core.*
import org.tensorflow.types.*
import org.tensorflow.types.family.*


/*
class TensorFlowWrapper {
    fun test() {
        tf.math.add()
        TensorFlow.init()
        NdArrays.vectorOf(1f, 2f, 3f)
        Tensor.of()
        NdArrays.ofBooleans(Shape.of(1, 2, 3))
        Signature.builder()
    }
}

val ops = Ops.create()
operator fun <T : TType> Operand<T>.plus(other: Operand<T>) = ops.math.add(this, other)



 */

object HelloTensorFlow {
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        println("Hello TensorFlow " + TensorFlow.version())
        ConcreteFunction.create(HelloTensorFlow::dbl).use { dbl ->
            TInt32.scalarOf(10).use { x ->
                dbl.call(x).use { dblX ->
                    println(x.getInt().toString() + " doubled is " + (dblX as TInt32).getInt())
                }
            }
        }
    }

    private fun dbl(tf: Ops): Signature {
        val x = tf.placeholder(TInt32::class.java)
        val dblX = tf.math.add(x, x)
        return Signature.builder().input("x", x).output("dbl", dblX).build()
    }
}

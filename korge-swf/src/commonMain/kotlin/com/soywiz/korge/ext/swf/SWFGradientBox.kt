package com.soywiz.korge.ext.swf

import com.soywiz.korma.geom.*


data class SWFGradientBox(val width: Double, val height: Double, val rotation: Angle, val tx: Double, val ty: Double) {
    companion object {
        val GRADIENT_SQUARE_MAX = 16384.0

        // WRONG IMPLEMENTATION
        fun fromMatrix(matrix: Matrix): SWFGradientBox {
            val transform = Matrix.Transform()
            transform.setMatrixNoReturn(matrix)
            val rotation = transform.rotation
            val twidth = (transform.scaleX * GRADIENT_SQUARE_MAX / 20) * 2
            val theight = (transform.scaleY * GRADIENT_SQUARE_MAX / 20) * 2
            val px0 = transform.x - twidth / 2
            val py0 = transform.y - theight / 2
            //val px1 = px0 + twidth * transform.rotation.cosine
            //val py1 = py0 + theight * transform.rotation.sine
            return SWFGradientBox(twidth, theight, rotation, px0, py0)
        }
    }

    fun toMatrix(out: Matrix = Matrix()): Matrix {
        val a = rotation.cosine * width * 10 / GRADIENT_SQUARE_MAX
        val b = rotation.sine * height * 10 / GRADIENT_SQUARE_MAX
        val c = -rotation.sine * width * 10 / GRADIENT_SQUARE_MAX
        val d = rotation.cosine * height * 10 / GRADIENT_SQUARE_MAX
        return out.setTo(a, b, c, d, tx + width / 2.0, ty + height / 2.0)
        return out
    }
}

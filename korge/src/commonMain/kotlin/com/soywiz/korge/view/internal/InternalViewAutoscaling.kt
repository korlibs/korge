package com.soywiz.korge.view.internal

import com.soywiz.korma.geom.*

internal class InternalViewAutoscaling {
    var renderedAtScaleX = 1.0; private set
    var renderedAtScaleY = 1.0; private set
    var renderedAtScaleXY = 1.0; private set
    private val matrixTransform = Matrix.Transform()

    fun onRender(autoScaling: Boolean, globalMatrix: Matrix): Boolean {
        if (autoScaling) {
            matrixTransform.setMatrix(globalMatrix)
            //val sx = kotlin.math.abs(matrixTransform.scaleX / this.scaleX)
            //val sy = kotlin.math.abs(matrixTransform.scaleY / this.scaleY)

            val sx = kotlin.math.abs(matrixTransform.scaleX)
            val sy = kotlin.math.abs(matrixTransform.scaleY)
            val sxy = kotlin.math.max(sx, sy)

            val diffX = kotlin.math.abs((sx / renderedAtScaleX) - 1.0)
            val diffY = kotlin.math.abs((sy / renderedAtScaleY) - 1.0)

            if (diffX >= 0.1 || diffY >= 0.1) {
                renderedAtScaleX = sx
                renderedAtScaleY = sy
                renderedAtScaleXY = sxy
                //println("renderedAtScale: $renderedAtScaleX, $renderedAtScaleY")
                return true
            }
        }
        return false
    }
}

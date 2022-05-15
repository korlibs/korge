package com.soywiz.korge.view.internal

import com.soywiz.korma.geom.Matrix
import kotlin.math.abs
import kotlin.math.max

internal class InternalViewAutoscaling {
    var renderedAtScaleXInv = 1.0; private set
    var renderedAtScaleYInv = 1.0; private set
    var renderedAtScaleX = 1.0; private set
    var renderedAtScaleY = 1.0; private set
    var renderedAtScaleXY = 1.0; private set
    private val matrixTransform = Matrix.Transform()

    fun onRender(autoScaling: Boolean, autoScalingPrecise: Boolean, globalMatrix: Matrix): Boolean {
        if (autoScaling) {
            matrixTransform.setMatrixNoReturn(globalMatrix)
            //val sx = kotlin.math.abs(matrixTransform.scaleX / this.scaleX)
            //val sy = kotlin.math.abs(matrixTransform.scaleY / this.scaleY)

            val sx = abs(matrixTransform.scaleX)
            val sy = abs(matrixTransform.scaleY)
            val sxy = max(sx, sy)

            val diffX = abs((sx / renderedAtScaleX) - 1.0)
            val diffY = abs((sy / renderedAtScaleY) - 1.0)

            val shouldUpdate = when (autoScalingPrecise) {
                true -> (diffX > 0.0 || diffY > 0.0)
                false -> diffX >= 0.1 || diffY >= 0.1
            }

            if (shouldUpdate) {
                //println("diffX=$diffX, diffY=$diffY")

                renderedAtScaleX = sx
                renderedAtScaleY = sy
                renderedAtScaleXY = sxy
                renderedAtScaleXInv = 1.0 / sx
                renderedAtScaleYInv = 1.0 / sy
                //println("renderedAtScale: $renderedAtScaleX, $renderedAtScaleY")
                return true
            }
        } else {
            renderedAtScaleX = 1.0
            renderedAtScaleY = 1.0
            renderedAtScaleXY = 1.0
            renderedAtScaleXInv = 1.0
            renderedAtScaleYInv = 1.0
        }
        return false
    }
}

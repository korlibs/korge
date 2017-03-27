package com.codeazur.as3swf.exporters

import com.codeazur.as3swf.SWF
import com.codeazur.as3swf.utils.NumberUtils
import com.soywiz.korim.geom.Matrix2d

open class ShapeExporter(val swf: SWF) {
	open fun beginShape() = Unit
	open fun endShape() = Unit
	open fun beginFills() = Unit
	open fun endFills() = Unit
	open fun beginLines() = Unit
	open fun endLines() = Unit
	open fun beginFill(color: Int, alpha: Double = 1.0) = Unit
	open fun beginGradientFill(type: String, colors: List<Int>, alphas: List<Double>, ratios: List<Int>, matrix: Matrix2d = Matrix2d(), spreadMethod: String = "pad", interpolationMethod: String = "rgb", focalPointRatio: Double = 0.0) = Unit
	open fun beginBitmapFill(bitmapId: Int, matrix: Matrix2d = Matrix2d(), repeat: Boolean = true, smooth: Boolean = false) = Unit
	open fun endFill() = Unit
	open fun lineStyle(thickness: Double = Double.NaN, color: Int = 0, alpha: Double = 1.0, pixelHinting: Boolean = false, scaleMode: String = "normal", startCaps: String? = null, endCaps: String? = null, joints: String? = null, miterLimit: Double = 3.0) = Unit
	open fun lineGradientStyle(type: String, colors: List<Int>, alphas: List<Double>, ratios: List<Int>, matrix: Matrix2d = Matrix2d(), spreadMethod: String = "pad", interpolationMethod: String = "rgb", focalPointRatio: Double = 0.0) = Unit
	open fun moveTo(x: Double, y: Double) = Unit
	open fun lineTo(x: Double, y: Double) = Unit
	open fun curveTo(controlX: Double, controlY: Double, anchorX: Double, anchorY: Double) = Unit
}

open class DefaultSVGShapeExporter(swf: SWF) : ShapeExporter(swf) {
	companion object {
		protected val DRAW_COMMAND_L: String = "L"
		protected val DRAW_COMMAND_Q: String = "Q"
	}

	protected var currentDrawCommand: String = ""
	lateinit protected var pathData: String

	override fun beginFill(color: Int, alpha: Double): Unit {
		finalizePath()
	}

	override fun beginGradientFill(type: String, colors: List<Int>, alphas: List<Double>, ratios: List<Int>, matrix: Matrix2d, spreadMethod: String, interpolationMethod: String, focalPointRatio: Double) {
		finalizePath()
	}

	override fun beginBitmapFill(bitmapId: Int, matrix: Matrix2d, repeat: Boolean, smooth: Boolean) {
		finalizePath()
	}

	override fun endFill(): Unit {
		finalizePath()
	}

	override fun lineStyle(thickness: Double, color: Int, alpha: Double, pixelHinting: Boolean, scaleMode: String, startCaps: String?, endCaps: String?, joints: String?, miterLimit: Double) {
		finalizePath()
	}

	override fun moveTo(x: Double, y: Double): Unit {
		currentDrawCommand = ""
		pathData += "M" +
			NumberUtils.roundPixels20(x) + " " +
			NumberUtils.roundPixels20(y) + " "
	}

	override fun lineTo(x: Double, y: Double): Unit {
		if (currentDrawCommand != DRAW_COMMAND_L) {
			currentDrawCommand = DRAW_COMMAND_L
			pathData += "L"
		}
		pathData += "${NumberUtils.roundPixels20(x)} ${NumberUtils.roundPixels20(y)} "
	}

	override fun curveTo(controlX: Double, controlY: Double, anchorX: Double, anchorY: Double): Unit {
		if (currentDrawCommand != DRAW_COMMAND_Q) {
			currentDrawCommand = DRAW_COMMAND_Q
			pathData += "Q"
		}
		pathData +=
			"" + NumberUtils.roundPixels20(controlX) + " " +
				NumberUtils.roundPixels20(controlY) + " " +
				NumberUtils.roundPixels20(anchorX) + " " +
				NumberUtils.roundPixels20(anchorY) + " "
	}

	override fun endLines(): Unit {
		finalizePath()
	}

	protected fun finalizePath(): Unit {
		pathData = ""
		currentDrawCommand = ""
	}
}

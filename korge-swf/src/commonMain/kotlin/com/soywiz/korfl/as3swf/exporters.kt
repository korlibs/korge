package com.soywiz.korfl.as3swf

import com.soywiz.kds.DoubleArrayList
import com.soywiz.kds.IntArrayList
import com.soywiz.korio.lang.format
import com.soywiz.korma.geom.BoundsBuilder
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.vector.LineScaleMode

open class ShapeExporter {
	open fun beginShape() = Unit
	open fun endShape() = Unit

	open fun beginFills() = Unit
	open fun beginFill(color: Int, alpha: Double = 1.0) = Unit
	open fun beginGradientFill(
		type: GradientType,
		colors: IntArrayList,
		alphas: DoubleArrayList,
		ratios: IntArrayList,
		matrix: Matrix = Matrix(),
		spreadMethod: GradientSpreadMode = GradientSpreadMode.PAD,
		interpolationMethod: GradientInterpolationMode = GradientInterpolationMode.NORMAL,
		focalPointRatio: Double = 0.0
	) = Unit

	open fun beginBitmapFill(
		bitmapId: Int,
		matrix: Matrix = Matrix(),
		repeat: Boolean = true,
		smooth: Boolean = false
	) = Unit

	open fun endFill() = Unit
	open fun endFills() = Unit

	open fun beginLines() = Unit
	open fun lineStyle(
        thickness: Double = Double.NaN,
        color: Int = 0,
        alpha: Double = 1.0,
        pixelHinting: Boolean = false,
        scaleMode: LineScaleMode = LineScaleMode.NORMAL,
        startCaps: LineCapsStyle = LineCapsStyle.ROUND,
        endCaps: LineCapsStyle = LineCapsStyle.ROUND,
        joints: String? = null,
        miterLimit: Double = 3.0
	) = Unit

	open fun lineGradientStyle(
		type: GradientType,
		colors: IntArrayList,
		alphas: DoubleArrayList,
		ratios: IntArrayList,
		matrix: Matrix = Matrix(),
		spreadMethod: GradientSpreadMode = GradientSpreadMode.PAD,
		interpolationMethod: GradientInterpolationMode = GradientInterpolationMode.NORMAL,
		focalPointRatio: Double = 0.0
	) = Unit

	open fun endLines() = Unit

	open fun moveTo(x: Double, y: Double) = Unit
	open fun lineTo(x: Double, y: Double) = Unit
	open fun curveTo(controlX: Double, controlY: Double, anchorX: Double, anchorY: Double) = Unit
	open fun closePath() = Unit
}

open class LoggerShapeExporter(val parent: ShapeExporter, val logger: (String) -> Unit = ::println) : ShapeExporter() {
	fun log(msg: String): LoggerShapeExporter = this.apply { logger(msg) }

	override fun beginShape() = log("beginShape()").parent.beginShape()
	override fun endShape() = log("endShape()").parent.endShape()
	override fun beginFills() = log("beginFills()").parent.beginFills()
	override fun endFills() = log("endFills()").parent.endFills()
	override fun beginLines() = log("beginLines()").parent.beginLines()
	override fun endLines() = log("endLines()").parent.endLines()
	override fun closePath() = log("closePath()").parent.closePath()

	override fun beginFill(color: Int, alpha: Double) =
		log("beginFill(${"%06X".format(color)}, $alpha)").parent.beginFill(color, alpha)

	override fun beginGradientFill(
		type: GradientType,
		colors: IntArrayList,
		alphas: DoubleArrayList,
		ratios: IntArrayList,
		matrix: Matrix,
		spreadMethod: GradientSpreadMode,
		interpolationMethod: GradientInterpolationMode,
		focalPointRatio: Double
	) {
		log("beginGradientFill($type, $colors, $alphas, $ratios, $matrix, $spreadMethod, $interpolationMethod, $focalPointRatio)").parent.beginGradientFill(
			type,
			colors,
			alphas,
			ratios,
			matrix,
			spreadMethod,
			interpolationMethod,
			focalPointRatio
		)
	}

	override fun beginBitmapFill(bitmapId: Int, matrix: Matrix, repeat: Boolean, smooth: Boolean) {
		log("beginBitmapFill($bitmapId, $matrix, $repeat, $smooth)").parent.beginBitmapFill(
			bitmapId,
			matrix,
			repeat,
			smooth
		)
	}

	override fun endFill() = log("endFill()").parent.endFill()
	override fun lineStyle(
		thickness: Double,
		color: Int,
		alpha: Double,
		pixelHinting: Boolean,
		scaleMode: LineScaleMode,
		startCaps: LineCapsStyle,
		endCaps: LineCapsStyle,
		joints: String?,
		miterLimit: Double
	) {
		log("lineStyle($thickness, $color, $alpha, $pixelHinting, $scaleMode, $startCaps, $endCaps, $joints, $miterLimit)").parent.lineStyle(
			thickness,
			color,
			alpha,
			pixelHinting,
			scaleMode,
			startCaps,
			endCaps,
			joints,
			miterLimit
		)
	}

	override fun lineGradientStyle(
		type: GradientType,
		colors: IntArrayList,
		alphas: DoubleArrayList,
		ratios: IntArrayList,
		matrix: Matrix,
		spreadMethod: GradientSpreadMode,
		interpolationMethod: GradientInterpolationMode,
		focalPointRatio: Double
	) {
		log("lineGradientStyle($type, $colors, $alphas, $ratios, $matrix, $spreadMethod, $interpolationMethod, $focalPointRatio)").parent.lineGradientStyle(
			type,
			colors,
			alphas,
			ratios,
			matrix,
			spreadMethod,
			interpolationMethod,
			focalPointRatio
		)
	}

	override fun moveTo(x: Double, y: Double) = log("moveTo($x, $y)").parent.moveTo(x, y)
	override fun lineTo(x: Double, y: Double) = log("lineTo($x, $y)").parent.lineTo(x, y)
	override fun curveTo(controlX: Double, controlY: Double, anchorX: Double, anchorY: Double) =
		log("curveTo($controlX, $controlY, $anchorX, $anchorY)").parent.curveTo(controlX, controlY, anchorX, anchorY)
}

class ShapeExporterBoundsBuilder : ShapeExporter() {
	val bb = BoundsBuilder()

	var lineWidth = 1.0

	override fun lineStyle(
		thickness: Double,
		color: Int,
		alpha: Double,
		pixelHinting: Boolean,
		scaleMode: LineScaleMode,
		startCaps: LineCapsStyle,
		endCaps: LineCapsStyle,
		joints: String?,
		miterLimit: Double
	) {
		lineWidth = thickness
	}

	override fun beginFills() {
		lineWidth = 0.0
	}

	override fun beginLines() {
		lineWidth = 1.0
	}

	private fun addPoint(x: Double, y: Double) {
		bb.add(x - lineWidth, y - lineWidth)
		bb.add(x + lineWidth, y + lineWidth)
	}

	private fun addRect(rect: Rectangle) {
		addPoint(rect.left, rect.top)
		addPoint(rect.right, rect.bottom)
	}

	var lastX = 0.0
	var lastY = 0.0

	override fun moveTo(x: Double, y: Double) {
		addPoint(x, y)
		lastX = x
		lastY = y
	}

	override fun lineTo(x: Double, y: Double) {
		addPoint(x, y)
		lastX = x
		lastY = y
	}

	private val tempRect = Rectangle()
	override fun curveTo(controlX: Double, controlY: Double, anchorX: Double, anchorY: Double) {
		//addRect(Bezier.quadBounds(lastX, lastY, controlX, controlY, anchorX, anchorY, tempRect))
		addPoint(controlX, controlY)
		addPoint(anchorX, anchorY)
		lastX = anchorX
		lastY = anchorY
	}

	override fun closePath() {
	}
}

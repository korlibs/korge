package com.soywiz.korge.ext.swf

import com.soywiz.korfl.as3swf.*
import com.soywiz.kds.*
import com.soywiz.kmem.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.paint.*
import com.soywiz.korim.vector.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import kotlin.math.*

/**
 * @TODO: Line ScaleMode not supported right now.
 * @TODO: Default behaviour for strokes:
 * @TODO: - smaller keeps at least 1 pixel
 * @TODO: - bigger: ScaleMode.NONE - keeps the size, ScaleMode.NORMAL - scales the stroke
 * @TODO: It would be possible to emulate using another texture with distances + colors
 * @TODO: But probably no worth
 */
class SWFShapeExporter(
	val swf: SWF,
	val debug: Boolean,
	val bounds: Rectangle,
	val export: (ShapeExporter) -> Unit,
	val rasterizerMethod: ShapeRasterizerMethod,
	val antialiasing: Boolean,
	val requestScale: Double = 2.0,
	val minSide: Int = 16,
	val maxSide: Int = 512,
	val path: VectorPath = VectorPath(),
    val charId: Int = -1
) : ShapeExporter() {
	//val bounds: Rectangle = dshape.shapeBounds.rect

	//val bmp = Bitmap32(bounds.width.toIntCeil(), bounds.height.toIntCeil())

	val realBoundsWidth = max(1, bounds.width.toIntCeil())
	val realBoundsHeight = max(1, bounds.height.toIntCeil())

	val desiredBoundsWidth = (realBoundsWidth * requestScale).toInt()
	val desiredBoundsHeight = (realBoundsHeight * requestScale).toInt()

	val limitBoundsWidth = desiredBoundsWidth.clamp(minSide, maxSide)
	val limitBoundsHeight = desiredBoundsHeight.clamp(minSide, maxSide)

	val actualScale = min(
		limitBoundsWidth.toDouble() / realBoundsWidth.toDouble(),
		limitBoundsHeight.toDouble() / realBoundsHeight.toDouble()
	)

	//val actualScale = 0.5

	val actualBoundsWidth = (realBoundsWidth * actualScale).toInt()
	val actualBoundsHeight = (realBoundsHeight * actualScale).toInt()

	private var cshape = CompoundShape(listOf())
    private val shapes = arrayListOf<Shape>()

	val actualShape: CompoundShape by lazy {
		export(if (debug) LoggerShapeExporter(this) else this)
		//this.dshape.export(if (debug) LoggerShapeExporter(this) else this)
		cshape
	}

	val image: Bitmap by lazy {
        BitmapVector(
            shape = actualShape,
            bounds = bounds,
            scale = actualScale,
            rasterizerMethod = rasterizerMethod,
            antialiasing = antialiasing,
            width = actualBoundsWidth,
            height = actualBoundsHeight,
            premultiplied = true
        )
	}
	val imageWithScale by lazy {
		BitmapWithScale(image, actualScale, bounds)
	}

	var drawingFill = true

	var apath = VectorPath()
	override fun beginShape() {
		//ctx.beginPath()
	}

	override fun endShape() {
		cshape = CompoundShape(shapes)
		//ctx.closePath()
	}

	override fun beginFills() {
		flush()
		drawingFill = true
	}

	override fun endFills() {
		flush()
	}

	override fun beginLines() {
		flush()
		drawingFill = false
	}

	override fun endLines() {
		flush()
	}

	fun GradientSpreadMode.toCtx() = when (this) {
		GradientSpreadMode.PAD -> CycleMethod.NO_CYCLE
		GradientSpreadMode.REFLECT -> CycleMethod.REFLECT
		GradientSpreadMode.REPEAT -> CycleMethod.REPEAT
	}

	var fillStyle: Paint = NonePaint

	override fun beginFill(color: Int, alpha: Double) {
		flush()
		drawingFill = true
		fillStyle = ColorPaint(decodeSWFColor(color, alpha))
	}

    private fun createGradientPaint(
        type: GradientType,
        colors: List<Int>,
        alphas: List<Double>,
        ratios: List<Int>,
        matrix: Matrix,
        spreadMethod: GradientSpreadMode,
        interpolationMethod: GradientInterpolationMode,
        focalPointRatio: Double
    ): GradientPaint {
        val aratios = DoubleArrayList(*ratios.map { it.toDouble() / 255.0 }.toDoubleArray())
        val acolors = IntArrayList(*colors.zip(alphas).map { decodeSWFColor(it.first, it.second).value }.toIntArray())

        val m2 = Matrix()
        m2.copyFrom(matrix)

        m2.pretranslate(-0.5, -0.5)
        m2.prescale(1638.4 / 2.0, 1638.4 / 2.0)

        val imethod = when (interpolationMethod) {
            GradientInterpolationMode.NORMAL -> GradientInterpolationMethod.NORMAL
            GradientInterpolationMode.LINEAR -> GradientInterpolationMethod.LINEAR
        }

        return when (type) {
            GradientType.LINEAR -> GradientPaint(
                GradientKind.LINEAR,
                -1.0, 0.0, 0.0,
                +1.0, 0.0, 0.0,
                aratios, acolors,
                spreadMethod.toCtx(),
                m2,
                imethod
            )
            GradientType.RADIAL -> GradientPaint(
                GradientKind.RADIAL,
                focalPointRatio, 0.0, 0.0,
                0.0, 0.0, 1.0,
                aratios, acolors,
                spreadMethod.toCtx(),
                m2,
                imethod
            )
        }
    }

	override fun beginGradientFill(
		type: GradientType,
		colors: List<Int>,
		alphas: List<Double>,
		ratios: List<Int>,
		matrix: Matrix,
		spreadMethod: GradientSpreadMode,
		interpolationMethod: GradientInterpolationMode,
		focalPointRatio: Double
	) {
		flush()
		drawingFill = true
		fillStyle = createGradientPaint(
			type, colors, alphas, ratios,
			matrix, spreadMethod, interpolationMethod, focalPointRatio
		)
	}

	override fun beginBitmapFill(bitmapId: Int, matrix: Matrix, repeat: Boolean, smooth: Boolean) {
		flush()
		drawingFill = true
		val bmp = swf.bitmaps[bitmapId] ?: Bitmap32(1, 1)
		fillStyle = BitmapPaint(
            bmp, matrix.clone(), CycleMethod.fromRepeat(repeat), CycleMethod.fromRepeat(repeat), smooth
        )
	}

	override fun endFill() {
		flush()
	}

	private fun __flushFill() {
		if (apath.isEmpty()) return
        shapes += FillShape(apath, null, fillStyle, Matrix())
		apath = VectorPath()
	}

	private fun __flushStroke() {
		if (apath.isEmpty()) return
		shapes += PolylineShape(
			apath,
			null,
			strokeStyle,
            Matrix(),
			lineWidth,
			true,
			LineScaleMode.NORMAL,
			lineCap,
			lineCap,
			LineJoin.MITER,
			miterLimit
		)
		apath = VectorPath()
	}

	private fun flush() {
		if (drawingFill) {
			__flushFill()
		} else {
			__flushStroke()
		}
	}

	private var lineWidth: Double = 1.0
	private var lineScaleMode = LineScaleMode.NORMAL
	private var miterLimit = 1.0
	private var lineCap: LineCap = LineCap.ROUND
	private var strokeStyle: Paint = ColorPaint(Colors.BLACK)

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
		flush()
		this.drawingFill = false
        this.lineWidth = thickness
		this.lineScaleMode = scaleMode
		this.miterLimit = miterLimit
		this.strokeStyle = ColorPaint(decodeSWFColor(color, alpha))
		this.lineCap = when (startCaps) {
			LineCapsStyle.NO -> LineCap.BUTT
			LineCapsStyle.ROUND -> LineCap.ROUND
			LineCapsStyle.SQUARE -> LineCap.SQUARE
		}
	}

	override fun lineGradientStyle(
		type: GradientType,
		colors: List<Int>,
		alphas: List<Double>,
		ratios: List<Int>,
		matrix: Matrix,
		spreadMethod: GradientSpreadMode,
		interpolationMethod: GradientInterpolationMode,
		focalPointRatio: Double
	) {
		flush()
		drawingFill = false
		strokeStyle = createGradientPaint(
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

	override fun moveTo(x: Double, y: Double) {
		apath.moveTo(x, y)
        //println("moveTo($x, $y)")
		if (drawingFill) path.moveTo(x, y)
	}

	override fun lineTo(x: Double, y: Double) {
		apath.lineTo(x, y)
        //println("lineTo($x, $y)")
		if (drawingFill) path.lineTo(x, y)
	}

	override fun curveTo(controlX: Double, controlY: Double, anchorX: Double, anchorY: Double) {
		apath.quadTo(controlX, controlY, anchorX, anchorY)
        //println("curveTo($controlX, $controlY, $anchorX, $anchorY)")
		if (drawingFill) path.quadTo(controlX, controlY, anchorX, anchorY)
	}

	override fun closePath() {
		apath.close()
		if (drawingFill) path.close()
	}
}

fun SWFColorTransform.toColorTransform() = ColorTransform(rMult, gMult, bMult, aMult, rAdd, gAdd, bAdd, aAdd)

fun decodeSWFColor(color: Int, alpha: Double = 1.0) =
	RGBA(color.extract8(16), color.extract8(8), color.extract8(0), (alpha * 255).toInt())


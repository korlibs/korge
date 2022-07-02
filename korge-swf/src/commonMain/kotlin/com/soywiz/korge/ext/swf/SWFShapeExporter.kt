package com.soywiz.korge.ext.swf

import com.soywiz.kds.DoubleArrayList
import com.soywiz.kds.IDoubleArrayList
import com.soywiz.kds.IntArrayList
import com.soywiz.kds.mapDouble
import com.soywiz.kmem.clamp
import com.soywiz.kmem.extract8
import com.soywiz.kmem.insert8
import com.soywiz.kmem.toIntCeil
import com.soywiz.korfl.as3swf.*
import com.soywiz.korge.view.GraphicsRenderer
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.paint.*
import com.soywiz.korim.vector.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.math.roundDecimalPlaces
import kotlin.math.*

fun TagDefineShape.export(bitmaps: Map<Int, Bitmap>): Shape {
    val exporter = SWFBaseShapeExporter(bitmaps)
    export(exporter)
    return exporter.cshape
}
fun TagDefineMorphShape.export(bitmaps: Map<Int, Bitmap>, ratio: Double): Shape {
    val exporter = SWFBaseShapeExporter(bitmaps)
    export(exporter, ratio)
    return exporter.cshape
}

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
	path: VectorPath = VectorPath(),
    val charId: Int = -1,
    roundDecimalPlaces: Int = -1,
    val graphicsRenderer: GraphicsRenderer = GraphicsRenderer.SYSTEM,
) : SWFBaseShapeExporter(swf.bitmaps, path, roundDecimalPlaces) {
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
            premultiplied = true,
            native = graphicsRenderer != GraphicsRenderer.CPU,
        )
	}
	val imageWithScale by lazy {
		BitmapWithScale(image, actualScale, bounds)
	}
}

open class SWFBaseShapeExporter(
    val bitmaps: Map<Int, Bitmap>,
    val path: VectorPath = VectorPath(),
    val roundDecimalPlaces: Int = -1
) : ShapeExporter() {
    //val bounds: Rectangle = dshape.shapeBounds.rect

    //val bmp = Bitmap32(bounds.width.toIntCeil(), bounds.height.toIntCeil())

    var cshape = CompoundShape(listOf())
        private set

    private val shapes = arrayListOf<Shape>()

    var drawingFill = true

    var apath = VectorPath()
    override fun beginShape() {
        //ctx.beginPath()
        apath = VectorPath()
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
        fillStyle = decodeSWFColor(color, alpha)
    }

    private fun createGradientPaint(
        type: GradientType,
        colors: IntArrayList,
        alphas: DoubleArrayList,
        ratios: IntArrayList,
        matrix: Matrix,
        spreadMethod: GradientSpreadMode,
        interpolationMethod: GradientInterpolationMode,
        focalPointRatio: Double
    ): GradientPaint {
        val aratios = ratios.mapDouble { it.toDouble() / 255.0 }
        val acolors = IntArrayList(colors.size)
        for (n in colors.indices) acolors.add(decodeSWFColor(colors[n], alphas[n]).value)

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
        colors: IntArrayList,
        alphas: DoubleArrayList,
        ratios: IntArrayList,
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
        val bmp = bitmaps[bitmapId] ?: Bitmap32(1, 1)
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
            StrokeInfo(
                lineWidth,
                true,
                lineScaleMode,
                lineCap,
                lineCap,
                LineJoin.MITER,
                miterLimit,
                lineDash,
                lineDashOffset
            )
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
    private var lineDash: IDoubleArrayList? = null
    private var lineDashOffset: Double = 0.0
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
        colors: IntArrayList,
        alphas: DoubleArrayList,
        ratios: IntArrayList,
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

    private val Double.nice: Double get() = when {
        roundDecimalPlaces < 0 -> this
        else -> this.roundDecimalPlaces(roundDecimalPlaces)
    }

    override fun moveTo(x: Double, y: Double) {
        apath.moveTo(x.nice, y.nice)
        //println("moveTo($x, $y)")
        if (drawingFill) path.moveTo(x.nice, y.nice)
    }

    override fun lineTo(x: Double, y: Double) {
        apath.lineTo(x.nice, y.nice)
        //println("lineTo($x, $y)")
        if (drawingFill) path.lineTo(x.nice, y.nice)
    }

    override fun curveTo(controlX: Double, controlY: Double, anchorX: Double, anchorY: Double) {
        apath.quadTo(controlX.nice, controlY.nice, anchorX.nice, anchorY.nice)
        //println("curveTo($controlX, $controlY, $anchorX, $anchorY)")
        if (drawingFill) path.quadTo(controlX.nice, controlY.nice, anchorX.nice, anchorY.nice)
    }

    override fun closePath() {
        apath.close()
    }
}

internal fun SWFColorTransform.toColorTransform() = ColorTransform(rMult, gMult, bMult, aMult, rAdd, gAdd, bAdd, aAdd)

fun encodeSWFColor(color: RGBA): Int = 0.insert8(color.r, 16).insert8(color.g, 8).insert8(color.b, 0)

internal fun decodeSWFColor(color: Int, alpha: Double = 1.0): RGBA =
	RGBA(color.extract8(16), color.extract8(8), color.extract8(0), (alpha * 255).toInt().clamp(0, 255))


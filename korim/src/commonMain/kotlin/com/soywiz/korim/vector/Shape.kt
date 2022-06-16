package com.soywiz.korim.vector

import com.soywiz.kds.IDoubleArrayList
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korim.bitmap.toUri
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.font.Font
import com.soywiz.korim.font.drawText
import com.soywiz.korim.paint.BitmapPaint
import com.soywiz.korim.paint.ColorPaint
import com.soywiz.korim.paint.GradientKind
import com.soywiz.korim.paint.GradientPaint
import com.soywiz.korim.paint.NonePaint
import com.soywiz.korim.paint.Paint
import com.soywiz.korim.text.HorizontalAlign
import com.soywiz.korim.text.VerticalAlign
import com.soywiz.korim.vector.format.SVG
import com.soywiz.korim.vector.format.SvgPath
import com.soywiz.korio.serialization.xml.Xml
import com.soywiz.korio.util.niceStr
import com.soywiz.korio.util.toStringDecimal
import com.soywiz.korma.geom.BoundsBuilder
import com.soywiz.korma.geom.IPointArrayList
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.bezier.Bezier
import com.soywiz.korma.geom.bezier.Curves
import com.soywiz.korma.geom.bezier.isConvex
import com.soywiz.korma.geom.contains
import com.soywiz.korma.geom.vector.LineCap
import com.soywiz.korma.geom.vector.LineJoin
import com.soywiz.korma.geom.vector.LineScaleMode
import com.soywiz.korma.geom.vector.StrokeInfo
import com.soywiz.korma.geom.vector.VectorPath
import com.soywiz.korma.geom.vector.add
import com.soywiz.korma.geom.vector.applyTransform
import com.soywiz.korma.geom.vector.strokeToFill
import com.soywiz.korma.geom.vector.toCurves
import com.soywiz.korma.geom.vector.toCurvesList
import kotlin.math.max
import kotlin.math.round

/*
<svg width="80px" height="30px" viewBox="0 0 80 30"
     xmlns="http://www.w3.org/2000/svg">

  <defs>
    <linearGradient id="Gradient01">
      <stop offset="20%" stop-color="#39F" />
      <stop offset="90%" stop-color="#F3F" />
    </linearGradient>
  </defs>

  <rect x="10" y="10" width="60" height="10"
        fill="url(#Gradient01)" />
</svg>
 */

class SvgBuilder(val bounds: Rectangle, val scale: Double) {
	val defs = arrayListOf<Xml>()
	val nodes = arrayListOf<Xml>()

	//val tx = -bounds.x
	//val ty = -bounds.y

	fun toXml(): Xml {
		return Xml.Tag(
			"svg",
			linkedMapOf(
				"width" to "${(bounds.width * scale).niceStr}px",
				"height" to "${(bounds.height * scale).niceStr}px",
				"viewBox" to "0 0 ${(bounds.width * scale).niceStr} ${(bounds.height * scale).niceStr}",
				"xmlns" to "http://www.w3.org/2000/svg",
				"xmlns:xlink" to "http://www.w3.org/1999/xlink"
			),
			listOf(
				Xml.Tag("defs", mapOf(), defs),
				Xml.Tag(
					"g",
					mapOf("transform" to Matrix().translate(-bounds.x, -bounds.y).scale(scale, scale).toSvg()),
					nodes
				)
			) //+ nodes
		)
	}
}

fun buildSvgXml(width: Int? = null, height: Int? = null, block: ShapeBuilder.() -> Unit): Xml = buildShape(width, height) { block() }.toSvg()

private fun Matrix.toSvg() = this.run {
	when (getType()) {
		Matrix.Type.IDENTITY -> "translate()"
		Matrix.Type.TRANSLATE -> "translate(${tx.niceStr}, ${ty.niceStr})"
		Matrix.Type.SCALE -> "scale(${a.niceStr}, ${d.niceStr})"
		Matrix.Type.SCALE_TRANSLATE -> "translate(${tx.niceStr}, ${ty.niceStr}) scale(${a.niceStr}, ${d.niceStr})"
		else -> "matrix(${a.niceStr}, ${b.niceStr}, ${c.niceStr}, ${d.niceStr}, ${tx.niceStr}, ${ty.niceStr})"
	}
}

fun VectorPath.toSvgPathString(separator: String = " ", decimalPlaces: Int = 1): String =
    SvgPath.toSvgPathString(this, separator, decimalPlaces)

//fun VectorPath.toSvgPathString(scale: Double, tx: Double, ty: Double): String {
//	val parts = arrayListOf<String>()
//
//	//fun Double.fix() = (this * scale).toInt()
//	fun Double.fixX() = ((this + tx) * scale).niceStr
//	fun Double.fixY() = ((this + ty) * scale).niceStr
//
//	this.visitCmds(
//		moveTo = { x, y -> parts += "M${x.fixX()} ${y.fixY()}" },
//		lineTo = { x, y -> parts += "L${x.fixX()} ${y.fixY()}" },
//		quadTo = { x1, y1, x2, y2 -> parts += "Q${x1.fixX()} ${y1.fixY()}, ${x2.fixX()} ${y2.fixY()}" },
//		cubicTo = { x1, y1, x2, y2, x3, y3 -> parts += "C${x1.fixX()} ${y1.fixY()}, ${x2.fixX()} ${y2.fixY()}, ${x3.fixX()} ${y3.fixY()}" },
//		close = { parts += "Z" }
//	)
//	return parts.joinToString("")
//}

interface Shape : BoundsDrawable {
	fun addBounds(bb: BoundsBuilder, includeStrokes: Boolean = false): Unit
	fun buildSvg(svg: SvgBuilder): Unit = Unit
    fun getPath(path: VectorPath = VectorPath()): VectorPath = path

    // Unoptimized version
    override val bounds: Rectangle get() = BoundsBuilder().also { addBounds(it) }.getBounds()
	fun containsPoint(x: Double, y: Double): Boolean = bounds.contains(x, y)
}

fun Shape.getBounds(out: Rectangle = Rectangle(), bb: BoundsBuilder = BoundsBuilder(), includeStrokes: Boolean = false): Rectangle {
	bb.reset()
	addBounds(bb, includeStrokes)
	bb.getBounds(out)
    return out
}

fun Shape.toSvgInstance(scale: Double = 1.0): SVG = SVG(toSvg(scale))
fun Shape.toSvg(scale: Double = 1.0): Xml = SvgBuilder(this.getBounds(), scale).apply { buildSvg(this) }.toXml()
fun Drawable.toShape(width: Int, height: Int): Shape = buildShape(width, height) { draw(this@toShape) }
fun Drawable.toSvg(width: Int, height: Int, scale: Double = 1.0): Xml = toShape(width, height).toSvg(scale)

fun SizedDrawable.toShape(): Shape = toShape(width, height)
fun SizedDrawable.toSvg(scale: Double = 1.0): Xml = toSvg(width, height, scale)

interface StyledShape : Shape {
    /**
     * Path with transform already applied
     *
     * @TODO: Probably it shouldn't have the transform applied
     */
	val path: VectorPath? get() = null
	val clip: VectorPath?
	val paint: Paint
	val transform: Matrix
    val globalAlpha: Double

    fun getUntransformedPath(): VectorPath? {
        return path?.clone()?.applyTransform(transform.inverted())
    }

	override fun addBounds(bb: BoundsBuilder, includeStrokes: Boolean) {
        path?.let { path ->
            // path is already transformed, so using `transform` is not required
            bb.add(path)
        }
	}

	override fun buildSvg(svg: SvgBuilder) {
		svg.nodes += Xml.Tag(
			"path", mapOf(
				"d" to (getUntransformedPath()?.toSvgPathString() ?: ""),
                "transform" to transform.toSvg()
            ) + getSvgXmlAttributes(svg), listOf()
		)
	}

    override fun getPath(path: VectorPath): VectorPath = path.also {
        this.path?.let { path.write(it) }
    }

    fun getSvgXmlAttributes(svg: SvgBuilder): Map<String, String> = mapOf(
		//"transform" to transform.toSvg()
	)

	override fun draw(c: Context2d) {
		c.keepTransform {
			//c.transform(transform) // Already applied to the path
			c.beginPath()
			path?.draw(c)
			if (clip != null) {
				clip!!.draw(c)
				c.clip()
			}
			drawInternal(c)
		}
	}

	fun drawInternal(c: Context2d) {
	}
}

private fun colorToSvg(color: RGBA): String {
	val r = color.r
	val g = color.g
	val b = color.b
	val af = color.af
	return "rgba($r,$g,$b,${af.smallNiceStr})"
}

private val Float.smallNiceStr: String get() = if (round(this) == this) "${this.toInt()}" else "$this"

fun Paint.toSvg(svg: SvgBuilder): String {
	val id = svg.defs.size
	/*
	svg.defs += when (this) {
		is Paint.
		Xml.Tag("")
	}
	return "url(#def$id)"
	*/
	when (this) {
		is GradientPaint -> {
			val stops = (0 until numberOfStops).map {
				val ratio = this.stops[it]
				val color = RGBA(this.colors.getAt(it))
				Xml.Tag("stop", mapOf("offset" to "${ratio * 100}%", "stop-color" to colorToSvg(color)), listOf())
			}

            when (this.kind) {
                GradientKind.LINEAR -> {
                    svg.defs += Xml.Tag(
                        "linearGradient",
                        mapOf(
                            "id" to "def$id",
                            "x1" to "$x0", "y1" to "$y0",
                            "x2" to "$x1", "y2" to "$y1",
                            "gradientTransform" to transform.toSvg()
                        ),
                        stops
                    )
                }
                GradientKind.RADIAL -> {
                    svg.defs += Xml.Tag(
                        "radialGradient",
                        mapOf(
                            "id" to "def$id",
                            "cx" to "$x0", "cy" to "$y0",
                            "fx" to "$x1", "fy" to "$y1",
                            "r" to "$r1",
                            "gradientTransform" to transform.toSvg()
                        ),
                        stops
                    )
                }
                else -> Unit
            }

			return "url(#def$id)"
		}
		is BitmapPaint -> {
			//<pattern id="img1" patternUnits="userSpaceOnUse" width="100" height="100">
			//<image xlink:href="wall.jpg" x="0" y="0" width="100" height="100" />
			//</pattern>


			svg.defs += Xml.Tag(
				"pattern", mapOf(
					"id" to "def$id",
					"patternUnits" to "userSpaceOnUse",
					"width" to "${bitmap.width}",
					"height" to "${bitmap.height}",
					"patternTransform" to transform.toSvg()
				), listOf(
					Xml.Tag(
						"image",
						mapOf(
							"xlink:href" to bitmap.toUri(),
							"width" to "${bitmap.width}",
							"height" to "${bitmap.height}"
						),
						listOf<Xml>()
					)
				)
			)
			return "url(#def$id)"
		}
		is ColorPaint -> {
			return colorToSvg(color)
		}
		else -> return "red"
	}
}

object EmptyShape : Shape {
    override fun addBounds(bb: BoundsBuilder, includeStrokes: Boolean) = Unit
    override fun draw(c: Context2d) = Unit
}

data class FillShape(
    override val path: VectorPath,
    override val clip: VectorPath?,
    override val paint: Paint,
    override val transform: Matrix = Matrix(),
    override val globalAlpha: Double = 1.0,
) : StyledShape {
    val pathCurvesList: List<Curves> by lazy {
        //println("Computed pathCurves for path=$path")
        path.toCurvesList()
    }
    val clipCurvesList: List<Curves>? by lazy { clip?.toCurvesList() }
    val isConvex: Boolean get() = pathCurvesList.size == 1 && pathCurvesList.first().isConvex && (clipCurvesList == null)

	override fun drawInternal(c: Context2d) {
		c.fill(paint)
	}

	override fun getSvgXmlAttributes(svg: SvgBuilder) = super.getSvgXmlAttributes(svg) + mapOf(
		"fill" to paint.toSvg(svg)
	)

	override fun containsPoint(x: Double, y: Double): Boolean {
		val tx = transform.transformX(x, y)
		val ty = transform.transformY(x, y)
		if (clip != null) return clip.containsPoint(tx, ty)
		return path.containsPoint(tx, ty)
	}
}

data class PolylineShape constructor(
    override val path: VectorPath,
    override val clip: VectorPath?,
    override val paint: Paint,
    override val transform: Matrix,
    val strokeInfo: StrokeInfo,
    override val globalAlpha: Double = 1.0,
) : StyledShape {
    private val tempBB = BoundsBuilder()
    private val tempRect = Rectangle()

    val thickness by strokeInfo::thickness
    val scaleMode by strokeInfo::scaleMode
    val startCaps by strokeInfo::startCap
    val endCaps by strokeInfo::endCap
    val lineJoin by strokeInfo::join
    val miterLimit by strokeInfo::miterLimit
    val lineDash by strokeInfo::dash
    val lineDashOffset by strokeInfo::dashOffset

    val fillShape: FillShape by lazy {
        FillShape(path.strokeToFill(strokeInfo), clip, paint, transform, globalAlpha)
    }

    override fun addBounds(bb: BoundsBuilder, includeStrokes: Boolean) {
        tempBB.reset()
        tempBB.add(path)
        tempBB.getBounds(tempRect)

        //println("TEMP_RECT: ${tempRect}")

        if (includeStrokes) {
            val halfThickness = max(strokeInfo.thickness / 2.0, 0.0)
            tempRect.inflate(halfThickness, halfThickness)
        }

        //println("  TEMP_RECT AFTER INFLATE: ${tempRect}")

        //println("PolylineShape.addBounds: thickness=$thickness, rect=$tempRect")
        bb.add(tempRect)
    }

    override fun drawInternal(c: Context2d) {
		c.lineScaleMode = strokeInfo.scaleMode
		c.lineWidth = strokeInfo.thickness
		c.lineCap = strokeInfo.endCap
        c.lineJoin = strokeInfo.join
		c.stroke(paint)
	}

	override fun containsPoint(x: Double, y: Double): Boolean {
		val tx = transform.transformX(x, y)
		val ty = transform.transformY(x, y)
		if (clip != null) return clip.containsPoint(tx, ty)
		return path.containsPoint(tx, ty)
	}

	override fun getSvgXmlAttributes(svg: SvgBuilder) = super.getSvgXmlAttributes(svg) + mapOf(
        "fill" to "none",
		"stroke-width" to "${strokeInfo.thickness}",
		"stroke" to paint.toSvg(svg)
	)
}

class CompoundShape(
	val components: List<Shape>
) : Shape {
	override fun addBounds(bb: BoundsBuilder, includeStrokes: Boolean) { components.fastForEach { it.addBounds(bb, includeStrokes)}  }
	override fun draw(c: Context2d) = c.buffering { components.fastForEach { it.draw(c) } }
	override fun buildSvg(svg: SvgBuilder) { components.fastForEach { it.buildSvg(svg) } }
    override fun getPath(path: VectorPath): VectorPath = path.also { components.fastForEach { it.getPath(path) } }
	override fun containsPoint(x: Double, y: Double): Boolean {
		return components.any { it.containsPoint(x, y) }
	}
}

class TextShape(
    val text: String,
    val x: Double,
    val y: Double,
    val font: Font?,
    val fontSize: Double,
    override val clip: VectorPath?,
    val fill: Paint?,
    val stroke: Paint?,
    val halign: HorizontalAlign = HorizontalAlign.LEFT,
    val valign: VerticalAlign = VerticalAlign.TOP,
    override val transform: Matrix = Matrix(),
    override val globalAlpha: Double = 1.0,
) : StyledShape {
    override val paint: Paint get() = fill ?: stroke ?: NonePaint

    override fun addBounds(bb: BoundsBuilder, includeStrokes: Boolean) {
        bb.add(x, y)
        bb.add(x + fontSize * text.length, y + fontSize) // @TODO: this is not right since we don't have information about Glyph metrics
    }
    override fun drawInternal(c: Context2d) {
        c.font(font, halign, valign) {
            if (fill != null) c.fillText(text, x, y)
            if (stroke != null) c.strokeText(text, x, y)
        }
    }

    val primitiveShapes: Shape by lazy {
        buildShape {
            this.transform(this@TextShape.transform)
            this.clip(this@TextShape.clip)
            if (this@TextShape.fill != null) this@TextShape.font?.drawText(this, this@TextShape.fontSize, this@TextShape.text, this@TextShape.fill, this@TextShape.x, this@TextShape.y, fill = true)
            if (this@TextShape.stroke != null) this@TextShape.font?.drawText(this, this@TextShape.fontSize, this@TextShape.text, this@TextShape.stroke, this@TextShape.x, this@TextShape.y, fill = false)
        }
    }

    override fun buildSvg(svg: SvgBuilder) {
        svg.nodes += Xml.Tag(
            "text", mapOf(
                "x" to x,
                "y" to y,
                "fill" to (fill?.toSvg(svg) ?: "none"),
                "stroke" to (stroke?.toSvg(svg) ?: "none"),
                "font-family" to font?.name,
                "font-size" to "${fontSize}px",
                "text-anchor" to when (halign) {
                    HorizontalAlign.JUSTIFY -> "justify"
                    HorizontalAlign.LEFT -> "start"
                    HorizontalAlign.CENTER -> "middle"
                    HorizontalAlign.RIGHT -> "end"
                    else -> "${(halign.ratio * 100)}%"
                },
                "alignment-baseline" to when (valign) {
                    VerticalAlign.TOP -> "hanging"
                    VerticalAlign.MIDDLE -> "middle"
                    VerticalAlign.BASELINE -> "baseline"
                    VerticalAlign.BOTTOM -> "bottom"
                    else -> "${(valign.ratio * 100)}%"
                },
                "transform" to transform.toSvg()
            ), listOf(
                Xml.Text(text)
            )
        )
    }
}

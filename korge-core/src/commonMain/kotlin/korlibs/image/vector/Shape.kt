package korlibs.image.vector

import korlibs.datastructure.iterators.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.image.paint.*
import korlibs.image.text.*
import korlibs.image.vector.format.*
import korlibs.io.serialization.xml.*
import korlibs.math.geom.*
import korlibs.math.geom.bezier.*
import korlibs.math.geom.vector.*
import korlibs.number.*
import kotlin.math.*

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

class SvgBuilder(
    val bounds: Rectangle,
    val scale: Double,
    val roundDecimalPlaces: Int = -1
) {
	val defs = arrayListOf<Xml>()
	val nodes = arrayListOf<Xml>()

	//val tx = -bounds.x
	//val ty = -bounds.y

    val Double.nice: String get() = niceStr(roundDecimalPlaces)

	fun toXml(): Xml {
		return Xml.Tag(
			"svg",
			linkedMapOf(
				"width" to "${(bounds.width * scale).nice}px",
				"height" to "${(bounds.height * scale).nice}px",
				"viewBox" to "0 0 ${(bounds.width * scale).nice} ${(bounds.height * scale).nice}",
				"xmlns" to "http://www.w3.org/2000/svg",
				"xmlns:xlink" to "http://www.w3.org/1999/xlink"
			),
			listOf(
				Xml.Tag("defs", mapOf(), defs),
				Xml.Tag(
					"g",
					mapOf("transform" to Matrix().translated(-bounds.x, -bounds.y).scaled(scale, scale).toSvg(roundDecimalPlaces)),
					nodes
				)
			) //+ nodes
		)
	}
}

fun buildSvgXml(width: Int? = null, height: Int? = null, block: ShapeBuilder.() -> Unit): Xml = buildShape(width, height) { block() }.toSvg()

private fun Matrix.toSvg(roundDecimalPlaces: Int = -1): String = this.mutable.toSvg(roundDecimalPlaces)

private fun MMatrix.toSvg(roundDecimalPlaces: Int = -1): String {
    val places = roundDecimalPlaces
	return when (getType()) {
		MatrixType.IDENTITY -> "translate()"
		MatrixType.TRANSLATE -> "translate(${tx.niceStr(places)}, ${ty.niceStr(places)})"
		MatrixType.SCALE -> "scale(${a.niceStr(places)}, ${d.niceStr(places)})"
		MatrixType.SCALE_TRANSLATE -> "translate(${tx.niceStr(places)}, ${ty.niceStr(places)}) scale(${a.niceStr(places)}, ${d.niceStr(places)})"
		else -> "matrix(${a.niceStr(places)}, ${b.niceStr(places)}, ${c.niceStr(places)}, ${d.niceStr(places)}, ${tx.niceStr(places)}, ${ty.niceStr(places)})"
	}
}

fun VectorPath.toSvgPathString(separator: String = " ", decimalPlaces: Int = 1): String =
    SvgPath.toSvgPathString(this, separator, decimalPlaces)

fun VectorPath.toContext2dCommands(prefix: String = "ctx.", suffix: String = ";", decimalPlaces: Int = 1): List<String> = buildList {
    fun Float.round(): String = this.niceStr(decimalPlaces)
    fun Double.round(): String = this.niceStr(decimalPlaces)
    this@toContext2dCommands.visitCmds(
        moveTo = { (x, y) -> add("moveTo(${x.round()}, ${y.round()})") },
        lineTo = { (x, y) -> add("lineTo(${x.round()}, ${y.round()})") },
        quadTo = { (cx, cy), (ax, ay) -> add("quadTo(${cx.round()}, ${cy.round()}, ${ax.round()}, ${ay.round()})") },
        cubicTo = { (cx1, cy1), (cx2, cy2), (ax, ay) -> add("cubicTo(${cx1.round()}, ${cy1.round()}, ${cx2.round()}, ${cy2.round()}, ${ax.round()}, ${ay.round()})") },
        close = { add("close()") }
    )
}.map { "$prefix$it$suffix" }

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

sealed interface Shape : BoundsDrawable {
	fun buildSvg(svg: SvgBuilder): Unit = Unit
    fun getPath(path: VectorPath = VectorPath()): VectorPath = path

    // Unoptimized version
    fun getBounds(includeStrokes: Boolean = true): Rectangle
    override val bounds: Rectangle get() = getBounds(includeStrokes = true)
	fun containsPoint(x: Double, y: Double): Boolean = bounds.contains(x, y)
}

fun Shape.optimize(): Shape {
    return when (this) {
        is CompoundShape -> {
            val comps = this.components.filter { it !is EmptyShape }
            if (comps.size == 1) comps.first() else CompoundShape(comps)
        }
        else -> this
    }
}

fun Shape.toSvgInstance(scale: Double = 1.0): SVG = SVG(toSvg(scale))
fun Shape.toSvg(scale: Double = 1.0, roundDecimalPlaces: Int = -1): Xml = SvgBuilder(this.getBounds(), scale, roundDecimalPlaces).apply { buildSvg(this) }.toXml()
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

	override fun getBounds(includeStrokes: Boolean): Rectangle {
        return path?.let { path ->
            (BoundsBuilder() + path).bounds
            // path is already transformed, so using `transform` is not required
        } ?: Rectangle.NIL
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
			c.beginPath()
			path?.draw(c)
			if (clip != null) {
				clip!!.draw(c)
				c.clip()
			}
            c.transform(transform.immutable)
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
                            "gradientTransform" to transform.toSvg(svg.roundDecimalPlaces)
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
    override fun getBounds(includeStrokes: Boolean): Rectangle = Rectangle.NIL
    override fun draw(c: Context2d) = Unit
}

data class FillShape(
    override val path: VectorPath,
    override val clip: VectorPath?,
    override val paint: Paint,
    override val transform: Matrix = Matrix.IDENTITY,
    override val globalAlpha: Double = 1.0,
) : StyledShape {
    val pathCurvesList: List<Curves> by lazy {
        //println("Computed pathCurves for path=$path")
        path.toCurvesList()
    }
    val clipCurvesList: List<Curves>? by lazy { clip?.toCurvesList() }
    val isConvex: Boolean get() = pathCurvesList.size == 1 && pathCurvesList.first().isConvex && (clipCurvesList == null)

	override fun drawInternal(c: Context2d) {
		c.fill(paint, path.winding)
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

    override fun getBounds(includeStrokes: Boolean): Rectangle {
        val bounds = (BoundsBuilder() + path).boundsOrNull()?.mutable

        //println("PolylineShape.addBounds: bounds=bounds, path=$path")

        //println("TEMP_RECT: ${tempRect}")

        if (includeStrokes) {
            val halfThickness = max(strokeInfo.thickness / 2.0, 0.0)
            bounds?.inflate(halfThickness, halfThickness)
        }

        //println("  TEMP_RECT AFTER INFLATE: ${tempRect}")

        //println("PolylineShape.addBounds: thickness=$thickness, rect=$tempRect")
        return bounds?.immutable ?: Rectangle.ZERO
    }

    fun setState(c: Context2d) {
        c.lineScaleMode = strokeInfo.scaleMode
        c.lineWidth = strokeInfo.thickness
        c.startLineCap = strokeInfo.startCap
        c.endLineCap = strokeInfo.endCap
        c.lineJoin = strokeInfo.join
        c.miterLimit = strokeInfo.miterLimit
        c.lineDash = strokeInfo.dash
        c.lineDashOffset = strokeInfo.dashOffset
    }

    override fun drawInternal(c: Context2d) {
        setState(c)
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
		"stroke-width" to strokeInfo.thickness.niceStr(svg.roundDecimalPlaces),
		"stroke" to paint.toSvg(svg)
	)
}

open class CompoundShape(
	val components: List<Shape>
) : Shape {
	override fun getBounds(includeStrokes: Boolean): Rectangle {
        var bb = BoundsBuilder()
        components.fastForEach { bb += it.getBounds(includeStrokes) }
        return bb.bounds
    }
	override fun draw(c: Context2d) = c.buffering { components.fastForEach { it.draw(c) } }
	override fun buildSvg(svg: SvgBuilder) { components.fastForEach { it.buildSvg(svg) } }
    override fun getPath(path: VectorPath): VectorPath = path.also { components.fastForEach { it.getPath(path) } }
	override fun containsPoint(x: Double, y: Double): Boolean {
		return components.any { it.containsPoint(x, y) }
	}

    override fun toString(): String {
        return "CompoundShape(\n  " + components.joinToString(",\n  ") + "\n)"
    }
}

class TextShape(
    val text: String,
    val pos: Point,
    val font: Font?,
    val fontSize: Double,
    override val clip: VectorPath?,
    val fill: Paint?,
    val stroke: Paint?,
    val align: TextAlignment = TextAlignment.TOP_LEFT,
    override val transform: Matrix = Matrix.IDENTITY,
    override val globalAlpha: Double = 1.0,
) : StyledShape {
    override val paint: Paint get() = fill ?: stroke ?: NonePaint

    override fun getBounds(includeStrokes: Boolean): Rectangle =
        Rectangle.fromBounds(pos, Point(pos.x + fontSize * text.length, pos.y + fontSize)) // @TODO: this is not right since we don't have information about Glyph metrics
    override fun drawInternal(c: Context2d) {
        c.font(font, align) {
            if (fill != null) c.fillText(text, pos)
            if (stroke != null) c.strokeText(text, pos)
        }
    }

    val primitiveShapes: Shape by lazy {
        buildShape {
            this.transform(this@TextShape.transform.immutable)
            this.clip(this@TextShape.clip)
            if (this@TextShape.fill != null) this@TextShape.font?.drawText(this, this@TextShape.fontSize, this@TextShape.text, this@TextShape.fill, this@TextShape.pos, fill = true)
            if (this@TextShape.stroke != null) this@TextShape.font?.drawText(this, this@TextShape.fontSize, this@TextShape.text, this@TextShape.stroke, this@TextShape.pos, fill = false)
        }
    }

    override fun buildSvg(svg: SvgBuilder) {
        svg.nodes += Xml.Tag(
            "text", mapOf(
                "x" to pos.x,
                "y" to pos.y,
                "fill" to (fill?.toSvg(svg) ?: "none"),
                "stroke" to (stroke?.toSvg(svg) ?: "none"),
                "font-family" to font?.name,
                "font-size" to "${fontSize}px",
                "text-anchor" to when (align.horizontal) {
                    HorizontalAlign.JUSTIFY -> "justify"
                    HorizontalAlign.LEFT -> "start"
                    HorizontalAlign.CENTER -> "middle"
                    HorizontalAlign.RIGHT -> "end"
                    else -> "${(align.horizontal.ratio * 100)}%"
                },
                "alignment-baseline" to when (align.vertical) {
                    VerticalAlign.TOP -> "hanging"
                    VerticalAlign.MIDDLE -> "middle"
                    VerticalAlign.BASELINE -> "baseline"
                    VerticalAlign.BOTTOM -> "bottom"
                    else -> "${(align.vertical.ratio * 100)}%"
                },
                "transform" to transform.toSvg()
            ), listOf(
                Xml.Text(text)
            )
        )
    }
}

fun Shape.transformedShape(m: Matrix): Shape = when (this) {
    is EmptyShape -> EmptyShape
    is CompoundShape -> CompoundShape(components.map { it.transformedShape(m) })
    is FillShape -> FillShape(path.applyTransform(m), clip?.applyTransform(m), paint, transform * m, globalAlpha)
    is PolylineShape -> PolylineShape(path.applyTransform(m), clip?.applyTransform(m), paint, transform * m, strokeInfo, globalAlpha)
    is TextShape -> TextShape(text, pos, font, fontSize, clip?.applyTransform(m), fill, stroke, align, transform * m, globalAlpha)
    else -> TODO()
}

fun Shape.scaledShape(sx: Double, sy: Double = sx): Shape = transformedShape(Matrix.IDENTITY.scaled(sx, sy))
fun Shape.translatedShape(x: Double = 0.0, y: Double = 0.0): Shape = transformedShape(Matrix.IDENTITY.translated(x, y))

fun Shape.mapShape(map: (Shape) -> Shape): Shape {
    val result = map(this)
    return if (result is CompoundShape) {
        CompoundShape(
            result.components
                .map { it.mapShape(map) }
                .filter { it !is EmptyShape }
        )
    } else {
        result
    }
}

fun Shape.filterShape(filter: (Shape) -> Boolean): Shape = when {
    filter(this) -> {
        when (this) {
            is CompoundShape -> {
                CompoundShape(
                    this.components
                        .map { it.filterShape(filter) }
                        .filter { it !is EmptyShape }
                )
            }

            else -> this
        }
    }
    else -> EmptyShape
}

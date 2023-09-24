package korlibs.image.vector.format

import korlibs.datastructure.*
import korlibs.image.annotation.*
import korlibs.image.color.*
import korlibs.image.paint.*
import korlibs.image.style.*
import korlibs.image.text.*
import korlibs.image.vector.*
import korlibs.io.lang.*
import korlibs.io.serialization.xml.*
import korlibs.io.util.*
import korlibs.logger.*
import korlibs.math.geom.*
import korlibs.math.geom.shape.*
import korlibs.math.geom.vector.*
import korlibs.math.interpolation.*
import korlibs.time.*
import kotlin.collections.set

class SVG(val root: Xml, val warningProcessor: ((message: String) -> Unit)? = null) : SizedDrawable {
	//constructor(@Language("xml") str: String) : this(Xml(str))
	constructor(str: String) : this(Xml(str))

    override fun toString(): String = "SVG($width, $height)"

    val x = root.int("x", 0)
	val y = root.int("y", 0)

	val dwidth = root.double("width", 128.0)
	val dheight = root.double("height", 128.0)
	val viewBox = root.getString("viewBox") ?: "0 0 $dwidth $dheight"
	val viewBoxNumbers = viewBox.split(' ').map { it.trim().toDoubleOrNull() ?: 0.0 }
	val viewBoxRectangle = Rectangle(
		viewBoxNumbers.getOrElse(0) { 0.0 },
		viewBoxNumbers.getOrElse(1) { 0.0 },
		viewBoxNumbers.getOrElse(2) { dwidth },
		viewBoxNumbers.getOrElse(3) { dheight }
	)

	override val width get() = viewBoxRectangle.width.toInt()
	override val height get() = viewBoxRectangle.height.toInt()

	val defs by lazy {
        hashMapOf<String, SvgDef>().also { _defs ->
            for (defs in root.children("defs")) {
                for (def in defs.allNodeChildren) {
                    val svgDef = SvgDef(def)
                    _defs[svgDef.id] = svgDef
                }
            }
        }
    }
    @KorimExperimental
    val cssList by lazy {
        arrayListOf<CSS>().also { cssList ->
            for (style in root.children("style")) {
                cssList += CSS.parseCSS(style.text)
            }
        }
    }
    @KorimExperimental
    val cssCombined by lazy { CSS(cssList) }
    private val _dom by lazy { DOM(cssCombined) }
    @KorimExperimental
    val dom by lazy {
        renderElement
        _dom
    }
    //val elementsById by lazy {
    //    (_dom.elementsById as Map<String, SvgElement>).also {
    //        renderElement
    //    }
    //}
    val renderElement by lazy {
        SvgElement(root)
    }
    @KorimExperimental
    val animator by lazy {
        DOMAnimator(dom)
    }

    @KorimExperimental
    fun updateStyles(dt: TimeSpan) {
        animator.update(dt)
    }

    class SvgDef(val def: Xml) {
        val id = def.str("id").lowercase()
        val type = def.nameLC
        val stops = parseStops(def)
        val gradientUnits = when (def.getString("gradientUnits") ?: "objectBoundingBox") {
            "userSpaceOnUse" -> GradientUnits.USER_SPACE_ON_USE
            else -> GradientUnits.OBJECT_BOUNDING_BOX
        }
        val gradientTransform: Matrix = def.getString("gradientTransform")?.let { CSS.parseTransform(it) } ?: Matrix.IDENTITY
        val spreadMethod = when ((def.getString("spreadMethod") ?: "pad").lowercase()) {
            "pad" -> CycleMethod.NO_CYCLE
            "repeat" -> CycleMethod.REPEAT
            "reflect" -> CycleMethod.REFLECT
            else -> CycleMethod.NO_CYCLE
        }

        val g: GradientPaint = when (type) {
            "lineargradient" -> {
                //println("Linear: ($x0,$y0)-($x1-$y1)")
                val x0 = def.double("x1", 0.0)
                val y0 = def.double("y1", 0.0)
                val x1 = def.double("x2", 1.0)
                val y1 = def.double("y2", 1.0)
                GradientPaint(GradientKind.LINEAR, x0, y0, 0.0, x1, y1, 0.0, cycle = spreadMethod, transform = gradientTransform, units = gradientUnits)
            }
            else -> {
                val cx = def.double("cx", 0.0)
                val cy = def.double("cy", 0.0)
                val r = def.double("r", 16.0)
                val fx = def.double("fx", cx)
                val fy = def.double("fy", cy)
                GradientPaint(GradientKind.RADIAL, cx, cy, 0.0, fx, fy, r, cycle = spreadMethod, transform = gradientTransform, units = gradientUnits)
            }
        }

        val ref = def.strNull("xlink:href")

        init {
            for ((offset, color) in stops) {
                //println(" - $offset: $color")
                g.addColorStop(offset, color)
            }
        }

        val paint = g

        fun parseStops(xml: Xml): List<Pair<Ratio, RGBA>> {
            val out = arrayListOf<Pair<Ratio, RGBA>>()
            for (stop in xml.children("stop")) {
                val info = SVG.parseAttributesAndStyles(stop)
                var offset = Ratio.ZERO
                var colorStop = SVG.ColorDefaultBlack.defaultColor
                var alphaStop = 1f
                for ((key, value) in info) {
                    when (key) {
                        "offset" -> offset = CSS.parseRatio(value)
                        "stop-color" -> colorStop = SVG.ColorDefaultBlack[value]
                        "stop-opacity" -> alphaStop = value.toFloatOrNull() ?: 1f
                    }
                }
                out += Pair(offset, RGBA(colorStop.rgb, ((colorStop.ad * alphaStop) * 255).toInt()))
            }
            return out
        }
    }

    fun createSvgElementFromXml(xml: Xml): SvgElement? {
        val nodeName = xml.nameLC
        return when (nodeName) {
            "g", "a", "svg" -> SvgElement(xml)
            "defs" -> null
            "_text_" -> null
            "_comment_" -> null
            "title" -> null
            "rect" -> RectSvgElement(xml)
            "circle" -> CircleSvgElement(xml)
            "ellipse" -> EllipseSvgElement(xml)
            "polyline", "polygon" -> PolySvgElement(xml)
            "line" -> LineSvgElement(xml)
            "text" -> TextSvgElement(xml)
            "path" -> PathSvgElement(xml)
            else -> {
                warningProcessor?.invoke("Unhandled SVG node '$nodeName'")
                null
            }
        }
    }

    object Mappings {
        val SVGMapping = DOM.DomPropertyMapping()
            .add("transform", SvgElement::transform)
            .add("opacity", SvgElement::opacity)
    }

    open inner class SvgElement(val xml: Xml) : DOM.DomElement(_dom, Mappings.SVGMapping) {
        init {
            id = xml.getString("id")
        }

        val attributes: Map<String, String> = parseAttributesAndStyles(xml)
        var transform: Matrix = attributes["transform"]?.let { CSS.parseTransform(it) } ?: Matrix.IDENTITY
        var opacity: Double = attributes["opacity"]?.toDoubleOrNull() ?: 1.0

        val children = xml.allNodeChildren.mapNotNull {
            createSvgElementFromXml(it)
        }

        override fun toString(): String = "SvgElement($id, ${xml.nameLC}, children=${children.size})"

        fun draw(c: Context2d): Unit {
            c.keepApply {
                //svg.drawElement(xml, c, render)
                transform?.let {
                    c.state.transform = c.state.transform.premultiplied(it.immutable)
                    //c.state.transform.postmultiply(it)
                }
                setCommonStyles(c)
                drawChildren(c)
                drawInternal(c)
                fillStrokeInternal(c)
            }
        }

        open fun drawChildren(c: Context2d) {
            for (child in children) {
                child.draw(c)
            }
        }

        open fun drawInternal(c: Context2d) {
        }

        open fun fillStrokeInternal(c: Context2d) {
            if (c.state.path.isNotEmpty()) {
                c.keep {
                    //c.fillStyle = c.fillStyle.getPaintWithUnits(c.state.transform, c.state.path.clone().applyTransform(c.state.transform).getBounds())
                    c.fillStyle = c.fillStyle.getPaintWithUnits(c.state.transform, c.state.path)
                    c.strokeStyle = c.strokeStyle.getPaintWithUnits(c.state.transform, c.state.path)
                    //println("fillStyle=${c.fillStyle}, strokeStyle=${c.strokeStyle}")
                    c.fillStroke()
                }
            }
        }

        fun String.parseNumber(default: Double): Double {
            val str = this.trim()
            val scale = when {
                str.endsWith("px") -> 1.0
                str.endsWith("pt") -> 1.0
                str.endsWith("em") -> 10.0
                str.endsWith("%") -> 0.01
                else -> 1.0
            }
            return str.removeSuffix("px").removeSuffix("pt").removeSuffix("em").removeSuffix("%").toFloatOrNull()?.times(scale) ?: default
        }

        open fun setCommonStyles(c: Context2d) {
            for ((key, it) in attributes) {
                when (key) {
                    "stroke-width" -> c.lineWidth = it.parseNumber(1.0)
                    "stroke-linejoin" -> c.lineJoin = LineJoin[it]
                    "stroke-linecap" -> c.lineCap = LineCap[it]
                    "stroke" -> c.strokeStyle = parseFillStroke(c, it)
                    "opacity" -> c.globalAlpha *= opacity
                    "fill-opacity" -> c.globalAlpha *= it.parseNumber(1.0) // @TODO: Do this properly
                    "stroke-opacity" -> c.globalAlpha *= it.parseNumber(1.0) // @TODO: Do this properly
                    "fill" -> c.fillStyle = parseFillStroke(c, it)
                    "font-size" -> c.fontSize = CSS.parseSizeAsDouble(it)
                    "font-family" -> c.font = c.fontRegistry?.get(it)
                    "text-anchor" -> c.horizontalAlign = when (it.lowercase().trim()) {
                        "left" -> HorizontalAlign.LEFT
                        "center", "middle" -> HorizontalAlign.CENTER
                        "right", "end" -> HorizontalAlign.RIGHT
                        else -> c.horizontalAlign
                    }
                    "alignment-baseline" -> c.verticalAlign = when (it.lowercase().trim()) {
                        "hanging" -> VerticalAlign.TOP
                        "center", "middle" -> VerticalAlign.MIDDLE
                        "baseline" -> VerticalAlign.BASELINE
                        "bottom" -> VerticalAlign.BOTTOM
                        else -> c.verticalAlign
                    }
                    "fill-rule" -> Unit // @TODO
                }
            }
        }

        //override fun setProperty(prop: String, value: Any?) {
        //    when (prop) {
        //        "transform" -> {
        //            //this.transform = getMatrix(prop, value)
        //            this.transform = getTransform(prop, value).toMatrix()
        //        }
        //        "opacity" -> {
        //            this.opacity = getRatio(prop, value)
        //        }
        //    }
        //}

        fun parseFillStroke(c: Context2d, str2: String): Paint {
            val str = str2.lowercase().trim()
            val res = when {
                str.startsWith("url(") -> {
                    val urlPattern = str.substr(4, -1).substringBefore(')')
                    val extra = str.substringAfter(')')

                    if (urlPattern.startsWith("#")) {
                        val idName = urlPattern.substr(1).toLowerCase()
                        val def = defs[idName]
                        if (def == null) {
                            logger.info { defs }
                            logger.info { "Can't find svg definition '$idName'" }
                        }
                        //println("URL: def=$def")
                        def?.paint ?: NonePaint
                    } else {
                        logger.info { "Unsupported $str" }
                        NonePaint
                    }
                }
                str.startsWith("rgba(") -> {
                    val components = str.removePrefix("rgba(").removeSuffix(")").split(",").map { it.trim().toDoubleOrNull() ?: 0.0 }
                    ColorPaint(RGBA(components[0].toInt(), components[1].toInt(), components[2].toInt(), (components[3] * 255).toInt()))
                }
                str.startsWith("rgb(") -> {
                    val components = str.removePrefix("rgb(").removeSuffix(")").split(",").map { it.trim().toDoubleOrNull() ?: 0.0 }
                    ColorPaint(RGBA(components[0].toInt(), components[1].toInt(), components[2].toInt(), 255))
                }
                else -> when (str) {
                    "none" -> NonePaint
                    else -> c.createColor(ColorDefaultBlack[str])
                }
            }
            //println("parseFillStroke: res=$res, str2='$str2'")
            return res
        }

    }

    open inner class RectSvgElement(xml: Xml) : SvgElement(xml) {
        var x = xml.double("x")
        var y = xml.double("y")
        var width = xml.double("width")
        var height = xml.double("height")
        var ry = xml.double("ry", xml.double("rx"))
        var rx = xml.double("rx", xml.double("ry"))

        override fun drawInternal(c: Context2d) {
            c.roundRect(x, y, width, height, rx, ry)
        }
    }

    open inner class CircleSvgElement(xml: Xml) : SvgElement(xml) {
        var cx = xml.double("cx")
        var cy = xml.double("cy")
        var radius = xml.double("r")

        override fun drawInternal(c: Context2d) {
            c.circle(Point(cx, cy), radius)
        }
    }

    open inner class EllipseSvgElement(xml: Xml) : SvgElement(xml) {
        var cx = xml.double("cx")
        var cy = xml.double("cy")
        var rx = xml.double("rx")
        var ry = xml.double("ry")

        override fun drawInternal(c: Context2d) {
            c.ellipse(Point(cx - rx, cy - ry), Size(rx * 2, ry * 2))
        }
    }

    open inner class PolySvgElement(xml: Xml) : SvgElement(xml) {
        val ss = StrReader(xml.str("points"))
        val pps = ListReader(mapWhile(cond = { ss.hasMore }, gen = {
            ss.skipWhile { !it.isNumeric }
            val out = ss.readWhile { it.isNumeric }.toDouble()
            ss.skipWhile { !it.isNumeric }
            out
        }).toList())
        val path = VectorPath().also { path ->
            var edges = 0
            path.moveTo(Point(pps.read(), pps.read()))
            while (pps.hasMore) {
                val x = pps.read()
                val y = pps.read()
                path.lineTo(Point(x, y))
                edges++
            }
            if (xml.nameLC == "polygon") path.close()
        }

        override fun drawInternal(c: Context2d) {
            c.beginPath()
            //println("bounds: $bounds, edges: $edges")
            c.path(path)
        }
    }

    open inner class LineSvgElement(xml: Xml) : SvgElement(xml) {
        var x1 = xml.double("x1")
        var y1 = xml.double("y1")
        var x2 = xml.double("x2")
        var y2 = xml.double("y2")

        override fun drawInternal(c: Context2d) {
            //println("LINE: $x1, $y1, $x2, $y2: $attributes")
            c.beginPath()
            c.moveTo(Point(x1, y1))
            c.lineTo(Point(x2, y2))
        }
    }

    open inner class TextSvgElement(xml: Xml) : SvgElement(xml) {
        var text = xml.text.trim()
        var x = xml.float("x")
        var dx = xml.float("dx")
        var y = xml.float("y")
        var dy = xml.float("dy")

        override fun drawInternal(c: Context2d) {
        }

        override fun fillStrokeInternal(c: Context2d) {
            c.fillText(text, Point(x + dx, y + dy))
        }
    }

    open inner class PathSvgElement(xml: Xml) : SvgElement(xml) {
        var d = xml.str("d")
        val path by lazy { SvgPath.parse(d) }
        init {
            warningProcessor?.invoke("Parsed SVG Path: '${path.toSvgPathString()}'")
            warningProcessor?.invoke("Original SVG Path: '$d'")
            warningProcessor?.invoke("Points: ${path.cachedPoints}")
        }

        override fun drawInternal(c: Context2d) {
            c.beginPath()
            c.write(path)
        }
    }

    //interface Def

	override fun draw(c: Context2d) {
		c.keep {
            c.strokeStyle = NonePaint
            c.fillStyle = Colors.BLACK
            renderElement.draw(c)
		}
	}


    class CSSDeclarations {
        val props = LinkedHashMap<String, String>()

        companion object {
            fun parseToMap(str: String): Map<String, String> = CSSDeclarations().parse(str).props
        }

        fun parse(str: String): CSSDeclarations = str.reader().parse()

        fun StrReader.parse(): CSSDeclarations {
            while (!eof) {
                parseCssDecl()
            }
            return this@CSSDeclarations
        }

        fun StrReader.parseCssDecl() {
            skipSpaces()
            val id = readCssId()
            skipSpaces()
            skipExpect(':')
            skipSpaces()
            //readStringLit()
            // @TODO: Proper parsing
            val value = readUntil { it == ';' }.trim()
            props[id] = value
            if (!eof) {
                skipExpect(';')
            }
        }

        fun StrReader.readCssId() = readWhile { it.isLetterOrDigit() || it == '-' }
    }

	companion object {
        val logger = Logger("SVG")

        val ColorDefaultBlack = Colors.WithDefault(Colors.BLACK)

        fun parseAttributesAndStyles(node: Xml): Map<String, String> {
            val out = node.attributes.toMutableMap()
            node.getString("style")?.let { out.putAll(CSSDeclarations.parseToMap(it)) }
            return out
        }
	}

	interface PathToken {
        val anyValue: Any
    }
	data class PathTokenNumber(val value: Double) : PathToken {
        override val anyValue: Any get() = value
    }
	data class PathTokenCmd(val id: Char) : PathToken {
        override val anyValue: Any get() = id
    }
}

package com.soywiz.korim.vector.format

import com.soywiz.kds.ListReader
import com.soywiz.kds.expect
import com.soywiz.kds.mapWhile
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.vector.*
import com.soywiz.korim.vector.paint.*
import com.soywiz.korio.lang.invalidOp
import com.soywiz.korio.lang.printStackTrace
import com.soywiz.korio.lang.substr
import com.soywiz.korio.serialization.xml.Xml
import com.soywiz.korio.serialization.xml.allChildren
import com.soywiz.korio.serialization.xml.isComment
import com.soywiz.korio.util.StrReader
import com.soywiz.korio.util.isDigit
import com.soywiz.korio.util.isLetterOrUnderscore
import com.soywiz.korio.util.isNumeric
import com.soywiz.korma.geom.Matrix
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.geom.a
import com.soywiz.korma.geom.vector.*
import kotlin.collections.set
import kotlin.math.*

class SVG(val root: Xml, val warningProcessor: ((message: String) -> Unit)? = null) : SizedDrawable {
	//constructor(@Language("xml") str: String) : this(Xml(str))
	constructor(str: String) : this(Xml(str))

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

	class Style {
		val props = hashMapOf<String, Any?>()
	}

	enum class GradientUnits {
		USER_SPACE_ON_USER,
		OBJECT_BOUNDING_BOX,
	}

	val defs = hashMapOf<String, Paint>()

	//interface Def

	fun parsePercent(str: String): Double {
		return if (str.endsWith("%")) {
			str.substr(0, -1).toDouble() / 100.0
		} else {
			str.toDouble()
		}
	}

	fun parseStops(xml: Xml): List<Pair<Double, RGBA>> {
		val out = arrayListOf<Pair<Double, RGBA>>()
		for (stop in xml.children("stop")) {
			val offset = parsePercent(stop.str("offset"))
			val colorStop = Colors.Default[stop.str("stop-color")]
			val alphaStop = stop.double("stop-opacity", 1.0)
			out += Pair(offset, RGBA(colorStop.rgb, (alphaStop * 255).toInt()))
		}
		return out
	}

	fun parseDef(def: Xml) {
		val type = def.nameLC
		when (type) {
			"lineargradient", "radialgradient" -> {
				val id = def.str("id").toLowerCase()
				val x0 = def.double("x1", 0.0)
				val y0 = def.double("y1", 0.0)
				val x1 = def.double("x2", 1.0)
				val y1 = def.double("y2", 1.0)
				val stops = parseStops(def)
				val href = def.strNull("xlink:href")

				val g: GradientPaint = if (type == "lineargradient") {
					//println("Linear: ($x0,$y0)-($x1-$y1)")
					GradientPaint(GradientKind.LINEAR, x0, y0, 0.0, x1, y1, 0.0)
				} else {
					val r0 = def.double("r0", 0.0)
					val r1 = def.double("r1", 0.0)
					GradientPaint(GradientKind.RADIAL, x0, y0, r0, x1, y1, r1)
				}

				def.strNull("xlink:href")?.let {
					val id = it.trim('#')
					val original = defs[id] as? GradientPaint?
					//println("href: $it --> $original")
					original?.let {
						g.stops.add(original.stops)
						g.colors.add(original.colors)
					}
				}

				for ((offset, color) in stops) {
					//println(" - $offset: $color")
					g.addColorStop(offset, color)
				}
				//println("Gradient: $g")
				def.getString("gradientTransform")?.let {
					g.transform.premultiply(parseTransform(it))
				}

				defs[id] = g
			}
			"style" -> {
			}
			"_text_" -> {
			}
			else -> {
				println("Unhandled def: '$type'")
			}
		}
	}

	fun parseDefs() {
		for (def in root["defs"].allChildren.filter { !it.isComment }) parseDef(def)
	}

	init {
		parseDefs()
	}

	override fun draw(c: Context2d) {
		c.keep {
			c.strokeStyle = NonePaint
			c.fillStyle = NonePaint //ColorPaint(Colors.BLACK)
			drawElement(root, c)
		}
	}

	fun drawChildren(xml: Xml, c: Context2d) {
		for (child in xml.allChildren) {
			drawElement(child, c)
		}
	}

	fun parseFillStroke(c: Context2d, str2: String, bounds: Rectangle): Paint {
		val str = str2.toLowerCase().trim()
		val res = when {
            str.startsWith("url(") -> {
                val urlPattern = str.substr(4, -1)
                if (urlPattern.startsWith("#")) {
                    val idName = urlPattern.substr(1).toLowerCase()
                    val def = defs[idName]
                    if (def == null) {
                        println(defs)
                        println("Can't find svg definition '$idName'")
                    }
                    def ?: NonePaint
                } else {
                    println("Unsupported $str")
                    NonePaint
                }
            }
            str.startsWith("rgba(") -> {
                val components = str.removePrefix("rgba(").removeSuffix(")").split(",").map { it.trim().toDoubleOrNull() ?: 0.0 }
                ColorPaint(RGBA(components[0].toInt(), components[1].toInt(), components[2].toInt(), (components[3] * 255).toInt()))
            }
            else -> when (str) {
                "none" -> NonePaint
                else -> c.createColor(Colors.Default[str])
            }
        }
        return when (res) {
            is GradientPaint -> {
                val m = Matrix()
                m.scale(bounds.width, bounds.height)
                val out = res.applyMatrix(m)
                //println(out)
                out
            }
            else -> {
                res
            }
        }
	}

    private val t = DoubleArray(6)

    fun drawElement(xml: Xml, c: Context2d): Context2d = c.keepApply {
		val bounds = Rectangle()
		val nodeName = xml.nameLC

		when (nodeName) {
			"_text_" -> Unit
			"svg" -> drawChildren(xml, c)
			"lineargradient", "radialgradient" -> {
				parseDef(xml)
			}
			"rect" -> {
				val x = xml.double("x")
				val y = xml.double("y")
				val width = xml.double("width")
				val height = xml.double("height")
				val rx = xml.double("rx")
				val ry = xml.double("ry")
				bounds.setTo(x, y, width, height)
				roundRect(x, y, width, height, rx, ry)
			}
			"circle" -> {
				val cx = xml.double("cx")
				val cy = xml.double("cy")
				val radius = xml.double("r")
				circle(cx, cy, radius)
				bounds.setBounds(cx - radius, cy - radius, cx + radius, cy + radius)
			}
			"polyline", "polygon" -> {
				beginPath()
				val ss = StrReader(xml.str("points"))

				val pps = ListReader(mapWhile(cond = { ss.hasMore }, gen = {
					ss.skipWhile { !it.isNumeric }
					val out = ss.readWhile { it.isNumeric }.toDouble()
					ss.skipWhile { !it.isNumeric }
					out
				}).toList())
				val path = GraphicsPath()
				var edges = 0
				path.moveTo(pps.read(), pps.read())
				while (pps.hasMore) {
					val x = pps.read()
                    val y = pps.read()
					path.lineTo(x, y)
					edges++
				}
				if (nodeName == "polygon") path.close()
				path.getBounds(bounds)
				//println("bounds: $bounds, edges: $edges")
				c.path(path)
			}
			"line" -> {
				beginPath()
				val x1 = xml.double("x1")
				val y1 = xml.double("y1")
				val x2 = xml.double("x2")
				val y2 = xml.double("y2")
				moveTo(x1, y1)
				lineTo(x2, y2)
				bounds.setBounds(x1, y1, x2, y2)
			}
			"g" -> {
			}
			"text" -> {
			}
			"path" -> {
				val d = xml.str("d")
				val tokens = tokenizePath(d)
				val tl = ListReader(tokens)

				fun dumpTokens() = run { for ((n, token) in tokens.withIndex()) warningProcessor?.invoke("- $n: $token") }
				fun isNextNumber(): Boolean = if (tl.hasMore) tl.peek() is PathTokenNumber else false
				fun readNumber(): Double {
					while (tl.hasMore) {
						val token = tl.read()
						if (token is PathTokenNumber) return token.value
                        warningProcessor?.invoke("Invalid path (expected number but found $token) at ${tl.position - 1}")
						dumpTokens()
					}
					return 0.0
				}
                fun n(): Double = readNumber()
                fun nX(relative: Boolean): Double = if (relative) lastX + readNumber() else readNumber()
                fun nY(relative: Boolean): Double = if (relative) lastY + readNumber() else readNumber()

				fun readNextTokenCmd(): Char? {
					while (tl.hasMore) {
						val token = tl.read()
						if (token is PathTokenCmd) return token.id
                        warningProcessor?.invoke("Invalid path (expected command but found $token) at ${tl.position - 1}")
						dumpTokens()
					}
					return null
				}

				//dumpTokens()

				beginPath()
                moveTo(0.0, 0.0) // Supports relative positioning as first command
                var lastCX = 0.0
                var lastCY = 0.0
                var lastCmd = '-'

                while (tl.hasMore) {
					val cmd = readNextTokenCmd() ?: break
                    val relative = cmd in 'a'..'z' // lower case
                    var lastCurve = when (lastCmd) {
                        'S', 'C', 'T', 'Q', 's', 'c', 't', 'q' -> true
                        else -> false
                    }
					when (cmd) {
						'M', 'm' -> {
							rMoveTo(n(), n(), relative)
							while (isNextNumber()) rLineTo(n(), n(), relative)
						}
						'L', 'l' -> while (isNextNumber()) rLineTo(n(), n(), relative)
						'H', 'h' -> while (isNextNumber()) rLineToH(n(), relative)
						'V', 'v' -> while (isNextNumber()) rLineToV(n(), relative)
						'Q', 'q' -> while (isNextNumber()) {
                            val cx = nX(relative)
                            val cy = nY(relative)
                            val x2 = nX(relative)
                            val y2 = nY(relative)
                            lastCX = cx
                            lastCY = cy
                            quadTo(cx, cy, x2, y2)
                        }
						'C', 'c' -> while (isNextNumber()) {
                            val x1 = nX(relative)
                            val y1 = nY(relative)
                            val x2 = nX(relative)
                            val y2 = nY(relative)
                            val x = nX(relative)
                            val y = nY(relative)
                            lastCX = x2
                            lastCY = y2
                            cubicTo(x1, y1, x2, y2, x, y)
                        }
                        'S', 's' -> {
                            while (isNextNumber()) {
                                // https://www.stkent.com/2015/07/03/building-smooth-paths-using-bezier-curves.html
                                // https://developer.mozilla.org/en-US/docs/Web/SVG/Tutorial/Paths

                                // S produces the same type of curve as earlier—but if it follows another S command or a C command,
                                // the first control point is assumed to be a reflection of the one used previously.
                                // If the S command doesn't follow another S or C command, then the current position of the cursor
                                // is used as the first control point. In this case the result is the same as what the Q command
                                // would have produced with the same parameters.

                                val x2 = nX(relative)
                                val y2 = nY(relative)
                                val x = nX(relative)
                                val y = nY(relative)

                                val x1 = if (lastCurve) lastX * 2 - lastCX else lastX
                                val y1 = if (lastCurve) lastY * 2 - lastCY else lastY

                                lastCX = x2
                                lastCY = y2

                                cubicTo(x1, y1, x2, y2, x, y)
                                lastCurve = true
                            }
                        }
                        'T', 't' -> {
                            var n = 0
                            while (isNextNumber()) {
                                val x2 = nX(relative)
                                val y2 = nY(relative)
                                val cx = if (lastCurve) lastX * 2 - lastCX else lastX
                                val cy = if (lastCurve) lastY * 2 - lastCY else lastY
                                //println("[$cmd]: $lastX, $lastY, $cx, $cy, $x2, $y2 :: $lastX - $lastCX :: $cx :: $lastCurve :: $lastCmd")
                                lastCX = cx
                                lastCY = cy
                                quadTo(cx, cy, x2, y2)
                                n++
                                lastCurve = true
                            }
                        }
                        'A', 'a' -> {
                            // Ported from nanosvg (https://github.com/memononen/nanosvg/blob/25241c5a8f8451d41ab1b02ab2d865b01600d949/src/nanosvg.h#L2067)
                            // Ported from canvg (https://code.google.com/p/canvg/)
                            var rx = readNumber().absoluteValue				// y radius
                            var ry = readNumber().absoluteValue				// x radius
                            val rotx = readNumber() / 180.0 * PI        // x rotation angle
                            val fa = if ((readNumber().absoluteValue) > 1e-6) 1 else 0	// Large arc
                            val fs = if ((readNumber().absoluteValue) > 1e-6) 1 else 0	// Sweep direction
                            val x1 = lastX							// start point
                            val y1 = lastY                          // end point
                            val x2 = nX(relative)
                            val y2 = nY(relative)

                            var dx = x1 - x2
                            var dy = y1 - y2

                            val d = hypot(dx, dy)
                            if (d < 1e-6f || rx < 1e-6f || ry < 1e-6f) {
                                // The arc degenerates to a line
                                lineTo(x2, y2)
                            } else {
                                val sinrx = kotlin.math.sin(rotx)
                                val cosrx = kotlin.math.cos(rotx)

                                // Convert to center point parameterization.
                                // http://www.w3.org/TR/SVG11/implnote.html#ArcImplementationNotes
                                // 1) Compute x1', y1'
                                val x1p = cosrx * dx / 2.0f + sinrx * dy / 2.0f
                                val y1p = -sinrx * dx / 2.0f + cosrx * dy / 2.0f
                                var d = sqr(x1p) / sqr(rx) + sqr(y1p) / sqr(ry)
                                if (d > 1) {
                                    d = sqr(d)
                                    rx *= d
                                    ry *= d
                                }
                                // 2) Compute cx', cy'
                                var s = 0.0
                                var sa = sqr(rx) * sqr(ry) - sqr(rx) * sqr(y1p) - sqr(ry) * sqr(x1p)
                                val sb = sqr(rx) * sqr(y1p) + sqr(ry) * sqr(x1p)
                                if (sa < 0.0) sa = 0.0
                                if (sb > 0.0)
                                    s = sqrt(sa / sb)
                                if (fa == fs)
                                    s = -s
                                val cxp = s * rx * y1p / ry
                                val cyp = s * -ry * x1p / rx

                                // 3) Compute cx,cy from cx',cy'
                                val cx = (x1 + x2) / 2.0 + cosrx * cxp - sinrx * cyp
                                val cy = (y1 + y2) / 2.0 + sinrx * cxp + cosrx * cyp

                                // 4) Calculate theta1, and delta theta.
                                val ux = (x1p - cxp) / rx
                                val uy = (y1p - cyp) / ry
                                val vx = (-x1p - cxp) / rx
                                val vy = (-y1p - cyp) / ry
                                val a1 = vecang(1.0, 0.0, ux, uy)    // Initial angle
                                var da = vecang(ux, uy, vx, vy)        // Delta angle

                                //	if (vecrat(ux,uy,vx,vy) <= -1.0f) da = NSVG_PI;
                                //	if (vecrat(ux,uy,vx,vy) >= 1.0f) da = 0;

                                if (fs == 0 && da > 0)
                                    da -= 2 * PI
                                else if (fs == 1 && da < 0)
                                    da += 2 * PI


                                // Approximate the arc using cubic spline segments.
                                t[0] = cosrx
                                t[1] = sinrx
                                t[2] = -sinrx
                                t[3] = cosrx
                                t[4] = cx
                                t[5] = cy

                                // Split arc into max 90 degree segments.
                                // The loop assumes an iteration per end point (including start and end), this +1.
                                val ndivs = (abs(da) / (PI * 0.5) + 1.0).toInt()
                                val hda = (da / ndivs.toDouble()) / 2.0
                                var kappa = abs(4.0f / 3.0f * (1.0f - cos(hda)) / sin(hda))
                                if (da < 0.0f) kappa = -kappa

                                var ptanx = 0.0
                                var ptany = 0.0
                                var px = 0.0
                                var py = 0.0

                                for (i in 0..ndivs) {
                                    val a = a1 + da * (i.toDouble() / ndivs.toDouble())
                                    dx = cos(a)
                                    dy = sin(a)
                                    val x = xformPointX(dx*rx, dy*ry, t) // position
                                    val y = xformPointY( dx*rx, dy*ry, t) // position
                                    val tanx = xformVecX( -dy*rx * kappa, dx*ry * kappa, t) // tangent
                                    val tany = xformVecY(-dy*rx * kappa, dx*ry * kappa, t) // tangent
                                    if (i > 0) {
                                        cubicTo(px + ptanx, py + ptany, x - tanx, y - tany, x, y)
                                    }
                                    px = x
                                    py = y
                                    ptanx = tanx
                                    ptany = tany
                                }

                                lastX = x2
                                lastY = y2
                                //*cpx = x2;
                                //*cpy = y2;
                            }
                        }
                        'Z', 'z' -> close()
						else -> TODO("Unsupported command '$cmd' : Parsed: '${state.path.toSvgPathString()}', Original: '$d'")
					}
                    lastCmd = cmd
				}
                warningProcessor?.invoke("Parsed SVG Path: '${state.path.toSvgPathString()}'")
                warningProcessor?.invoke("Original SVG Path: '$d'")
                warningProcessor?.invoke("Points: ${state.path.getPoints()}")
				getBounds(bounds)
			}
		}

		if (xml.hasAttribute("stroke-width")) {
			lineWidth = xml.double("stroke-width", 1.0)
		}
		if (xml.hasAttribute("stroke")) {
			strokeStyle = parseFillStroke(c, xml.str("stroke"), bounds)
		}
		if (xml.hasAttribute("fill")) applyFill(c, xml.str("fill"), bounds)
		if (xml.hasAttribute("font-size")) {
            fontSize = parseSizeAsDouble(xml.str("font-size"))
		}
		if (xml.hasAttribute("font-family")) {
			font = fontRegistry[xml.str("font-family")]
		}
		if (xml.hasAttribute("style")) {
			applyStyle(c, SvgStyle.parse(xml.str("style"), warningProcessor), bounds)
		}
		if (xml.hasAttribute("transform")) {
			applyTransform(state, parseTransform(xml.str("transform")))
		}
		if (xml.hasAttribute("text-anchor")) {
			horizontalAlign = when (xml.str("text-anchor").toLowerCase().trim()) {
				"left" -> HorizontalAlign.LEFT
				"center", "middle" -> HorizontalAlign.CENTER
				"right", "end" -> HorizontalAlign.RIGHT
				else -> horizontalAlign
			}
		}
        if (xml.hasAttribute("alignment-baseline")) {
            verticalAlign = when (xml.str("alignment-baseline").toLowerCase().trim()) {
                "hanging" -> VerticalAlign.TOP
                "center", "middle" -> VerticalAlign.MIDDLE
                "baseline" -> VerticalAlign.BASELINE
                "bottom" -> VerticalAlign.BOTTOM
                else -> verticalAlign
            }
        }
		if (xml.hasAttribute("fill-opacity")) {
			globalAlpha = xml.double("fill-opacity", 1.0)
		}

		when (nodeName) {
			"g" -> {
				drawChildren(xml, c)
			}
            "text" -> {
                fillText(xml.text.trim(), xml.double("x") + xml.double("dx"), xml.double("y") + xml.double("dy"))
            }
		}

		c.fillStroke()
	}

    private fun sqr(v: Double) = v * v
    private fun vmag(x: Double, y: Double): Double {
        return sqrt(x * x + y * y)
    }

    private fun vecrat(ux: Double, uy: Double, vx: Double, vy: Double): Double {
        return (ux * vx + uy * vy) / (vmag(ux, uy) * vmag(vx, vy))
    }

    private fun vecang(ux: Double, uy: Double, vx: Double, vy: Double): Double {
        var r = vecrat(ux, uy, vx, vy)
        if (r < -1.0) r = -1.0
        if (r > 1.0) r = 1.0
        return (if (ux * vy < uy * vx) -1.0 else 1.0) * acos(r)
    }
    private fun xformPointX(x: Double, y: Double, t: DoubleArray) = x*t[0] + y*t[2] + t[4]
    private fun xformPointY(x: Double, y: Double, t: DoubleArray) = x*t[1] + y*t[3] + t[5]
    private fun xformVecX(x: Double, y: Double, t: DoubleArray) = x*t[0] + y*t[2]
    private fun xformVecY(x: Double, y: Double, t: DoubleArray): Double = x*t[1] + y*t[3]

    fun parseSizeAsDouble(size: String): Double {
        return size.filter { it !in 'a'..'z' && it !in 'A'..'Z' }.toDoubleOrNull() ?: 16.0
    }

	fun applyFill(c: Context2d, str: String, bounds: Rectangle) {
		c.fillStyle = parseFillStroke(c, str, bounds)
	}

	private fun applyTransform(state: Context2d.State, transform: Matrix) {
		//println("Apply transform $transform to $state")
		state.transform.premultiply(transform)
	}

	private fun applyStyle(c: Context2d, style: SvgStyle, bounds: Rectangle) {
		//println("Apply style $style to $c")
		for ((k, v) in style.styles) {
			//println("$k <-- $v")
			when (k) {
				"fill" -> applyFill(c, v, bounds)
				else -> warningProcessor?.invoke("Unsupported style $k in css")
			}
		}
	}

	fun parseTransform(str: String): Matrix {
		val tokens = SvgStyle.tokenize(str)
		val tr = ListReader(tokens)
		val out = Matrix()
		//println("Not implemented: parseTransform: $str: $tokens")
		while (tr.hasMore) {
			val id = tr.read().toLowerCase()
			val args = arrayListOf<String>()
			if (tr.peek() == "(") {
				tr.read()
				while (true) {
					if (tr.peek() == ")") {
						tr.read()
						break
					}
					if (tr.peek() == ",") {
						tr.read()
						continue
					}
					args += tr.read()
				}
			}
			val doubleArgs = args.map { it.toDoubleOrNull() ?: 0.0 }
			fun double(index: Int) = doubleArgs.getOrElse(index) { 0.0 }
			when (id) {
				"translate" -> out.pretranslate(double(0), double(1))
				"scale" -> out.prescale(double(0), double(1))
				"matrix" -> out.premultiply(double(0), double(1), double(2), double(3), double(4), double(5))
				else -> invalidOp("Unsupported transform $id : $args : $doubleArgs ($str)")
			}
			//println("ID: $id, args=$args")
		}
		return out
	}

	companion object {
		fun tokenizePath(str: String): List<PathToken> {
			val sr = StrReader(str)
			fun StrReader.skipSeparators() {
				skipWhile { it == ',' || it == ' ' || it == '\t' || it == '\n' || it == '\r' }
			}

			fun StrReader.readNumber(): Double {
				skipSeparators()
				var first = true
				val str = readWhile {
					if (first) {
						first = false
						it.isDigit() || it == '-' || it == '+'
					} else {
						it.isDigit() || it == '.'
					}
				}
				return if (str.isEmpty()) 0.0 else try {
					str.toDouble()
				} catch (e: Throwable) {
					e.printStackTrace()
					0.0
				}
			}

			val out = arrayListOf<PathToken>()
			while (sr.hasMore) {
				sr.skipSeparators()
				val c = sr.peekChar()
				out += if (c in '0'..'9' || c == '-' || c == '+') {
					PathTokenNumber(sr.readNumber())
				} else {
					PathTokenCmd(sr.readChar())
				}
			}
			return out
		}
	}

	interface PathToken
	data class PathTokenNumber(val value: Double) : PathToken
	data class PathTokenCmd(val id: Char) : PathToken

	data class SvgStyle(
		val styles: MutableMap<String, String> = hashMapOf()
	) {
		companion object {
			fun tokenize(str: String): List<String> {
				val sr = StrReader(str)
				val out = arrayListOf<String>()
				while (sr.hasMore) {
					while (true) {
						sr.skipSpaces()
						val id = sr.readWhile { it.isLetterOrUnderscore() || it.isNumeric || it == '-' || it == '#' }
						if (id.isNotEmpty()) {
							out += id
						} else {
							break
						}
					}
					if (sr.eof) break
					sr.skipSpaces()
					val symbol = sr.read()
					out += "$symbol"
				}
				return out
			}

			fun ListReader<String>.readId() = this.read()
			fun ListReader<String>.readColon() = expect(":")
			fun ListReader<String>.readExpression() = this.read()

			fun parse(str: String, warningProcessor: ((message: String) -> Unit)? = null): SvgStyle {
				val tokens = tokenize(str)
				val tr = ListReader(tokens)
				//println("Style: $str : $tokens")
				val style = SvgStyle()
				while (tr.hasMore) {
					val id = tr.readId()
					if (tr.eof) {
                        warningProcessor?.invoke("EOF. Parsing (ID='$id'): '$str', $tokens")
						break
					}
					tr.readColon()
					val rexpr = arrayListOf<String>()
					while (tr.hasMore && tr.peek() != ";") {
						rexpr += tr.readExpression()
					}
					style.styles[id.toLowerCase()] = rexpr.joinToString("")
					if (tr.hasMore) tr.expect(";")
					//println("$id --> $rexpr")
				}
				return style
			}
		}
	}
}

// @TODO: Move to korma
private inline fun VectorBuilder.rCubicTo(cx1: Number, cy1: Number, cx2: Number, cy2: Number, ax: Number, ay: Number, relative: Boolean) =
    if (relative) rCubicTo(cx1, cy1, cx2, cy2, ax, ay) else cubicTo(cx1, cy1, cx2, cy2, ax, ay)

private inline fun VectorBuilder.rQuadTo(cx: Number, cy: Number, ax: Number, ay: Number, relative: Boolean) =
    if (relative) rQuadTo(cx, cy, ax, ay) else quadTo(cx, cy, ax, ay)

private inline fun VectorBuilder.rLineTo(ax: Number, ay: Number, relative: Boolean) =
    if (relative) rLineTo(ax, ay) else lineTo(ax, ay)

private inline fun VectorBuilder.rMoveTo(ax: Number, ay: Number, relative: Boolean) =
    if (relative) rMoveTo(ax, ay) else moveTo(ax, ay)

private inline fun VectorBuilder.rMoveToH(ax: Number, relative: Boolean) =
    if (relative) rMoveToH(ax) else moveToH(ax)

private inline fun VectorBuilder.rMoveToV(ay: Number, relative: Boolean) =
    if (relative) rMoveToV(ay) else moveToV(ay)

private inline fun VectorBuilder.rLineToH(ax: Number, relative: Boolean) =
    if (relative) rLineToH(ax) else lineToH(ax)

private inline fun VectorBuilder.rLineToV(ay: Number, relative: Boolean) =
    if (relative) rLineToV(ay) else lineToV(ay)

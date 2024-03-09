package korlibs.image.vector.format

import korlibs.datastructure.*
import korlibs.io.util.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*
import kotlin.math.*

object SvgPath {
    fun parse(d: String, warningProcessor: ((message: String) -> Unit)? = null): VectorPath {
        val t = DoubleArray(6)

        val out = VectorPath()
        val tokens = tokenizePath(d)
        val tl = ListReader(tokens)

        fun dumpTokens() {
            for ((n, token) in tokens.withIndex()) warningProcessor?.invoke("- $n: $token")
        }

        fun isNextNumber(): Boolean = if (tl.hasMore) tl.peek() is SVG.PathTokenNumber else false
        fun readNumber(): Double {
            while (tl.hasMore) {
                val token = tl.read()
                if (token is SVG.PathTokenNumber) return token.value
                warningProcessor?.invoke("Invalid path (expected number but found $token) at ${tl.position - 1}")
                dumpTokens()
            }
            return 0.0
        }

        fun n(): Double = readNumber()
        fun nX(relative: Boolean): Double = if (relative) out.lastPos.x + readNumber() else readNumber()
        fun nY(relative: Boolean): Double = if (relative) out.lastPos.y + readNumber() else readNumber()

        fun readNextTokenCmd(): Char? {
            while (tl.hasMore) {
                val token = tl.read()
                if (token is SVG.PathTokenCmd) return token.id
                warningProcessor?.invoke("Invalid path (expected command but found $token) at ${tl.position - 1}")
                dumpTokens()
            }
            return null
        }

        //dumpTokens()

        out.moveTo(Point(0.0, 0.0)) // Supports relative positioning as first command
        var lastCX = 0.0
        var lastCY = 0.0
        var lastCmd = '-'

        while (tl.hasMore) {
            val cmd = readNextTokenCmd() ?: break
            if (cmd == '\u0000' || cmd.isWhitespaceFast()) continue
            val relative = cmd in 'a'..'z' // lower case
            var lastCurve = when (lastCmd) {
                'S', 'C', 'T', 'Q', 's', 'c', 't', 'q' -> true
                else -> false
            }
            when (cmd) {
                'M', 'm' -> {
                    out.rMoveTo(Point(n(), n()), relative)
                    while (isNextNumber()) out.rLineTo(Point(n(), n()), relative)
                }

                'L', 'l' -> while (isNextNumber()) out.rLineTo(Point(n(), n()), relative)
                'H', 'h' -> while (isNextNumber()) out.rLineToH(n(), relative)
                'V', 'v' -> while (isNextNumber()) out.rLineToV(n(), relative)
                'Q', 'q' -> while (isNextNumber()) {
                    val cx = nX(relative)
                    val cy = nY(relative)
                    val x2 = nX(relative)
                    val y2 = nY(relative)
                    lastCX = cx
                    lastCY = cy
                    out.quadTo(Point(cx, cy), Point(x2, y2))
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
                    out.cubicTo(Point(x1, y1), Point(x2, y2), Point(x, y))
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

                        val x1 = if (lastCurve) out.lastPos.x * 2 - lastCX else out.lastPos.x
                        val y1 = if (lastCurve) out.lastPos.y * 2 - lastCY else out.lastPos.y

                        lastCX = x2
                        lastCY = y2

                        out.cubicTo(Point(x1, y1), Point(x2, y2), Point(x, y))
                        lastCurve = true
                    }
                }

                'T', 't' -> {
                    var n = 0
                    while (isNextNumber()) {
                        val x2 = nX(relative)
                        val y2 = nY(relative)
                        val cx = if (lastCurve) out.lastPos.x * 2 - lastCX else out.lastPos.x
                        val cy = if (lastCurve) out.lastPos.y * 2 - lastCY else out.lastPos.y
                        //println("[$cmd]: $lastX, $lastY, $cx, $cy, $x2, $y2 :: $lastX - $lastCX :: $cx :: $lastCurve :: $lastCmd")
                        lastCX = cx
                        lastCY = cy
                        out.quadTo(Point(cx, cy), Point(x2, y2))
                        n++
                        lastCurve = true
                    }
                }

                'A', 'a' -> {
                    // @TODO: Use [Arc] class
                    val EPSILON = 1e-6

                    // Ported from nanosvg (https://github.com/memononen/nanosvg/blob/25241c5a8f8451d41ab1b02ab2d865b01600d949/src/nanosvg.h#L2067)
                    // Ported from canvg (https://code.google.com/p/canvg/)
                    var rx = readNumber().absoluteValue                // y radius
                    var ry = readNumber().absoluteValue                // x radius
                    val rotx = readNumber() / 180.0 * PI        // x rotation angle
                    val fa = if ((readNumber().absoluteValue) > EPSILON) 1 else 0    // Large arc
                    val fs = if ((readNumber().absoluteValue) > EPSILON) 1 else 0    // Sweep direction
                    val x1 = out.lastPos.x                            // start point
                    val y1 = out.lastPos.y                          // end point
                    val x2 = nX(relative)
                    val y2 = nY(relative)

                    var dx: Double = x1 - x2
                    var dy: Double = y1 - y2

                    val d = hypot(dx, dy)
                    if (d < EPSILON || rx < EPSILON || ry < EPSILON) {
                        // The arc degenerates to a line
                        out.lineTo(Point(x2, y2))
                    } else {
                        val sinrx = sin(rotx)
                        val cosrx = cos(rotx)

                        // Convert to center point parameterization.
                        // http://www.w3.org/TR/SVG11/implnote.html#ArcImplementationNotes
                        // 1) Compute x1', y1'
                        val x1p = cosrx * dx / 2.0 + sinrx * dy / 2.0
                        val y1p = -sinrx * dx / 2.0 + cosrx * dy / 2.0
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
                            da -= 2 * kotlin.math.PI
                        else if (fs == 1 && da < 0)
                            da += 2 * kotlin.math.PI


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
                            val x = xformPointX(dx * rx, dy * ry, t) // position
                            val y = xformPointY(dx * rx, dy * ry, t) // position
                            val tanx = xformVecX(-dy * rx * kappa, dx * ry * kappa, t) // tangent
                            val tany = xformVecY(-dy * rx * kappa, dx * ry * kappa, t) // tangent
                            if (i > 0) {
                                out.cubicTo(Point(px + ptanx, py + ptany), Point(x - tanx, y - tany), Point(x, y))
                            }
                            px = x
                            py = y
                            ptanx = tanx
                            ptany = tany
                        }

                        out.lastPos = Point(x2, y2)
                        //*cpx = x2;
                        //*cpy = y2;
                    }
                }

                'Z', 'z' -> out.close()
                else -> {
                    TODO("Unsupported command '$cmd' (${cmd.toInt()}) : Parsed: '${out.toSvgPathString()}', Original: '$d'")
                }
            }
            lastCmd = cmd
        }
        return out
    }

    private fun sqr(v: Double): Double = v * v
    private fun vmag(x: Double, y: Double): Double = sqrt(x * x + y * y)
    private fun vecrat(ux: Double, uy: Double, vx: Double, vy: Double): Double =
        (ux * vx + uy * vy) / (vmag(ux, uy) * vmag(vx, vy))

    private fun vecang(ux: Double, uy: Double, vx: Double, vy: Double): Double {
        var r = vecrat(ux, uy, vx, vy)
        if (r < -1.0) r = -1.0
        if (r > 1.0) r = 1.0
        return (if (ux * vy < uy * vx) -1.0 else 1.0) * acos(r)
    }

    private fun xformPointX(x: Double, y: Double, t: DoubleArray) = x * t[0] + y * t[2] + t[4]
    private fun xformPointY(x: Double, y: Double, t: DoubleArray) = x * t[1] + y * t[3] + t[5]
    private fun xformVecX(x: Double, y: Double, t: DoubleArray) = x * t[0] + y * t[2]
    private fun xformVecY(x: Double, y: Double, t: DoubleArray): Double = x * t[1] + y * t[3]

    // @TODO: Do not allocate PathToken!
    fun tokenizePath(str: String): List<SVG.PathToken> {
        val sr = StrReader(str)
        fun StrReader.skipSeparators() {
            skipWhile { it == ',' || it == ' ' || it == '\t' || it == '\n' || it == '\r' }
        }

        fun StrReader.readNumber(): Double {
            skipSeparators()
            var first = true
            var pointCount = 0
            val str = readWhile {
                if (it == '.') {
                    if (pointCount > 0) return@readWhile false
                    pointCount++
                }
                when {
                    first -> {
                        first = false
                        it.isDigit() || it == '-' || it == '+' || it == '.'
                    }

                    it == 'e' || it == 'E' -> {
                        first = true
                        true
                    }

                    else -> {
                        it.isDigit() || it == '.'
                    }
                }
            }
            return if (str.isEmpty()) 0.0 else try {
                str.toDouble()
            } catch (e: Throwable) {
                e.printStackTrace()
                0.0
            }
        }

        val out = arrayListOf<SVG.PathToken>()
        while (sr.hasMore) {
            sr.skipSeparators()
            val c = sr.peekChar()
            out += if (c in '0'..'9' || c == '-' || c == '+' || c == '.') {
                SVG.PathTokenNumber(sr.readNumber())
            } else {
                SVG.PathTokenCmd(sr.readChar())
            }
        }
        return out
    }

    fun toSvgPathString(path: VectorPath, separator: String = " ", decimalPlaces: Int = 1): String {
        val parts = arrayListOf<String>()

        fun Double.fixX() = this.toStringDecimal(decimalPlaces, skipTrailingZeros = true)
        fun Double.fixY() = this.toStringDecimal(decimalPlaces, skipTrailingZeros = true)
        fun Float.fixX() = this.toStringDecimal(decimalPlaces, skipTrailingZeros = true)
        fun Float.fixY() = this.toStringDecimal(decimalPlaces, skipTrailingZeros = true)

        path.visitCmds(
            moveTo = { (x, y) -> parts += "M${x.fixX()} ${y.fixY()}" },
            lineTo = { (x, y) -> parts += "L${x.fixX()} ${y.fixY()}" },
            quadTo = { (x1, y1), (x2, y2) -> parts += "Q${x1.fixX()} ${y1.fixY()}, ${x2.fixX()} ${y2.fixY()}" },
            cubicTo = { (x1, y1), (x2, y2), (x3, y3) -> parts += "C${x1.fixX()} ${y1.fixY()}, ${x2.fixX()} ${y2.fixY()}, ${x3.fixX()} ${y3.fixY()}" },
            close = { parts += "Z" }
        )
        return parts.joinToString("")
    }
}

fun VectorPath.toSvgPathString(separator: String = " ", decimalPlaces: Int = 1): String =
    SvgPath.toSvgPathString(this, separator, decimalPlaces)

fun VectorBuilder.pathSvg(path: String, m: Matrix = Matrix.NIL) {
    write(SvgPath.parse(path), m)
}

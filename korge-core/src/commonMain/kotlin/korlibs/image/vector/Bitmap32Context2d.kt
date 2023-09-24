package korlibs.image.vector

import korlibs.datastructure.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.paint.*
import korlibs.image.vector.rasterizer.*
import korlibs.math.*
import korlibs.math.geom.shape.*
import korlibs.math.geom.vector.*
import kotlin.math.*

// References:
// - https://github.com/memononen/nanosvg/blob/master/src/nanosvgrast.h
// - https://www.geeksforgeeks.org/scan-line-polygon-filling-using-opengl-c/
// - https://hackernoon.com/computer-graphics-scan-line-polygon-fill-algorithm-3cb47283df6
// - https://nothings.org/gamedev/rasterize/
// - https://www.mathematik.uni-marburg.de/~thormae/lectures/graphics1/code_v2/RasterPoly/index.html
class Bitmap32Context2d(val bmp: Bitmap32, val antialiasing: Boolean) : korlibs.image.vector.renderer.Renderer() {
    init {
        //check(bmp.premultiplied) { error("Can't get a context2d from a non-premultiplied Bitmap32") }
    }

	override val width: Int get() = bmp.width
	override val height: Int get() = bmp.height

    val bounds = bmp.bounds.float
    val rasterizer = Rasterizer()
	val colorFiller = ColorFiller()
	val gradientFiller = GradientFiller()
	val bitmapFiller = BitmapFiller()
    val scanlineWriter = ScanlineWriter()
    //private val tempPath = VectorPath(winding = Winding.NON_ZERO)
    private val tempPath = VectorPath(winding = Winding.EVEN_ODD)
    private val tempFillStrokeTemp = StrokeToFill()

    override fun renderFinal(state: Context2d.State, fill: Boolean, winding: Winding?) {
		//println("RENDER")
		val style = if (fill) state.fillStyle else state.strokeStyle
		val filler = when (style) {
			is NonePaint -> NoneFiller
			is ColorPaint -> colorFiller.set(style, state)
			is GradientPaint -> gradientFiller.set(style, state)
			is BitmapPaint -> bitmapFiller.set(style, state)
			else -> TODO()
		}

        scanlineWriter.compositeMode = state.globalCompositeOperation
        rasterizer.reset()
        rasterizer.debug = debug

        val doingStroke = !fill
        val fillPath = if (fill) {
            //rasterizer.scale = 1
            state.path
        } else {
            //rasterizer.scale = 1
            //state.path.getFilledStroke(state.lineWidth, state.startLineCap, state.endLineCap, state.lineJoin, rasterizer.scale)
            tempPath.clear()
            state.path.strokeToFill(
                state.scaledLineWidth,
                state.lineJoin, state.startLineCap, state.endLineCap,
                state.miterLimit,
                temp = tempFillStrokeTemp, outFill = tempPath,
                lineDash = state.lineDash,
                lineDashOffset = state.lineDashOffset,
            )
        }

        fun flush() {
            if (rasterizer.path.isNotEmpty()) {
                //rasterizer.strokeWidth = state.lineWidth
                //rasterizer.quality = if (antialiasing) 5 else 1
                rasterizer.quality = if (antialiasing) 4 else 1
                //rasterizer.quality = if (antialiasing) 2 else 1
                scanlineWriter.filler = filler
                scanlineWriter.globalAlpha = state.globalAlpha.toFloat()
                scanlineWriter.reset()
                rasterizer.rasterizeFill(bounds, winding = winding ?: fillPath.winding) { x0, x1, y ->
                    scanlineWriter.select(x0, x1, y)
                }
                scanlineWriter.flush()
                rasterizer.path.reset()
            }
        }

        if (state.clip != null) {
            rasterizer.clip.winding = state.clip!!.winding
            state.clip!!.emitPoints2(flush = {
                if (it) rasterizer.clip.close()
            }, emit = { (x, y), move ->
                rasterizer.clip.add(x, y, move)
            })
        }

        rasterizer.path.winding = winding ?: state.path!!.winding
        //rasterizer.path.winding = Winding.NON_ZERO
        //println("------------- $bmp : ${state.path!!.winding}")
        fillPath.emitPoints2(flush = {
            if (it) {
                //println("CLOSE")
                rasterizer.path.close()
            }
        }, emit = { (x, y), move ->
            // When rendering strokes we might want to do each stroke at a time to prevent artifacts.
            // But on fills this would produce issues when for rendering 'o' that are two circles one inside another.
            if (doingStroke) {
                //rasterizer.path.close()
                if (move) { flush() }
            }
            //println("POINT: $x, $y")
            rasterizer.path.add(x, y, move)
        })
        flush()
    }

    class SegmentHandler {
        val xmin = intArrayListOf()
        val xmax = intArrayListOf()
        val size get() = xmin.size

        init {
            reset()
        }

        fun reset() {
            xmin.clear()
            xmax.clear()
        }

        private fun overlaps(a0: Int, a1: Int, b0: Int, b1: Int): Boolean {
            val min = min(a0, a0)
            val max = max(a1, a1)
            val maxMinor = max(a0, b0)
            val minMajor = min(a1, b1)
            return (maxMinor in min..max) || (minMajor in min..max)
        }

        fun add(x0: Int, x1: Int) {
            // @TODO: Maybe we can optimize this if we keep segments in order
            for (n in 0 until size) {
                val xmin = this.xmin.getAt(n)
                val xmax = this.xmax.getAt(n)
                if (overlaps(xmin, xmax, x0, x1)) {
                    this.xmin[n] = min(x0, xmin)
                    this.xmax[n] = max(x1, xmax)
                    return
                }
            }
            // Only works if done from left to right
            //if (size > 0 && overlaps(xmin[size - 1], xmax[size - 1], x0, x1)) {
            //    xmin[size - 1] = min(x0, xmin[size - 1])
            //    xmax[size - 1] = max(x0, xmax[size - 1])
            //} else {
            xmin.add(x0)
            xmax.add(x1)
            //}
        }

        inline fun forEachFast(block: (x0: Int, x1: Int) -> Unit) {
            for (n in 0 until size) {
                block(xmin.getAt(n), xmax.getAt(n))
            }
        }
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    inner class ScanlineWriter {
        var compositeMode: CompositeOperation = CompositeOperation.DEFAULT
        var filler: BaseFiller = NoneFiller
        var ny0 = -1
        var ny = -1
        val size get() = bmp.width
        val width1 get() = bmp.width - 1
        val alpha = FloatArray(size)
        val hitbits = IntArray(size)
        val origin = RgbaPremultipliedArray(size)
        val color = RgbaPremultipliedArray(size)
        val segments = SegmentHandler()
        var subRowCount = 0
        var globalAlpha = 1f
        fun reset() {
            segments.forEachFast { xmin, xmax ->
                alpha.fill(0f, xmin, xmax + 1)
                hitbits.fill(0, xmin, xmax + 1)
            }
            subRowCount = 0
            segments.reset()
        }
        fun select(x0: Int, x1: Int, y0: Int) {
            if (width1 < 1) return
            val x0 = x0.coerceIn(0, width1 * RastScale.RAST_FIXED_SCALE)
            val x1 = x1.coerceIn(0, width1 * RastScale.RAST_FIXED_SCALE)
            val a = x0 / RastScale.RAST_FIXED_SCALE
            val b = x1 / RastScale.RAST_FIXED_SCALE
            val y = y0 / RastScale.RAST_FIXED_SCALE
            val i0 = a.coerceIn(0, width1)
            val i1 = b.coerceIn(0, width1)
            val i0m = x0 % RastScale.RAST_FIXED_SCALE
            val i1m = x1 % RastScale.RAST_FIXED_SCALE

            if (ny != y) {
                if (y >= 0) flush()
                ny = y
                reset()
            }
            if (ny0 != y0) {
                ny0 = y0
                subRowCount++
            }
            if (i1 > i0) {
                segments.add(i0, i1)
                //println("ROW[$y0]: $x0,$x1")
                //println("i1=$i1, x1=$x1")
                put(i0, 1f - i0m.toFloat() / RastScale.RAST_FIXED_SCALE)
                for (x in i0 + 1 until i1) put(x, 1f)
                if (i1m != 0) put(i1, i1m.toFloat() / RastScale.RAST_FIXED_SCALE)
                //alphaCount++
            }
        }

        fun put(x: Int, ratio: Float) {
            val mask = 1 shl subRowCount
            if ((hitbits[x] and mask) == 0) {
                hitbits[x] = hitbits[x] or mask
                alpha[x] += ratio
            }
        }

        fun flush() {
            if (ny !in 0 until bmp.height) return
            val galpha = globalAlpha.clamp01().toFloat()
            val scale = (1f / subRowCount) * galpha
            segments.forEachFast { xmin, xmax ->
                val x = xmin
                val count = xmax - xmin + 1
                filler.fill(color, 0, xmin, xmax, ny)
                for (n in xmin..xmax) alpha[n] *= scale
                scale(color, xmin, alpha, xmin, count)
                val index = bmp.index(0, ny) + x
                when {
                    bmp.premultiplied -> korlibs.memory.arraycopy(bmp.ints, index, origin.ints, x, count)
                    else -> premultiply(RgbaArray(bmp.ints), index, origin, x, count)
                }
                //for (n in xmin..xmax) color[n] = color[n].scaled(galpha)
                compositeMode.blend(origin, x, color, x, count)
                when {
                    bmp.premultiplied -> korlibs.memory.arraycopy(origin.ints, x, bmp.ints, index, count)
                    else -> depremultiply(origin, x, RgbaArray(bmp.ints), index, count)
                }
            }
        }
    }
}

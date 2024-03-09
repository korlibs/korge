package korlibs.image.bitmap

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.image.color.*
import korlibs.image.format.*
import korlibs.io.file.*
import korlibs.io.util.*
import korlibs.math.*
import korlibs.math.geom.*
import kotlin.math.*

data class NinePatchInfo constructor(
	val xranges: List<Pair<Boolean, IntRange>>,
	val yranges: List<Pair<Boolean, IntRange>>,
	val width: Int,
	val height: Int,
    val content: BmpSlice? = null
) {
	constructor(
		width: Int, height: Int,
		left: Int, top: Int, right: Int, bottom: Int,
        content: BmpSlice? = null
	) : this(
		listOf(false to (0 until left), true to (left until right), false to (right until width)),
		listOf(false to (0 until top), true to (top until bottom), false to (bottom until height)),
		width, height, content
	)

	class AxisSegment(val scaled: Boolean, val range: IntRange) {
		val fixed: Boolean get() = !scaled
		val length: Int get() = range.length

		fun computedLength(axis: AxisInfo, boundsLength: Int): Double {
			val scale = (boundsLength.toDouble() / axis.totalLen.toDouble()).clamp(0.0, 1.0)
			return if (fixed) {
				length.toDouble() * scale
			} else {
				val variableSize = (boundsLength - (axis.fixedLen * scale))
				variableSize.toDouble() * (length.toDouble() / axis.scaledLen.toDouble())
			}
		}
	}

	data class AxisInfo(val ranges: List<Pair<Boolean, IntRange>>, val totalLen: Int) {
		val segments = ranges.map { AxisSegment(it.first, it.second) }.toFastList()
		val fixedLen = max(1, segments.filter { it.fixed }.sumOf { it.length })
		val scaledLen = max(1, segments.filter { it.scaled }.sumOf { it.length })
	}

	val xaxis = AxisInfo(xranges, width)
	val yaxis = AxisInfo(yranges, height)

	val xsegments = xaxis.segments
	val ysegments = yaxis.segments

	val fixedWidth = xaxis.fixedLen
	val fixedHeight = yaxis.fixedLen

    val totalSegments get() = xsegments.size * ysegments.size

	val scaledWidth = xaxis.scaledLen
	val scaledHeight = yaxis.scaledLen

	class Segment(val info: NinePatchInfo, val rect: RectangleInt, val x: AxisSegment, val y: AxisSegment) : Extra by Extra.Mixin() {
		val scaleX: Boolean = x.scaled
		val scaleY: Boolean = y.scaled

        val bmpSlice = info.content?.slice(this.rect)
        val bmp by lazy { bmpSlice?.extract() }
	}

	val segments = ysegments.map { y ->
		xsegments.map { x ->
			Segment(
                this,
				RectangleInt.fromBounds(x.range.first, y.range.first, x.range.last + 1, y.range.last + 1),
				x, y
			)
		}.toFastList()
	}.toFastList()

    //init { println("Created NinePatchInfo") }

    fun computeScale(
        bounds: RectangleInt,
        new: Boolean = true,
        callback: (segment: Segment, x: Int, y: Int, width: Int, height: Int) -> Unit
    ) = if (new) computeScaleNew(bounds, callback) else computeScaleOld(bounds, callback)

	// Can be reused for textures using AG
	fun computeScaleOld(
        bounds: RectangleInt,
        callback: (segment: Segment, x: Int, y: Int, width: Int, height: Int) -> Unit
	) {
		//println("scaleFixed=($scaleFixedX,$scaleFixedY)")
		var ry = 0
		for ((yindex, y) in ysegments.withIndex()) {
			val segHeight = y.computedLength(this.yaxis, bounds.height).toInt()
			var rx = 0
			for ((xindex, x) in xsegments.withIndex()) {
				val segWidth = x.computedLength(this.xaxis, bounds.width).toInt()

				val seg = segments[yindex][xindex]
				val segLeft = (rx + bounds.left).toInt()
				val segTop = (ry + bounds.top).toInt()

				//println("($x,$y):($segWidth,$segHeight)")
				callback(seg, segLeft, segTop, segWidth.toInt(), segHeight.toInt())

				rx += segWidth
			}
			ry += segHeight
		}
	}

    private val xComputed = IntArray(64)
    private val yComputed = IntArray(64)

    fun computeScaleNew(
        bounds: RectangleInt,
        callback: (segment: NinePatchInfo.Segment, x: Int, y: Int, width: Int, height: Int) -> Unit
    ) {
        //println("scaleFixed=($scaleFixedX,$scaleFixedY)")

        ysegments.fastForEachWithIndex { index, _ -> yComputed[index] = Int.MAX_VALUE }
        xsegments.fastForEachWithIndex { index, _ -> xComputed[index] = Int.MAX_VALUE }

        ysegments.fastForEachWithIndex { yindex, y ->
            val segHeight = y.computedLength(this.yaxis, bounds.height)
            xsegments.fastForEachWithIndex { xindex, x ->
                val segWidth = x.computedLength(this.xaxis, bounds.width)
                if (x.fixed && y.fixed) {
                    val xScale = segWidth / x.length.toDouble()
                    val yScale = segHeight / y.length.toDouble()
                    val minScale = min(xScale, yScale)
                    xComputed[xindex] = min(xComputed[xindex], (x.length * minScale).toInt())
                    yComputed[yindex] = min(yComputed[yindex], (y.length * minScale).toInt())
                } else {
                    xComputed[xindex] = min(xComputed[xindex], segWidth.toInt())
                    yComputed[yindex] = min(yComputed[yindex], segHeight.toInt())
                }
            }
        }

        val denormalizedWidth = xComputed.sum()
        val denormalizedHeight = yComputed.sum()
        val denormalizedScaledWidth = xsegments.mapIndexed { index, it -> if (it.scaled) xComputed[index] else 0 }.sum()
        val denormalizedScaledHeight = ysegments.mapIndexed { index, it -> if (it.scaled) yComputed[index] else 0 }.sum()
        val xScaledRatio = if (denormalizedWidth > 0) denormalizedScaledWidth.toDouble() / denormalizedWidth.toDouble() else 1.0
        val yScaledRatio = if (denormalizedWidth > 0) denormalizedScaledHeight.toDouble() / denormalizedHeight.toDouble() else 1.0

        for (n in 0 until 2) {
            val segments = if (n == 0) ysegments else xsegments
            val computed = if (n == 0) yComputed else xComputed
            val denormalizedScaledLen = if (n == 0) denormalizedScaledHeight else denormalizedScaledWidth
            val side = if (n == 0) bounds.height else bounds.width
            val scaledRatio = if (n == 0) yScaledRatio else xScaledRatio
            val scaledSide = side * scaledRatio

            segments.fastForEachWithIndex { index, v ->
                if (v.scaled) {
                    computed[index] = (scaledSide * (computed[index].toDouble() / denormalizedScaledLen.toDouble())).toInt()
                }
            }
        }

        val xRemaining = bounds.width - xComputed.sum()
        val yRemaining = bounds.height - yComputed.sum()
        val xScaledFirst = xsegments.indexOfFirst { it.scaled }
        val yScaledFirst = ysegments.indexOfFirst { it.scaled }
        if (xRemaining > 0 && xScaledFirst >= 0) xComputed[xScaledFirst] += xRemaining
        if (yRemaining > 0 && yScaledFirst >= 0) yComputed[yScaledFirst] += yRemaining

        var ry = 0
        for (yindex in ysegments.indices) {
            val segHeight = yComputed[yindex].toInt()
            var rx = 0
            for (xindex in xsegments.indices) {
                val segWidth = xComputed[xindex].toInt()

                val seg = segments[yindex][xindex]
                val segLeft = (rx + bounds.left).toInt()
                val segTop = (ry + bounds.top).toInt()

                //println("($x,$y):($segWidth,$segHeight)")
                callback(seg, segLeft, segTop, segWidth.toInt(), segHeight.toInt())

                rx += segWidth
            }
            ry += segHeight
        }
    }
}

open class NinePatchBmpSlice(
    val content: BmpSlice,
    val info: NinePatchInfo,
    val bmpSlice: BmpSlice = content
) {
    companion object {
        //val DUMMY = createSimple(Bitmap32(3, 3).slice(), 1, 1, 2, 2)

        fun createSimple(bmp: BmpSlice, left: Int, top: Int, right: Int, bottom: Int): NinePatchBmpSlice {
            return NinePatchBmpSlice(bmp, NinePatchInfo(bmp.width, bmp.height, left, top, right, bottom, bmp))
        }

        operator fun invoke(bmp: Bitmap) = invoke(bmp.slice())
        operator fun invoke(bmpSlice: BmpSlice): NinePatchBmpSlice {
            val content = bmpSlice.sliceWithBounds(1, 1, bmpSlice.width - 1, bmpSlice.height - 1)
            return NinePatchBmpSlice(
                content = content,
                info = run {
                    val topPixels = RgbaArray(bmpSlice.readPixelsUnsafe(0, 0, bmpSlice.width, 1))
                    val leftPixels = RgbaArray(bmpSlice.readPixelsUnsafe(0, 0, 1, bmpSlice.height))
                    NinePatchInfo(
                        (1 until bmpSlice.width - 1).computeRle { topPixels[it].a != 0 },
                        (1 until bmpSlice.height - 1).computeRle { leftPixels[it].a != 0 },
                        content.width, content.height, content
                    )
                },
                bmpSlice = bmpSlice
            )
        }
    }

	val width: Int get() = bmpSlice.width
	val height: Int get() = bmpSlice.height
    @Deprecated("", ReplaceWith("widthD")) val dwidth get() = widthD
	@Deprecated("", ReplaceWith("heightD")) val dheight get() = heightD
    val widthD get() = width.toDouble()
    val heightD get() = height.toDouble()
    val widthF get() = width.toFloat()
    val heightF get() = height.toFloat()

    fun getSegmentBmpSlice(segment: NinePatchInfo.Segment) = segment.bmpSlice!!

	fun <T : Bitmap> drawTo(
        other: T,
        bounds: RectangleInt,
        antialiased: Boolean = true,
        drawRegions: Boolean = false
	): T {
		other.context2d(antialiased) {
			info.computeScale(bounds) { seg, segLeft, segTop, segWidth, segHeight ->
				drawImage(seg.bmp!!, Point(segLeft, segTop), Size(segWidth, segHeight))
				if (drawRegions) {
					stroke(Colors.RED) { rect(segLeft, segTop, segWidth, segHeight) }
				}
			}
		}
		return other
	}

	fun renderedNative(width: Int, height: Int, antialiased: Boolean = true, drawRegions: Boolean = false): NativeImage = drawTo(
        NativeImage(width, height),
        //Bitmap32(width, height),
        RectangleInt(0, 0, width, height),
        antialiased = antialiased,
        drawRegions = drawRegions
    )

    fun rendered(width: Int, height: Int, antialiased: Boolean = true, drawRegions: Boolean = false): Bitmap32 = renderedNative(width, height, antialiased, drawRegions).toBMP32IfRequired()
}

typealias NinePatchBitmap32 = NinePatchBmpSlice

fun BmpSlice.asNinePatch() = NinePatchBmpSlice(this)
fun Bitmap.asNinePatch() = NinePatchBitmap32(this.toBMP32IfRequired())

fun BmpSlice.asNinePatchSimpleRatio(left: Float, top: Float, right: Float, bottom: Float) = this.asNinePatchSimple(
    (left * width).toInt(), (top * height).toInt(),
    (right * width).toInt(), (bottom * height).toInt()
)
fun BmpSlice.asNinePatchSimpleRatio(left: Double, top: Double, right: Double, bottom: Double) = this.asNinePatchSimple(
    (left * width).toInt(), (top * height).toInt(),
    (right * width).toInt(), (bottom * height).toInt()
)
fun BmpSlice.asNinePatchSimple(left: Int, top: Int, right: Int, bottom: Int) = NinePatchBmpSlice.createSimple(this, left, top, right, bottom)
fun Bitmap.asNinePatchSimple(left: Int, top: Int, right: Int, bottom: Int) = this.slice().asNinePatchSimple(left, top, right, bottom)

suspend fun VfsFile.readNinePatch(props: ImageDecodingProps = ImageDecodingProps.DEFAULT): NinePatchBmpSlice = NinePatchBitmap32(readBitmap(props).toBMP32())
suspend fun VfsFile.readNinePatch(format: ImageFormat): NinePatchBmpSlice = readNinePatch(ImageDecodingProps.DEFAULT.copy(format = format))

private inline fun <T, R : Any> Iterable<T>.computeRle(callback: (T) -> R): List<Pair<R, IntRange>> {
    var first = true
    var pos = 0
    var startpos = 0
    lateinit var lastRes: R
    val out = FastArrayList<Pair<R, IntRange>>()
    for (it in this) {
        val current = callback(it)
        if (!first) {
            if (current != lastRes) {
                out += lastRes to (startpos until pos)
                startpos = pos
            }
        }
        lastRes = current
        first = false
        pos++
    }
    if (startpos != pos) {
        out += lastRes to (startpos until pos)
    }
    return out
}

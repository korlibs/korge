package korlibs.math.geom.bezier

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.math.annotations.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*
import korlibs.memory.*
import kotlin.jvm.*

@JvmName("ListCurves_toCurves")
fun List<Curves>.toCurves(closed: Boolean = this.last().closed) = Curves(this.flatMap { it.beziers }, closed)
@JvmName("ListCurve_toCurves")
fun List<Bezier>.toCurves(closed: Boolean) = Curves(this, closed)

fun Curves.toCurves(closed: Boolean) = this
fun Bezier.toCurves(closed: Boolean) = Curves(listOf(this), closed)

data class Curves(val beziers: List<Bezier>, val closed: Boolean) : Curve, Extra by Extra.Mixin() {
    var assumeConvex: Boolean = false

    /**
     * All [beziers] in this set are contiguous
     */
    val contiguous by lazy {
        for (n in 1 until beziers.size) {
            val curr = beziers[n - 1]
            val next = beziers[n]
            if (!curr.points.last.isAlmostEquals(next.points.first)) return@lazy false
            //if (!curr.points.lastX.isAlmostEquals(next.points.firstX)) return@lazy false
            //if (!curr.points.lastY.isAlmostEquals(next.points.firstY)) return@lazy false
        }
        return@lazy true
    }

    constructor(vararg curves: Bezier, closed: Boolean = false) : this(curves.toList(), closed)

    override val order: Int get() = -1

    data class CurveInfo(
        val index: Int,
        val curve: Bezier,
        val startLength: Float,
        val endLength: Float,
        val bounds: Rectangle,
    ) {
        fun contains(length: Float): Boolean = length in startLength..endLength

        val length: Float get() = endLength - startLength
    }

    val infos: List<CurveInfo> by lazy {
        var pos = 0f
        beziers.mapIndexed { index, curve ->
            val start = pos
            pos += curve.length
            CurveInfo(index, curve, start, pos, curve.getBounds())
        }

    }
    override val length: Float by lazy { infos.sumOfFloat { it.length } }

    val CurveInfo.startRatio: Float get() = this.startLength / this@Curves.length
    val CurveInfo.endRatio: Float get() = this.endLength / this@Curves.length

    override fun getBounds(): Rectangle {
        var bb = BoundsBuilder()
        infos.fastForEach { bb += it.bounds }
        return bb.bounds
    }

    @PublishedApi
    internal fun findInfo(t: Float): CurveInfo {
        val pos = t * length
        val index = infos.binarySearch {
            when {
                it.contains(pos) -> 0
                it.endLength < pos -> -1
                else -> +1
            }
        }
        if (t < 0.0) return infos.first()
        if (t > 1.0) return infos.last()
        return infos.getOrNull(index) ?: error("OUTSIDE")
    }

    @PublishedApi
    internal inline fun <T> findTInCurve(t: Float, block: (info: CurveInfo, ratioInCurve: Float) -> T): T {
        val pos = t * length
        val info = findInfo(t)
        val posInCurve = pos - info.startLength
        val ratioInCurve = posInCurve / info.length
        return block(info, ratioInCurve)
    }

    override fun calc(t: Float): Point =
        findTInCurve(t) { info, ratioInCurve -> info.curve.calc(ratioInCurve) }

    override fun normal(t: Float): Point =
        findTInCurve(t) { info, ratioInCurve -> info.curve.normal(ratioInCurve) }

    override fun tangent(t: Float): Point =
        findTInCurve(t) { info, ratioInCurve -> info.curve.tangent(ratioInCurve) }

    override fun ratioFromLength(length: Float): Float {
        if (length <= 0f) return 0f
        if (length >= this.length) return 1f

        val curveIndex = infos.binarySearch {
            when {
                it.endLength < length -> -1
                it.startLength > length -> +1
                else -> 0
            }
        }
        val index = if (curveIndex < 0) -curveIndex + 1 else curveIndex
        if (curveIndex < 0) {
            //infos.fastForEach { println("it=$it") }
            //println("length=${this.length}, requestedLength = $length, curveIndex=$curveIndex")
            return Float.NaN
        } // length not in curve!
        val info = infos[index]
        val lengthInCurve = length - info.startLength
        val ratioInCurve = info.curve.ratioFromLength(lengthInCurve)
        return ratioInCurve.convertRange(0f, 1f, info.startRatio, info.endRatio)
    }

    fun splitLeftByLength(len: Float): Curves = splitLeft(ratioFromLength(len))
    fun splitRightByLength(len: Float): Curves = splitRight(ratioFromLength(len))
    fun splitByLength(len0: Float, len1: Float): Curves = split(ratioFromLength(len0), ratioFromLength(len1))

    fun splitLeft(t: Float): Curves = split(0f, t)
    fun splitRight(t: Float): Curves = split(t, 1f)

    fun split(t0: Float, t1: Float): Curves {
        if (t0 > t1) return split(t1, t0)
        check(t0 <= t1)

        if (t0 == t1) return Curves(emptyList(), closed = false)

        return Curves(findTInCurve(t0) { info0, ratioInCurve0 ->
            findTInCurve(t1) { info1, ratioInCurve1 ->
                if (info0.index == info1.index) {
                    listOf(info0.curve.split(ratioInCurve0, ratioInCurve1).curve)
                } else {
                    buildList {
                        if (ratioInCurve0 != 1f) add(info0.curve.splitRight(ratioInCurve0).curve)
                        for (index in info0.index + 1 until info1.index) add(infos[index].curve)
                        if (ratioInCurve1 != 0f) add(info1.curve.splitLeft(ratioInCurve1).curve)
                    }
                }
            }
        }, closed = false)
    }

    fun roundDecimalPlaces(places: Int): Curves = Curves(beziers.map { it.roundDecimalPlaces(places) }, closed)
}

fun Curve.toVectorPath(out: VectorPath = VectorPath()): VectorPath = listOf(this).toVectorPath(out)

fun List<Curve>.toVectorPath(out: VectorPath = VectorPath()): VectorPath {
    var first = true

    fun bezier(bezier: Bezier) {
        val points = bezier.points
        if (first) {
            out.moveTo(points.first)
            first = false
        }
        when (bezier.order) {
            1 -> out.lineTo(points[1])
            2 -> out.quadTo(points[1], points[2])
            3 -> out.cubicTo(points[1], points[2], points[3])
            else -> TODO()
        }
    }

    fastForEach { curves ->
        when (curves) {
            is Curves -> {
                curves.beziers.fastForEach { bezier(it) }
                if (curves.closed) out.close()
            }
            is Bezier -> bezier(curves)
            else -> TODO()
        }
    }

    return out
}

inline fun List<Curves>.fastForEachBezier(block: (Bezier) -> Unit) {
    this.fastForEach { it.beziers.fastForEach(block) }
}

@KormaExperimental
@KormaMutableApi
fun Curves.toNonCurveSimplePointList(out: PointArrayList = PointArrayList()): PointList? {
    val curves = this
    val beziers = curves.beziers//.flatMap { it.toSimpleList() }.map { it.curve }
    val epsilon = 0.0001f
    beziers.fastForEach { bezier ->
        if (bezier.inflections().isNotEmpty()) return null
        val points = bezier.points
        points.fastForEach { p ->
            if (out.isEmpty() || !out.last.isAlmostEquals(p, epsilon)) {
                out.add(p)
            }
        }
        //println("bezier=$bezier")
        //out.add(points, 0, points.size - 1)
    }
    if (out.last.isAlmostEquals(out.first, epsilon)) {
        out.removeAt(out.size - 1)
    }
    return out
}

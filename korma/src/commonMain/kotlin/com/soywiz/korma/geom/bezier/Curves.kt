package com.soywiz.korma.geom.bezier

import com.soywiz.kds.Extra
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korma.annotations.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.vector.*
import com.soywiz.korma.interpolation.*
import com.soywiz.korma.math.isAlmostEquals
import kotlin.jvm.JvmName

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
            if (!curr.points.lastX.isAlmostEquals(next.points.firstX)) return@lazy false
            if (!curr.points.lastY.isAlmostEquals(next.points.firstY)) return@lazy false
        }
        return@lazy true
    }

    constructor(vararg curves: Bezier, closed: Boolean = false) : this(curves.toList(), closed)

    override val order: Int get() = -1

    data class CurveInfo(
        val index: Int,
        val curve: Bezier,
        val startLength: Double,
        val endLength: Double,
        val bounds: MRectangle,
    ) {
        fun contains(length: Double): Boolean = length in startLength..endLength

        val length: Double get() = endLength - startLength
    }

    val infos: List<CurveInfo> by lazy {
        var pos = 0.0
        beziers.mapIndexed { index, curve ->
            val start = pos
            pos += curve.length
            CurveInfo(index, curve, start, pos, curve.getBounds())
        }

    }
    override val length: Double by lazy { infos.sumOf { it.length } }
    private val bb = BoundsBuilder()

    val CurveInfo.startRatio: Ratio get() = Ratio(this.startLength, this@Curves.length)
    val CurveInfo.endRatio: Ratio get() = Ratio(this.endLength, this@Curves.length)

    override fun getBounds(target: MRectangle): MRectangle {
        bb.reset()
        infos.fastForEach { bb.addEvenEmpty(it.bounds) }
        return bb.getBounds(target)
    }

    @PublishedApi
    internal fun findInfo(t: Ratio): CurveInfo {
        val pos = t.valueD * length
        val index = infos.binarySearch {
            when {
                it.contains(pos) -> 0
                it.endLength < pos -> -1
                else -> +1
            }
        }
        if (t < Ratio.ZERO) return infos.first()
        if (t > Ratio.ONE) return infos.last()
        return infos.getOrNull(index) ?: error("OUTSIDE")
    }

    @PublishedApi
    internal inline fun <T> findTInCurve(t: Ratio, block: (info: CurveInfo, ratioInCurve: Ratio) -> T): T {
        val pos = t.valueD * length
        val info = findInfo(t)
        return block(info, Ratio(pos - info.startLength, info.length))
    }

    override fun calc(t: Ratio): Point =
        findTInCurve(t) { info, ratioInCurve -> info.curve.calc(ratioInCurve) }

    override fun normal(t: Ratio): Point =
        findTInCurve(t) { info, ratioInCurve -> info.curve.normal(ratioInCurve) }

    override fun tangent(t: Ratio): Point =
        findTInCurve(t) { info, ratioInCurve -> info.curve.tangent(ratioInCurve) }

    override fun ratioFromLength(length: Double): Ratio {
        if (length <= 0.0) return Ratio.ZERO
        if (length >= this.length) return Ratio.ONE

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
            return Ratio.NaN
        } // length not in curve!
        val info = infos[index]
        val lengthInCurve = length - info.startLength
        val ratioInCurve = info.curve.ratioFromLength(lengthInCurve)
        return ratioInCurve.convertToRange(info.startRatio, info.endRatio)
    }

    fun splitLeftByLength(len: Double): Curves = splitLeft(ratioFromLength(len))
    fun splitRightByLength(len: Double): Curves = splitRight(ratioFromLength(len))
    fun splitByLength(len0: Double, len1: Double): Curves = split(ratioFromLength(len0), ratioFromLength(len1))

    fun splitLeft(t: Ratio): Curves = split(Ratio.ZERO, t)
    fun splitRight(t: Ratio): Curves = split(t, Ratio.ONE)

    fun split(t0: Ratio, t1: Ratio): Curves {
        if (t0 > t1) return split(t1, t0)
        check(t0 <= t1)

        if (t0 == t1) return Curves(emptyList(), closed = false)

        return Curves(findTInCurve(t0) { info0, ratioInCurve0 ->
            findTInCurve(t1) { info1, ratioInCurve1 ->
                if (info0.index == info1.index) {
                    listOf((info0.curve as Bezier).split(ratioInCurve0, ratioInCurve1).curve)
                } else {
                    buildList {
                        if (ratioInCurve0 != Ratio.ONE) add((info0.curve as Bezier).splitRight(ratioInCurve0).curve)
                        for (index in info0.index + 1 until info1.index) add(infos[index].curve)
                        if (ratioInCurve1 != Ratio.ZERO) add((info1.curve as Bezier).splitLeft(ratioInCurve1).curve)
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
            1 -> out.lineTo(points.get(1))
            2 -> out.quadTo(points.get(1), points.get(2))
            3 -> out.cubicTo(points.get(1), points.get(2), points.get(3))
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

@KormaExperimental
@KormaMutableApi
fun Curves.toNonCurveSimplePointList(out: PointArrayList = PointArrayList()): IPointArrayList? {
    val curves = this
    val beziers = curves.beziers//.flatMap { it.toSimpleList() }.map { it.curve }
    val epsilon = 0.0001f
    beziers.fastForEach { bezier ->
        if (bezier.inflections().isNotEmpty()) return null
        val points = bezier.points
        points.fastForEach { p ->
            if (out.isEmpty() || !out.last.isAlmostEquals(p, epsilon)) out.add(p)
        }
        //println("bezier=$bezier")
        //out.add(points, 0, points.size - 1)
    }
    if (out.last.isAlmostEquals(out.first, epsilon)) {
        out.removeAt(out.size - 1)
    }
    return out
}

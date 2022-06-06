package com.soywiz.korma.geom.bezier

import com.soywiz.kds.getCyclic
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.korma.geom.BoundsBuilder
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.Rectangle
import com.soywiz.korma.math.convertRange
import kotlin.jvm.JvmName

@JvmName("ListCurves_toCurves")
fun List<Curves>.toCurves(closed: Boolean = this.last().closed) = Curves(this.flatMap { it.curves }, closed)
@JvmName("ListCurve_toCurves")
fun List<Curve>.toCurves(closed: Boolean) = Curves(this, closed)

data class Curves(val curves: List<Curve>, val closed: Boolean) : Curve {
    constructor(vararg curves: Curve, closed: Boolean = false) : this(curves.toList(), closed)

    override val order: Int get() = -1

    data class CurveInfo(
        val index: Int,
        val curve: Curve,
        val startLength: Double,
        val endLength: Double,
        val bounds: Rectangle,
    ) {
        fun contains(length: Double): Boolean = length in startLength..endLength

        val length: Double get() = endLength - startLength
    }

    val infos: List<CurveInfo> by lazy {
        var pos = 0.0
        curves.mapIndexed { index, curve ->
            val start = pos
            pos += curve.length()
            CurveInfo(index, curve, start, pos, curve.getBounds())
        }

    }
    val length: Double by lazy { infos.sumOf { it.length } }
    private val bb = BoundsBuilder()

    val CurveInfo.startRatio: Double get() = this.startLength / this@Curves.length
    val CurveInfo.endRatio: Double get() = this.endLength / this@Curves.length

    override fun getBounds(target: Rectangle): Rectangle {
        bb.reset()
        infos.fastForEach { bb.addEvenEmpty(it.bounds) }
        return bb.getBounds(target)
    }

    @PublishedApi
    internal fun findInfo(t: Double): CurveInfo {
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
    internal inline fun <T> findTInCurve(t: Double, block: (info: CurveInfo, ratioInCurve: Double) -> T): T {
        val pos = t * length
        val info = findInfo(t)
        val posInCurve = pos - info.startLength
        val ratioInCurve = posInCurve / info.length
        return block(info, ratioInCurve)
    }

    override fun calc(t: Double, target: Point): Point =
        findTInCurve(t) { info, ratioInCurve -> info.curve.calc(ratioInCurve, target) }

    override fun normal(t: Double, target: Point): Point =
        findTInCurve(t) { info, ratioInCurve -> info.curve.normal(ratioInCurve, target) }

    override fun tangent(t: Double, target: Point): Point =
        findTInCurve(t) { info, ratioInCurve -> info.curve.tangent(ratioInCurve, target) }

    override fun ratioFromLength(length: Double): Double {
        if (length <= 0.0) return 0.0
        if (length >= this.length) return 1.0

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
            return Double.NaN
        } // length not in curve!
        val info = infos[index]
        val lengthInCurve = length - info.startLength
        val ratioInCurve = info.curve.ratioFromLength(lengthInCurve)
        return ratioInCurve.convertRange(0.0, 1.0, info.startRatio, info.endRatio)
    }

    fun splitLeftByLength(len: Double): Curves = splitLeft(ratioFromLength(len))
    fun splitRightByLength(len: Double): Curves = splitRight(ratioFromLength(len))
    fun splitByLength(len0: Double, len1: Double): Curves = split(ratioFromLength(len0), ratioFromLength(len1))

    fun splitLeft(t: Double): Curves = split(0.0, t)
    fun splitRight(t: Double): Curves = split(t, 1.0)

    fun split(t0: Double, t1: Double): Curves {
        if (t0 > t1) return split(t1, t0)
        check(t0 <= t1)

        if (t0 == t1) return Curves(emptyList(), closed = false)

        return Curves(findTInCurve(t0) { info0, ratioInCurve0 ->
            findTInCurve(t1) { info1, ratioInCurve1 ->
                if (info0.index == info1.index) {
                    listOf((info0.curve as BezierCurve).split(ratioInCurve0, ratioInCurve1).curve)
                } else {
                    buildList {
                        if (ratioInCurve0 != 1.0) add((info0.curve as BezierCurve).splitRight(ratioInCurve0).curve)
                        for (index in info0.index + 1 until info1.index) add(infos[index].curve)
                        if (ratioInCurve1 != 0.0) add((info1.curve as BezierCurve).splitLeft(ratioInCurve1).curve)
                    }
                }
            }
        }, closed = false)
    }

    override fun length(steps: Int): Double = length
}

fun Curves.toDashes(pattern: DoubleArray, offset: Double = 0.0): List<Curves> {
    check(!pattern.all { it <= 0.0 })
    val length = this.length
    var current = offset
    var dashNow = true
    var index = 0
    val out = arrayListOf<Curves>()
    while (current < length) {
        val len = pattern.getCyclic(index++)
        if (dashNow) {
            out += splitByLength(current, current + len)
        }
        current += len
        dashNow = !dashNow
    }
    return out
}

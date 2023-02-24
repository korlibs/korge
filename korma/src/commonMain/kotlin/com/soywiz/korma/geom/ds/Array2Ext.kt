package com.soywiz.korma.geom.ds

import com.soywiz.kds.Array2
import com.soywiz.korma.annotations.*
import com.soywiz.korma.geom.*

@KormaValueApi inline operator fun <T> Array2<T>.get(p: Point): T = get(p.x.toInt(), p.y.toInt())
@KormaValueApi inline operator fun <T> Array2<T>.set(p: Point, value: T) = set(p.x.toInt(), p.y.toInt(), value)
@KormaValueApi inline fun <T> Array2<T>.tryGet(p: Point): T? = tryGet(p.x.toInt(), p.y.toInt())
@KormaValueApi inline fun <T> Array2<T>.trySet(p: Point, value: T) = trySet(p.x.toInt(), p.y.toInt(), value)
@KormaValueApi inline operator fun <T> Array2<T>.get(p: PointInt): T = get(p.x, p.y)
@KormaValueApi inline operator fun <T> Array2<T>.set(p: PointInt, value: T) = set(p.x, p.y, value)
@KormaValueApi inline fun <T> Array2<T>.tryGet(p: PointInt): T? = tryGet(p.x, p.y)
@KormaValueApi inline fun <T> Array2<T>.trySet(p: PointInt, value: T) = trySet(p.x, p.y, value)

@KormaMutableApi inline operator fun <T> Array2<T>.get(p: IPoint): T = get(p.x.toInt(), p.y.toInt())
@KormaMutableApi inline operator fun <T> Array2<T>.set(p: IPoint, value: T) = set(p.x.toInt(), p.y.toInt(), value)
@KormaMutableApi inline fun <T> Array2<T>.tryGet(p: IPoint): T? = tryGet(p.x.toInt(), p.y.toInt())
@KormaMutableApi inline fun <T> Array2<T>.trySet(p: IPoint, value: T) = trySet(p.x.toInt(), p.y.toInt(), value)
@KormaMutableApi inline operator fun <T> Array2<T>.get(p: IPointInt): T = get(p.x, p.y)
@KormaMutableApi inline operator fun <T> Array2<T>.set(p: IPointInt, value: T) = set(p.x, p.y, value)
@KormaMutableApi inline fun <T> Array2<T>.tryGet(p: IPointInt): T? = tryGet(p.x, p.y)
@KormaMutableApi inline fun <T> Array2<T>.trySet(p: IPointInt, value: T) = trySet(p.x, p.y, value)

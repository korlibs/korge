package com.soywiz.korma.geom.ds

import com.soywiz.kds.*
import com.soywiz.korma.annotations.*
import com.soywiz.korma.geom.*

@KormaValueApi inline operator fun <T> Array2<T>.get(p: Point): T = get(p.x.toInt(), p.y.toInt())
@KormaValueApi inline operator fun <T> Array2<T>.set(p: Point, value: T) = set(p.x.toInt(), p.y.toInt(), value)
@KormaValueApi inline fun <T> Array2<T>.tryGet(p: Point): T? = tryGet(p.x.toInt(), p.y.toInt())
@KormaValueApi inline fun <T> Array2<T>.trySet(p: Point, value: T) = trySet(p.x.toInt(), p.y.toInt(), value)
@KormaValueApi inline operator fun <T> Array2<T>.get(p: Vector2Int): T = get(p.x, p.y)
@KormaValueApi inline operator fun <T> Array2<T>.set(p: Vector2Int, value: T) = set(p.x, p.y, value)
@KormaValueApi inline fun <T> Array2<T>.tryGet(p: Vector2Int): T? = tryGet(p.x, p.y)
@KormaValueApi inline fun <T> Array2<T>.trySet(p: Vector2Int, value: T) = trySet(p.x, p.y, value)

@KormaMutableApi inline operator fun <T> Array2<T>.get(p: MPoint): T = get(p.x.toInt(), p.y.toInt())
@KormaMutableApi inline operator fun <T> Array2<T>.set(p: MPoint, value: T) = set(p.x.toInt(), p.y.toInt(), value)
@KormaMutableApi inline fun <T> Array2<T>.tryGet(p: MPoint): T? = tryGet(p.x.toInt(), p.y.toInt())
@KormaMutableApi inline fun <T> Array2<T>.trySet(p: MPoint, value: T) = trySet(p.x.toInt(), p.y.toInt(), value)
@KormaMutableApi inline operator fun <T> Array2<T>.get(p: MPointInt): T = get(p.x, p.y)
@KormaMutableApi inline operator fun <T> Array2<T>.set(p: MPointInt, value: T) = set(p.x, p.y, value)
@KormaMutableApi inline fun <T> Array2<T>.tryGet(p: MPointInt): T? = tryGet(p.x, p.y)
@KormaMutableApi inline fun <T> Array2<T>.trySet(p: MPointInt, value: T) = trySet(p.x, p.y, value)

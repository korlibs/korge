package com.soywiz.korma.geom.ds

import com.soywiz.kds.*
import com.soywiz.korma.geom.*

inline operator fun <T> Array2<T>.get(p: IPoint): T = get(p.x.toInt(), p.y.toInt())
inline operator fun <T> Array2<T>.set(p: IPoint, value: T) = set(p.x.toInt(), p.y.toInt(), value)

inline fun <T> Array2<T>.tryGet(p: IPoint): T? = tryGet(p.x.toInt(), p.y.toInt())
inline fun <T> Array2<T>.trySet(p: IPoint, value: T) = trySet(p.x.toInt(), p.y.toInt(), value)

inline operator fun <T> Array2<T>.get(p: IPointInt): T = get(p.x, p.y)
inline operator fun <T> Array2<T>.set(p: IPointInt, value: T) = set(p.x, p.y, value)

inline fun <T> Array2<T>.tryGet(p: IPointInt): T? = tryGet(p.x, p.y)
inline fun <T> Array2<T>.trySet(p: IPointInt, value: T) = trySet(p.x, p.y, value)

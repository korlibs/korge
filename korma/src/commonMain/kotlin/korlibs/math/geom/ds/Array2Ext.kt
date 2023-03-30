package korlibs.math.geom.ds

import korlibs.datastructure.*
import korlibs.math.annotations.*
import korlibs.math.geom.*

inline operator fun <T> Array2<T>.get(p: Point): T = get(p.x.toInt(), p.y.toInt())
inline operator fun <T> Array2<T>.set(p: Point, value: T) = set(p.x.toInt(), p.y.toInt(), value)
inline fun <T> Array2<T>.tryGet(p: Point): T? = tryGet(p.x.toInt(), p.y.toInt())
inline fun <T> Array2<T>.trySet(p: Point, value: T) = trySet(p.x.toInt(), p.y.toInt(), value)
inline operator fun <T> Array2<T>.get(p: PointInt): T = get(p.x, p.y)
inline operator fun <T> Array2<T>.set(p: PointInt, value: T) = set(p.x, p.y, value)
inline fun <T> Array2<T>.tryGet(p: PointInt): T? = tryGet(p.x, p.y)
inline fun <T> Array2<T>.trySet(p: PointInt, value: T) = trySet(p.x, p.y, value)

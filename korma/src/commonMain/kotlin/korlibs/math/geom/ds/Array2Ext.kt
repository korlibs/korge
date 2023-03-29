package korlibs.math.geom.ds

import korlibs.datastructure.*
import korlibs.math.annotations.*
import korlibs.math.geom.*

@KormaValueApi inline operator fun <T> Array2<T>.get(p: Point): T = get(p.x.toInt(), p.y.toInt())
@KormaValueApi inline operator fun <T> Array2<T>.set(p: Point, value: T) = set(p.x.toInt(), p.y.toInt(), value)
@KormaValueApi inline fun <T> Array2<T>.tryGet(p: Point): T? = tryGet(p.x.toInt(), p.y.toInt())
@KormaValueApi inline fun <T> Array2<T>.trySet(p: Point, value: T) = trySet(p.x.toInt(), p.y.toInt(), value)
@KormaValueApi inline operator fun <T> Array2<T>.get(p: PointInt): T = get(p.x, p.y)
@KormaValueApi inline operator fun <T> Array2<T>.set(p: PointInt, value: T) = set(p.x, p.y, value)
@KormaValueApi inline fun <T> Array2<T>.tryGet(p: PointInt): T? = tryGet(p.x, p.y)
@KormaValueApi inline fun <T> Array2<T>.trySet(p: PointInt, value: T) = trySet(p.x, p.y, value)

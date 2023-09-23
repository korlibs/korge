package korlibs.image.vector

import korlibs.datastructure.*
import korlibs.datastructure.iterators.*
import korlibs.image.bitmap.*
import korlibs.image.bitmap.sdf.*
import korlibs.image.color.*
import korlibs.math.geom.*
import korlibs.math.geom.bezier.*
import korlibs.math.geom.vector.*

fun VectorPath.sdf(width: Int, height: Int): FloatArray2 = sdf(FloatArray2(width, height, 0f))

// @TODO: We should optimize this as much as possible, to not query all the edges, but as few as possible!
// CHECK: https://math.stackexchange.com/questions/4079605/how-to-find-closest-point-to-polygon-shape-from-any-coordinate
fun VectorPath.sdf(data: FloatArray2): FloatArray2 {
    val path = this
    val curvesList = path.toCurvesList()
    var p = Point()
    val pp = Bezier.ProjectedPoint()
    for (y in 0 until data.height) {
        for (x in 0 until data.width) {
            p = Point(x + 0.5, y + 0.5)
            val inside = path.containsPoint(p)
            var min = Double.POSITIVE_INFINITY
            curvesList.fastForEach { curves ->
                //curves.beziers.size
                curves.beziers.fastForEachWithIndex { index, edge ->
                    //val color = index % 3
                    val result = edge.project(p, pp)
                    min = kotlin.math.min(min, result.d)
                    //val dist = Point.distance(result.p, p)
                    //println()
                }
            }
            if (inside) min = -min
            data[x, y] = min.toFloat()
        }
    }
    return data
}
fun VectorPath.sdfBmp(width: Int, height: Int): Bitmap32 {
    /*
    val msdf = sdf(width, height)
    msdf.scale(-1f)
    msdf.clamp(-1f, +1f)
    msdf.normalizeUniform()
    val msdfBitmap = msdf.toBMP32 { RGBA.float(1f, 1f, 1f, it) }
    */
    val newImage = NewSDF.process(NativeImageContext2d(width, height) { fill(Colors.WHITE) { write(this@sdfBmp) } }.toBMP32(), buffer = 0)
    //return newImage.bmp.sliceWithBounds(newImage.buffer, newImage.buffer, newImage.bmp.width - newImage.buffer, newImage.bmp.height - newImage.buffer).toBitmap().toBMP32IfRequired()
    return newImage.bmp.toBMP32IfRequired()
}

private fun msdfColor(index: Int, size: Int, closed: Boolean): RGBA {
    if (size == 1) return Colors.WHITE
    if (index == 0) return Colors.FUCHSIA
    return if ((index - 1) % 2 == 0) Colors.YELLOW else Colors.CYAN
}

fun VectorPath.msdf(width: Int, height: Int): FloatBitmap32 = msdf(FloatBitmap32(width, height))
fun VectorPath.msdfBmp(width: Int, height: Int): Bitmap32 {
    val msdf = msdf(width, height)
    msdf.updateComponent { component, value -> if (component == 3) -100f else value }
    msdf.scale(-1f)
    msdf.clamp(-1f, +1f)
    msdf.normalizeUniform()
    val msdfBitmap = msdf.toBMP32()
    return msdfBitmap
}

// @TODO: We should optimize this as much as possible, to not query all the edges, but as few as possible!
// CHECK: https://math.stackexchange.com/questions/4079605/how-to-find-closest-point-to-polygon-shape-from-any-coordinate
fun VectorPath.msdf(data: FloatBitmap32): FloatBitmap32 {
    val path = this
    val curvesList = path.toCurvesList()
    var p = Point()

    val colorizedCurves = curvesList.map { it.beziers.colorize(it.closed) }
    val allColorizedCurves = ColorizedBeziers(colorizedCurves.flatMap { it.beziers })

    for (y in 0 until data.height) {
        for (x in 0 until data.width) {
            p = Point(x + 0.5, y + 0.5)
            val inside = path.containsPoint(p)
            val allDist = allColorizedCurves.allProjected.closestDistance(p)
            val redDist = allColorizedCurves.redProjected.closestDistance(p)
            val greenDist = allColorizedCurves.greenProjected.closestDistance(p)
            val blueDist = allColorizedCurves.blueProjected.closestDistance(p)

            var minR = redDist
            var minG = greenDist
            var minB = blueDist
            var minA = allDist

            if (inside) {
                minR = -minR
                minG = -minG
                minB = -minB
                minA = -minA
            }
            data.setRgbaf(x, y, minR.toFloat(), minG.toFloat(), minB.toFloat(), minA.toFloat())
        }
    }
    return data
}

fun List<Bezier>.colorize(closed: Boolean = true): ColorizedBeziers {
    return ColorizedBeziers(this.mapIndexed { index, bezier ->
        val color = msdfColor(index, this.size, closed)
        ColoredBezier(bezier, color)
    })
}

data class ColoredBezier(val bezier: Bezier, val color: RGBA)

class ColorizedBeziers(val beziers: List<ColoredBezier>) {
    val red by lazy { beziers.filter { it.color.r != 0 } }
    val green by lazy { beziers.filter { it.color.g != 0 } }
    val blue by lazy { beziers.filter { it.color.b != 0 } }

    val allProjected by lazy { ProjectCurvesLookup(beziers.map { it.bezier }) }
    val redProjected by lazy { ProjectCurvesLookup(red.map { it.bezier }) }
    val greenProjected by lazy { ProjectCurvesLookup(green.map { it.bezier }) }
    val blueProjected by lazy { ProjectCurvesLookup(blue.map { it.bezier }) }
}

class ProjectCurvesLookup(val beziers: List<Bezier>) {
    private val tempProjected = Bezier.ProjectedPoint()
    private val tempPoint = MPoint()

    fun closestDistance(point: Point): Double = Point.distance(point, closest(point)).toDouble()

    fun closest(point: Point): Point {
        if (beziers.isEmpty()) return Point(0, 0)

        var minDistSq: Double = Double.POSITIVE_INFINITY
        //var closest: BezierWithInfo = beziers.first()

        // Find bezier with closest, farthest point against [point]
        beziers.fastForEach {
            val dist = it.outerCircle.distanceFarthestSquared(point)
            if (dist < minDistSq) {
                minDistSq = dist.toDouble()
                //closest = it
            }
        }

        // Cull Beziers whose nearest point is farther than the found farthest nearest point
        var bminDistSq = Double.POSITIVE_INFINITY
        var out = Point()
        //val keep = beziers.filter { it.outerCircle.distanceClosestSquared(point) <= minDistSq }
        //println("keep=${keep.size}, total=${beziers.size}")
        beziers.fastForEach {
            if (it.outerCircle.distanceClosestSquared(point) > minDistSq) return@fastForEach
            it.project(point, tempProjected)
            if (tempProjected.dSq < bminDistSq) {
                bminDistSq = tempProjected.dSq
                out = tempProjected.p
            }
        }

        return out
    }
}

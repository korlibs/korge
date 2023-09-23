@file:Suppress("PackageDirectoryMismatch")

package korlibs.math.geom.convex

import korlibs.math.geom.*
import korlibs.math.geom.bezier.*
import korlibs.math.geom.vector.*
import korlibs.math.interpolation.*
import kotlin.math.*

object Convex {
    fun isConvex(path: VectorPath): Boolean {
        //if (true) {
        //    val pointLists = path.toPathPointList()
        //    if (pointLists.size != 1) return false
        //    return isConvex(pointLists.first())
        //}
        val curvesLists = path.getCurvesList()
        if (curvesLists.size != 1) return false
        return isConvex(curvesLists.first())
    }

    fun isConvex(curves: Curves): Boolean {
        val points = curves.toNonCurveSimplePointList() ?: return false
        return isConvex(points)
    }

    fun isConvex(vertices: PointList): Boolean { // Rory Daulton
        var base = 0
        var n = vertices.size
        val TWO_PI: Double = PI2

        // points is 'strictly convex': points are valid, side lengths non-zero, interior angles are strictly between zero and a straight
        // angle, and the polygon does not intersect itself.
        // NOTES:  1.  Algorithm: the signed changes of the direction angles from one side to the next side must be all positive or
        // all negative, and their sum must equal plus-or-minus one full turn (2 pi radians). Also check for too few,
        // invalid, or repeated points.
        //      2.  No check is explicitly done for zero internal angles(180 degree direction-change angle) as this is covered
        // in other ways, including the `n < 3` check.
        // needed for any bad points or direction changes
        // Check for too few points
        if (n <= 3) return true
        if (vertices.getX(base) == vertices.getX(n - 1) && vertices.getY(base) == vertices.getY(n - 1)) {
            // if its a closed polygon, ignore last vertex
            n--
        }
        // Get starting information
        var old_x = vertices.getX(n - 2).toDouble()
        var old_y = vertices.getY(n - 2).toDouble()
        var new_x = vertices.getX(n - 1).toDouble()
        var new_y = vertices.getY(n - 1).toDouble()
        var new_direction: Double = kotlin.math.atan2(new_y - old_y, new_x - old_x)
        var old_direction: Double
        var angle_sum = Angle.ZERO
        var orientation = 0.0
        // Check each point (the side ending there, its angle) and accum. angles for ndx, newpoint in enumerate(polygon):
        for (i in 0 until n) {
            // Update point coordinates and side directions, check side length
            old_x = new_x
            old_y = new_y
            old_direction = new_direction
            val p = base++
            new_x = vertices.getX(p).toDouble()
            new_y = vertices.getY(p).toDouble()
            new_direction = kotlin.math.atan2(new_y - old_y, new_x - old_x)
            if (old_x == new_x && old_y == new_y) { // repeated consecutive points
                return false
            }
            // Calculate & check the normalized direction-change angle
            var angle = new_direction - old_direction
            when {
                angle <= -PI -> angle += TWO_PI // make it in half-open interval (-Pi, Pi]
                angle > PI -> angle -= TWO_PI
            }
            when {
                // if first time through loop, initialize orientation
                i == 0 -> {
                    if (angle == 0.0) {
                        return false
                    }
                    orientation = if (angle > 0) 1.0 else -1.0
                }
                // if other time through loop, check orientation is stable
                // not both pos. or both neg.
                orientation * angle < 0.0 -> {
                    return false
                }
            }
            // Accumulate the direction-change angle
            angle_sum += Angle.fromRadians(angle)
            // Check that the total number of full turns is plus-or-minus 1
        }
        return Ratio.ONE.isAlmostEquals(angle_sum.ratio.absoluteValue)
    }

    /*
    // https://stackoverflow.com/questions/471962/how-do-i-efficiently-determine-if-a-polygon-is-convex-non-convex-or-complex/45372025#45372025
    fun isConvex2(vertices: MPointArrayList): Boolean {
        if (vertices.size < 4) return true
        var sign = false
        val n: Int = vertices.size
        for (i in 0 until n) {
            val dx1: Double = vertices.getX((i + 2) % n) - vertices.getX((i + 1) % n)
            val dy1: Double = vertices.getY((i + 2) % n) - vertices.getY((i + 1) % n)
            val dx2: Double = vertices.getX(i) - vertices.getX((i + 1) % n)
            val dy2: Double = vertices.getY(i) - vertices.getY((i + 1) % n)
            val zcrossproduct = dx1 * dy2 - dy1 * dx2
            if (i == 0) sign = zcrossproduct > 0 else if (sign != zcrossproduct > 0) return false
        }
        return true
    }

    // https://math.stackexchange.com/questions/1743995/determine-whether-a-polygon-is-convex-based-on-its-vertices
    /*
    fun isConvex(vertices: MPointArrayList): Boolean {
        if (vertices.size < 3) return false

        val N = vertices.size
        var wSign = 0.0        // First nonzero orientation (positive or negative)

        var xSign = 0
        var xFirstSign = 0   // Sign of first nonzero edge vector x
        var xFlips = 0       // Number of sign changes in x

        var ySign = 0
        var yFirstSign = 0   // Sign of first nonzero edge vector y
        var yFlips = 0       // Number of sign changes in y

        val curr = vertices.getPoint(N - 2)   // Second-to-last vertex
        val next = vertices.getPoint(N - 1)     // Last vertex

        vertices.fastForEach { x, y -> // Each vertex, in order
            val v = Point(x, y)
            val prev = curr          // Previous vertex
            val curr = next          // Current vertex
            val next = v             // Next vertex

            // Previous edge vector ("before"):
            val bx = curr.x - prev.x
            val by = curr.y - prev.y

            // Next edge vector ("after"):
            val ax = next.x - curr.x
            val ay = next.y - curr.y

            // Calculate sign flips using the next edge vector ("after"),
            // recording the first sign.
            if (ax > 0) {
                when {
                    xSign == 0 -> xFirstSign = +1
                    xSign < 0 -> xFlips += 1
                }
                xSign = +1
            } else if (ax < 0) {
                when {
                    xSign == 0 -> xFirstSign = -1
                    xSign > 0 -> xFlips += 1
                }
                xSign = -1
            }

            if (xFlips > 2) return false

            if (ay > 0) {
                when {
                    ySign == 0 -> yFirstSign = +1
                    ySign < 0 -> yFlips += 1
                }
                ySign = +1
            } else if (ay < 0) {
                when {
                    ySign == 0 -> yFirstSign = -1
                    ySign > 0 -> yFlips += 1
                }
                ySign = -1
            }

            if (yFlips > 2) return false

            // Find out the orientation of this pair of edges,
            // and ensure it does not differ from previous ones.
            val w = bx * ay - ax * by
            when {
                (wSign == 0.0) && (w != 0.0) -> wSign = w
                (wSign > 0) && (w < 0) -> return false
                (wSign < 0) && (w > 0) -> return false
            }
        }

        // Final/wraparound sign flips:
        if ((xSign != 0) && (xFirstSign != 0) && (xSign != xFirstSign)) xFlips += 1
        if ((ySign != 0) && (yFirstSign != 0) && (ySign != yFirstSign)) yFlips += 1

        // Concave polygons have two sign flips along each axis.
        if ((xFlips != 2) || (yFlips != 2)) return false

        // This is a convex polygon.
        return true
    }
    */
    */
}

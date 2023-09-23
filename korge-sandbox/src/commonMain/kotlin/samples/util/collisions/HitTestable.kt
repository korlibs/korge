package samples.util.collisions

import CollisionKind
import korlibs.datastructure.iterators.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.math.geom.collider.HitTestable
import korlibs.math.geom.collider.HitTestDirection

private val MOVE_ANGLES = arrayOf(0.degrees, 5.degrees, 10.degrees, 15.degrees, 20.degrees, 30.degrees, 45.degrees, 60.degrees, 80.degrees, 85.degrees)
private val MOVE_SCALES = arrayOf(+1.0, -1.0)

// @TODO: if dx & dy are big, we should check intermediary positions to ensure we are not jumping to the other side of the object
fun View.moveWithHitTestable(collision: HitTestable, dx: Double, dy: Double, hitTestDirection: korlibs.math.geom.collider.HitTestDirection? = null) {
    val char = this
    val deltaXY = Point(dx, dy)
    val angle = Angle.between(Point.ZERO, deltaXY)
    val length = deltaXY.length
    val oldX = char.x
    val oldY = char.y
    MOVE_ANGLES.fastForEach { dangle ->
        MOVE_SCALES.fastForEach { dscale ->
            val rangle = angle + dangle * dscale
            val lengthScale = dangle.cosine
            val dpoint = Point.polar(rangle, length * lengthScale)
            char.x = oldX + dpoint.x
            char.y = oldY + dpoint.y
            val global = char.globalPos
            if (!collision.hitTestAny(global, hitTestDirection ?: HitTestDirection.fromAngle(angle))) {
                return // Accept movement
            }
        }
    }
    char.x = oldX
    char.y = oldY
}

fun View.moveWithCollisions(collision: List<View>, delta: Vector2F, kind: CollisionKind = CollisionKind.SHAPE) {
    return moveWithCollisions(collision, delta.x, delta.y, kind)
}

fun View.moveWithCollisions(collision: List<View>, dx: Float, dy: Float, kind: CollisionKind = CollisionKind.SHAPE) {
    val char = this
    val deltaXY = Point(dx, dy)
    val angle = Angle.between(Point.ZERO, deltaXY)
    val length = deltaXY.length
    val oldX = char.x
    val oldY = char.y
    MOVE_ANGLES.fastForEach { dangle ->
        MOVE_SCALES.fastForEach { dscale ->
            val rangle = angle + dangle * dscale
            val lengthScale = dangle.cosine
            val dpoint = Point.polar(rangle, length * lengthScale)
            char.pos = Point(oldX + dpoint.x, oldY + dpoint.y)
            //char.hitTestView(collision, kind)
            //if (!char.collidesWith(collision, kind)) {
            if (collision.all { it == this || it.hitTestView(char) == null }) {
            //if (char.hitTestView(collision) == null) {
                return // Accept movement
            }
        }
    }
    char.x = oldX
    char.y = oldY
}

package com.soywiz.korge.view

import com.soywiz.kds.iterators.fastForEach
import com.soywiz.kmem.extract
import com.soywiz.kmem.insert
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.Vector2D
import com.soywiz.korma.geom.angleTo
import com.soywiz.korma.geom.cosine
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.div
import com.soywiz.korma.geom.plus
import com.soywiz.korma.geom.times
import com.soywiz.korma.geom.vector.VectorPath

@Deprecated("", replaceWith = ReplaceWith("com.soywiz.korma.geom.collider.HitTestable"))
typealias HitTestable = com.soywiz.korma.geom.collider.HitTestable
@Deprecated("", replaceWith = ReplaceWith("com.soywiz.korma.geom.collider.HitTestDirectionFlags"))
typealias HitTestDirectionFlags = com.soywiz.korma.geom.collider.HitTestDirectionFlags
@Deprecated("", replaceWith = ReplaceWith("com.soywiz.korma.geom.collider.HitTestDirection"))
typealias HitTestDirection = com.soywiz.korma.geom.collider.HitTestDirection

private val MOVE_ANGLES = arrayOf(0.degrees, 5.degrees, 10.degrees, 15.degrees, 20.degrees, 30.degrees, 45.degrees, 60.degrees, 80.degrees, 85.degrees)
private val MOVE_SCALES = arrayOf(+1.0, -1.0)

// @TODO: if dx & dy are big, we should check intermediary positions to ensure we are not jumping to the other side of the object
fun View.moveWithHitTestable(collision: HitTestable, dx: Double, dy: Double, hitTestDirection: HitTestDirection? = null) {
    val char = this
    val deltaXY = Point(dx, dy)
    val angle = Angle.between(0.0, 0.0, deltaXY.x, deltaXY.y)
    val length = deltaXY.length
    val oldX = char.x
    val oldY = char.y
    MOVE_ANGLES.fastForEach { dangle ->
        MOVE_SCALES.fastForEach { dscale ->
            val rangle = angle + dangle * dscale
            val lengthScale = dangle.cosine
            val dpoint = Point.fromPolar(rangle, length * lengthScale)
            char.x = oldX + dpoint.x
            char.y = oldY + dpoint.y
            if (!collision.hitTestAny(
                char.globalX, char.globalY,
                hitTestDirection ?: HitTestDirection.fromAngle(angle))
            ) {
                return // Accept movement
            }
        }
    }
    char.x = oldX
    char.y = oldY
}

fun View.moveWithCollisions(collision: List<View>, delta: Vector2D, kind: CollisionKind = CollisionKind.SHAPE) {
    return moveWithCollisions(collision, delta.x, delta.y, kind)
}

fun View.moveWithCollisions(collision: List<View>, dx: Double, dy: Double, kind: CollisionKind = CollisionKind.SHAPE) {
    val char = this
    val deltaXY = Point(dx, dy)
    val angle = Angle.between(0.0, 0.0, deltaXY.x, deltaXY.y)
    val length = deltaXY.length
    val oldX = char.x
    val oldY = char.y
    MOVE_ANGLES.fastForEach { dangle ->
        MOVE_SCALES.fastForEach { dscale ->
            val rangle = angle + dangle * dscale
            val lengthScale = dangle.cosine
            val dpoint = Point.fromPolar(rangle, length * lengthScale)
            char.x = oldX + dpoint.x
            char.y = oldY + dpoint.y
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

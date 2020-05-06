package com.soywiz.korge.input

import com.soywiz.korge.view.*
import com.soywiz.korma.geom.*
import kotlin.math.*

data class SwipeInfo(
    var dx: Double = 0.0,
    var dy: Double = 0.0,
    var direction: SwipeDirection
)

enum class SwipeDirection {
    LEFT, RIGHT, TOP, BOTTOM
}

/**
 * This methods lets you specify a [callback] to execute when swipe event is triggered.
 * @property threshold a threshold for dx or dy after which a swipe event should be triggered.
 * Negative [threshold] means a swipe event should be triggered only after [onUp] mouse event.
 * [threshold] is set to -1 by default.
 * @property direction a direction for which a swipe event should be triggered.
 * [direction] is set to null by default, which means a swipe event should be triggered for any direction.
 * @property callback a callback to execute
 */
fun <T : View> T.onSwipe(
    threshold: kotlin.Number = -1,
    direction: SwipeDirection? = null,
    callback: suspend Views.(SwipeInfo) -> Unit
): T {
    var register = false
    var sx = 0.0
    var sy = 0.0
    var cx = 0.0
    var cy = 0.0
    var movedLeft = false
    var movedRight = false
    var movedTop = false
    var movedBottom = false
    val thr = threshold.toDouble()
    val view = this
    val mousePos = Point()

    fun views() = view.stage!!.views

    fun updateMouse() {
        val views = views()
        mousePos.setTo(
            views.nativeMouseX,
            views.nativeMouseY
        )
    }

    fun updateCoordinates() {
        if (mousePos.x < cx) movedLeft = true
        if (mousePos.x > cx) movedRight = true
        if (mousePos.y < cy) movedTop = true
        if (mousePos.y > cy) movedBottom = true
        cx = mousePos.x
        cy = mousePos.y
    }

    suspend fun triggerEvent(direction: SwipeDirection) {
        register = false
        views().callback(SwipeInfo(cx - sx, cy - sy, direction))
    }

    suspend fun checkPositionOnMove() {
        if (thr < 0) return
        when (direction) {
            SwipeDirection.LEFT -> if (sx - cx > thr && !movedRight) triggerEvent(direction)
            SwipeDirection.RIGHT -> if (cx - sx > thr && !movedLeft) triggerEvent(direction)
            SwipeDirection.TOP -> if (sy - cy > thr && !movedBottom) triggerEvent(direction)
            SwipeDirection.BOTTOM -> if (cy - sy > thr && !movedTop) triggerEvent(direction)
            null -> when {
                sx - cx > thr && !movedRight -> triggerEvent(SwipeDirection.LEFT)
                cx - sx > thr && !movedLeft -> triggerEvent(SwipeDirection.RIGHT)
                sy - cy > thr && !movedBottom -> triggerEvent(SwipeDirection.TOP)
                cy - sy > thr && !movedTop -> triggerEvent(SwipeDirection.BOTTOM)
            }
        }
    }

    suspend fun checkPositionOnUp() {
        if (thr >= 0) return
        val horDiff = abs(cx - sx)
        val verDiff = abs(cy - sy)
        when (direction) {
            SwipeDirection.LEFT -> if (horDiff >= verDiff && cx < sx && !movedRight) triggerEvent(direction)
            SwipeDirection.RIGHT -> if (horDiff >= verDiff && cx > sx && !movedLeft) triggerEvent(direction)
            SwipeDirection.TOP -> if (horDiff <= verDiff && cy < sy && !movedBottom) triggerEvent(direction)
            SwipeDirection.BOTTOM -> if (horDiff <= verDiff && cy > sy && !movedTop) triggerEvent(direction)
            null -> if (horDiff >= verDiff) {
                if (cx < sx && !movedRight) {
                    triggerEvent(SwipeDirection.LEFT)
                } else if (cx > sx && !movedLeft) {
                    triggerEvent(SwipeDirection.RIGHT)
                }
            } else {
                if (cy < sy && !movedBottom) {
                    triggerEvent(SwipeDirection.TOP)
                } else if (cy > sy && !movedTop) {
                    triggerEvent(SwipeDirection.BOTTOM)
                }
            }
        }
    }

    this.mouse {
        onDown {
            updateMouse()
            register = true
            sx = mousePos.x
            sy = mousePos.y
            cx = sx
            cy = sy
            movedLeft = false
            movedRight = false
            movedTop = false
            movedBottom = false
        }
        onMoveAnywhere {
            if (register) {
                updateMouse()
                updateCoordinates()
                checkPositionOnMove()
            }
        }
        onUpAnywhere {
            if (register) {
                updateMouse()
                updateCoordinates()
                register = false
                checkPositionOnUp()
            }
        }
    }
    return this
}

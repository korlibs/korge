package com.soywiz.korge.input

import com.soywiz.korge.view.*
import com.soywiz.korma.geom.*
import kotlin.math.*

data class SwipeInfo(
    var dx: Double = 0.0,
    var dy: Double = 0.0,
    var direction: SwipeDirection
) {
    fun setTo(dx: Double, dy: Double, direction: SwipeDirection): SwipeInfo {
        this.dx = dx
        this.dy = dy
        this.direction = direction
        return this
    }
}

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
    threshold: Double = -1.0,
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

    val view = this
    val mousePos = Point()
    val swipeInfo = SwipeInfo(0.0, 0.0, SwipeDirection.TOP)

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
        views().callback(swipeInfo.setTo(cx - sx, cy - sy, direction))
    }

    suspend fun checkPositionOnMove() {
        if (threshold < 0) return
        val curDirection = when {
            sx - cx > threshold && !movedRight -> SwipeDirection.LEFT
            cx - sx > threshold && !movedLeft -> SwipeDirection.RIGHT
            sy - cy > threshold && !movedBottom -> SwipeDirection.TOP
            cy - sy > threshold && !movedTop -> SwipeDirection.BOTTOM
            else -> return
        }
        if (direction == null || direction == curDirection) {
            triggerEvent(curDirection)
        }
    }

    suspend fun checkPositionOnUp() {
        if (threshold >= 0) return
        val horDiff = abs(cx - sx)
        val verDiff = abs(cy - sy)
        val curDirection = when {
            horDiff >= verDiff && cx < sx && !movedRight -> SwipeDirection.LEFT
            horDiff >= verDiff && cx > sx && !movedLeft -> SwipeDirection.RIGHT
            horDiff <= verDiff && cy < sy && !movedBottom -> SwipeDirection.TOP
            horDiff <= verDiff && cy > sy && !movedTop -> SwipeDirection.BOTTOM
            else -> return
        }
        if (direction == null || direction == curDirection) {
            triggerEvent(curDirection)
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

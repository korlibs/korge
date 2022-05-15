package com.soywiz.korge.input

import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.degrees
import com.soywiz.korma.geom.minus

enum class SwipeRecognizerDirection(val dx: Int, val dy: Int) {
    UP(0, -1),
    RIGHT(+1, 0),
    DOWN(0, +1),
    LEFT(-1, 0);
}

fun TouchEvents.swipeRecognizer(
    thresold: Double = 32.0,
    block: (direction: SwipeRecognizerDirection) -> Unit
) {
    var completed = false
    endAll {
        if (it.infos.size == 1) {
            completed = false
        }
    }
    updateAll {
        if (it.infos.size == 1) {
            if (!completed) {
                val i = it.infos[0]

                val distance = Point.distance(i.startGlobal, i.global)
                if (distance >= thresold) {
                    val angle = Angle.between(i.startGlobal, i.global)
                    completed = true
                    val direction = when {
                        angle >= 315.degrees || angle < 45.degrees -> SwipeRecognizerDirection.RIGHT
                        angle >= 45.degrees && angle < 135.degrees -> SwipeRecognizerDirection.DOWN
                        angle >= 135.degrees && angle < 225.degrees -> SwipeRecognizerDirection.LEFT
                        angle >= 225.degrees && angle < 315.degrees -> SwipeRecognizerDirection.UP
                        else -> null
                    }
                    //println("distance=$distance, angle=$angle, direction=$direction")
                    block(direction ?: SwipeRecognizerDirection.UP)
                }
            }
        }
    }
}

data class ScaleRecognizerInfo(
    // True when the gesture starts
    var started: Boolean = false,
    // True when the gestuer ends
    var completed: Boolean = true,
    var start: Double = 0.0,
    var current: Double = 0.0,
) {
    val ratio get() = current / start
}

fun TouchEvents.scaleRecognizer(
    start: ScaleRecognizerInfo.() -> Unit = {},
    end: ScaleRecognizerInfo.(ratio: Double) -> Unit = {},
    block: ScaleRecognizerInfo.(ratio: Double) -> Unit
) {
    val info = ScaleRecognizerInfo()
    updateAll {
        if (it.infos.size >= 2) {
            val i0 = it.infos[0]
            val i1 = it.infos[1]
            info.started = info.completed
            info.completed = false
            info.start = Point.distance(i0.startGlobal, i1.startGlobal)
            info.current = Point.distance(i0.global, i1.global)
            if (info.started) {
                start(info)
            }
            block(info, info.ratio)
        } else {
            if (!info.completed) {
                info.completed = true
                info.started = false
                block(info, info.ratio)
                end(info, info.ratio)
            }
        }
    }
}

data class RotationRecognizerInfo(
    var started: Boolean = false,
    var completed: Boolean = false,
    var start: Angle = 0.degrees,
    var current: Angle = 0.degrees,
) {
    val delta get() = current - start
}

fun TouchEvents.rotationRecognizer(
    start: RotationRecognizerInfo.() -> Unit = {},
    end: RotationRecognizerInfo.(delta: Angle) -> Unit = {},
    block: RotationRecognizerInfo.(delta: Angle) -> Unit
) {
    val info = RotationRecognizerInfo()
    updateAll {
        if (it.infos.size >= 2) {
            val i0 = it.infos[0]
            val i1 = it.infos[1]
            info.started = info.completed
            info.completed = false
            info.start = Angle.between(i0.startGlobal, i1.startGlobal)
            info.current = Angle.between(i0.global, i1.global)
            if (info.started) {
                start(info)
            }
            block(info, info.delta)
        } else {
            if (!info.completed) {
                info.completed = true
                block(info, info.delta)
                end(info, info.delta)
            }
        }
    }
}

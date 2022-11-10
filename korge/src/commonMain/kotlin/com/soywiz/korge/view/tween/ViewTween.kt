package com.soywiz.korge.view.tween

import com.soywiz.klock.TimeSpan
import com.soywiz.korge.tween.DEFAULT_EASING
import com.soywiz.korge.tween.DEFAULT_TIME
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.View
import com.soywiz.korma.geom.Angle
import com.soywiz.korma.geom.plus
import com.soywiz.korma.interpolation.Easing

@Deprecated("Use animator instead")
suspend fun View.show(time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) =
    tween(this::alpha[1.0], time = time, easing = easing) { this.visible = true }

@Deprecated("Use animator instead")
suspend fun View.hide(time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) =
    tween(this::alpha[0.0], time = time, easing = easing)

@Deprecated("Use animator instead")
suspend inline fun View.moveTo(x: Double, y: Double, time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) = tween(this::x[x], this::y[y], time = time, easing = easing)
@Deprecated("Use animator instead")
suspend inline fun View.moveTo(x: Float, y: Float, time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) = tween(this::x[x], this::y[y], time = time, easing = easing)
@Deprecated("Use animator instead")
suspend inline fun View.moveTo(x: Int, y: Int, time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) = tween(this::x[x], this::y[y], time = time, easing = easing)

@Deprecated("Use animator instead")
suspend inline fun View.moveBy(dx: Double, dy: Double, time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) = tween(this::x[this.x + dx], this::y[this.y + dy], time = time, easing = easing)
@Deprecated("Use animator instead")
suspend inline fun View.moveBy(dx: Float, dy: Float, time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) = tween(this::x[this.x + dx], this::y[this.y + dy], time = time, easing = easing)
@Deprecated("Use animator instead")
suspend inline fun View.moveBy(dx: Int, dy: Int, time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) = tween(this::x[this.x + dx], this::y[this.y + dy], time = time, easing = easing)

@Deprecated("Use animator instead")
suspend inline fun View.scaleTo(sx: Double, sy: Double, time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) = tween(this::scaleX[sx], this::scaleY[sy], time = time, easing = easing)
@Deprecated("Use animator instead")
suspend inline fun View.scaleTo(sx: Float, sy: Float, time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) = tween(this::scaleX[sx], this::scaleY[sy], time = time, easing = easing)
@Deprecated("Use animator instead")
suspend inline fun View.scaleTo(sx: Int, sy: Int, time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) = tween(this::scaleX[sx], this::scaleY[sy], time = time, easing = easing)

@Deprecated("Use animator instead")
suspend inline fun View.rotateTo(angle: Angle, time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) =
    tween(this::rotation[angle], time = time, easing = easing)

@Deprecated("Use animator instead")
suspend inline fun View.rotateBy(deltaAngle: Angle, time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) =
    tween(this::rotation[rotation + deltaAngle], time = time, easing = easing)

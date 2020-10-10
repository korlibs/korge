@file:Suppress("NOTHING_TO_INLINE")

package com.soywiz.korge3d.tween

import com.soywiz.klock.*
import com.soywiz.korge.tween.*
import com.soywiz.korge3d.View3D
import com.soywiz.korma.interpolation.*

//suspend fun View3D.show(time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) =
//	tween(this::alpha[1.0], time = time, easing = easing) { this.visible = true }

//suspend fun View3D.hide(time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) =
//	tween(this::alpha[0.0], time = time, easing = easing)

suspend inline fun View3D.moveTo(x: Float, y: Float, z: Float, time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) = tween(this::x[x], this::y[y], this::z[z], time = time, easing = easing)
suspend inline fun View3D.moveBy(dx: Float, dy: Float, dz: Float, time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) = tween(this::x[this.x + dx], this::y[this.y + dy], this::z[this.z + dz], time = time, easing = easing)
suspend inline fun View3D.scaleTo(sx: Double, sy: Double, sz: Float, time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) = tween(this::scaleX[sx], this::scaleY[sy], this::scaleZ[sz], time = time, easing = easing)

//suspend inline fun View3D.rotateTo(deg: Angle, time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) =
//	tween(this::rotationRadians[deg.radians], time = time, easing = easing)

//suspend inline fun View3D.rotateBy(ddeg: Angle, time: TimeSpan = DEFAULT_TIME, easing: Easing = DEFAULT_EASING) =
//	tween(this::rotationRadians[this.rotationRadians + ddeg.radians], time = time, easing = easing)

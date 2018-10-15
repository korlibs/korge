package com.soywiz.korge.view

import com.soywiz.korge.time.TimeSpan
import com.soywiz.korge.tween.Easing
import com.soywiz.korge.tween.V2
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korma.Matrix2d
import com.soywiz.korma.geom.Rectangle

class Camera(views: Views) : Container(views) {
	override var width: Double = views.virtualWidth.toDouble()
	override var height: Double = views.virtualHeight.toDouble()

	override fun getLocalBoundsInternal(out: Rectangle) {
		out.setTo(0, 0, width, height)
	}

	fun getLocalMatrixFittingGlobalRect(rect: Rectangle): Matrix2d {
		val destinationBounds = rect
		val mat = this.parent?.globalMatrix?.clone() ?: Matrix2d()
		mat.translate(-destinationBounds.x, -destinationBounds.y)
		mat.scale(
			width / destinationBounds.width,
			height / destinationBounds.height
		)
		//println(identityBounds)
		//println(destinationBounds)
		return mat
	}

	fun getLocalMatrixFittingView(view: View?): Matrix2d = getLocalMatrixFittingGlobalRect((view ?: views.stage).globalBounds)

	fun setTo(view: View?) = run { this.localMatrix = getLocalMatrixFittingView(view) }
	fun setTo(rect: Rectangle) = run { this.localMatrix = getLocalMatrixFittingGlobalRect(rect) }

	suspend fun tweenTo(view: View?, vararg vs: V2<*>, time: TimeSpan, easing: Easing = Easing.LINEAR) = this.tween(this::localMatrix[this.localMatrix.clone(), getLocalMatrixFittingView(view)], *vs, time = time, easing = easing)
	suspend fun tweenTo(rect: Rectangle, vararg vs: V2<*>, time: TimeSpan, easing: Easing = Easing.LINEAR) = this.tween(this::localMatrix[this.localMatrix.clone(), getLocalMatrixFittingGlobalRect(rect)], *vs, time = time, easing = easing)
}

fun Views.camera() = Camera(this)

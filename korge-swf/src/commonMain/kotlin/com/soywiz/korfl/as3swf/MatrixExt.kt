package com.soywiz.korfl.as3swf

import com.soywiz.korma.geom.*

fun Matrix.createGradientBox(
	width: Double,
	height: Double,
	rotation: Double = 0.0,
	tx: Double = 0.0,
	ty: Double = 0.0
) {
	this.createBox(width / 1638.4, height / 1638.4, rotation, tx + width / 2, ty + height / 2)
}

fun Matrix.createBox(scaleX: Double, scaleY: Double, rotation: Double = 0.0, tx: Double = 0.0, ty: Double = 0.0) {
	val u = kotlin.math.cos(rotation)
	val v = kotlin.math.sin(rotation)
	this.a = u * scaleX
	this.b = v * scaleY
	this.c = -v * scaleX
	this.d = u * scaleY
	this.tx = tx
	this.ty = ty
}

inline fun Matrix.createBox(scaleX: Number, scaleY: Number, rotation: Number = 0.0, tx: Number = 0.0, ty: Number = 0.0): Unit = createBox(scaleX.toDouble(), scaleY.toDouble(), rotation.toDouble(), tx.toDouble(), ty.toDouble())

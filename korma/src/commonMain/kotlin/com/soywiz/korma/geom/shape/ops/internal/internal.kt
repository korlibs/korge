package com.soywiz.korma.geom.shape.ops.internal

@PublishedApi internal fun min2(a: Int, b: Int) = if (a < b) a else b
@PublishedApi internal fun max2(a: Int, b: Int) = if (a > b) a else b

@PublishedApi internal fun min2(a: Float, b: Float) = if (a < b) a else b
@PublishedApi internal fun max2(a: Float, b: Float) = if (a > b) a else b

@PublishedApi internal fun min2(a: Double, b: Double) = if (a < b) a else b
@PublishedApi internal fun max2(a: Double, b: Double) = if (a > b) a else b

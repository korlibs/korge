package com.soywiz.korim.vector.chart

import com.soywiz.korim.vector.Context2d
import com.soywiz.korim.vector.Drawable

abstract class Chart() : Drawable {
	abstract fun Context2d.renderChart()
}


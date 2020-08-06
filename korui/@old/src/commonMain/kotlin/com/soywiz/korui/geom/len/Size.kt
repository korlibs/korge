package com.soywiz.korui.geom.len

data class Size(var width: Length? = null, var height: Length? = null) {
	fun copyFrom(other: Size) = setTo(other.width, other.height)

	fun setTo(width: Length?, height: Length?) = this.apply {
		this.width = width
		this.height = height
	}

	fun setToScale(sX: Double, sY: Double = sX) = this.apply {
		this.setTo(this.width * sX, this.height * sY)
	}
}
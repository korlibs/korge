package com.soywiz.korui.geom.len

data class Padding(var top: Length? = Length.ZERO, var right: Length? = Length.ZERO, var bottom: Length? = Length.ZERO, var left: Length? = Length.ZERO) {
	constructor(vertical: Length?, horizontal: Length?) : this(vertical, horizontal, vertical, horizontal)
	constructor(pad: Length?) : this(pad, pad, pad, pad)

	fun setTo(top: Length?, right: Length?, bottom: Length?, left: Length?) = this.apply {
		this.top = top
		this.right = right
		this.bottom = bottom
		this.left = left
	}

	fun setTo(vertical: Length?, horizontal: Length?) = setTo(vertical, horizontal, vertical, horizontal)
	fun setTo(pad: Length?) = setTo(pad, pad, pad, pad)
	fun setTo(pad: Padding) = setTo(pad.top, pad.right, pad.bottom, pad.left)
}

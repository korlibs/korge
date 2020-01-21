package com.soywiz.korge.ui

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*

data class UISkin(
	val normal: BmpSlice,
	val hover: BmpSlice = normal,
	val down: BmpSlice = normal,
	val disabled: BmpSlice = normal,
	val backColor: RGBA = Colors.DARKGREY
)

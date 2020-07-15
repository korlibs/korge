package com.soywiz.korim.bitmap.effect

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*

// @TODO: Blending modes
fun Bitmap32.glow(r: Int, color: RGBA = Colors.BLACK): Bitmap32 = dropShadow(0, 0, r, color)
fun Bitmap32.glowInplace(r: Int, color: RGBA = Colors.BLACK): Bitmap32 = dropShadowInplace(0, 0, r, color)

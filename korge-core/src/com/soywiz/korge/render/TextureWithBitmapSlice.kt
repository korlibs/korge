package com.soywiz.korge.render

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.BitmapSlice

data class TextureWithBitmapSlice(val texture: Texture, val bitmapSlice: BitmapSlice<Bitmap>)

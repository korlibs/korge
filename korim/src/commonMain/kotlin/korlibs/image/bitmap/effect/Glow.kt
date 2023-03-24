package korlibs.image.bitmap.effect

import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.Colors
import korlibs.image.color.RGBA

// @TODO: Blending modes
fun Bitmap32.glow(r: Int, color: RGBA = Colors.BLACK): Bitmap32 = dropShadow(0, 0, r, color)
fun Bitmap32.glowInplace(r: Int, color: RGBA = Colors.BLACK): Bitmap32 = dropShadowInplace(0, 0, r, color)

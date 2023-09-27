package korlibs.image.bitmap.effect

import korlibs.image.bitmap.Bitmap32
import korlibs.image.color.Colors
import korlibs.image.color.RGBA
import kotlin.math.absoluteValue
import kotlin.math.max

data class BitmapEffect(
    // Blur
    var blurRadius: Int = 0,
    // Drop Shadow
    var dropShadowX: Int = 0,
    var dropShadowY: Int = 0,
    var dropShadowRadius: Int = 0,
    var dropShadowColor: RGBA = Colors.BLACK,
    // Border
    var borderSize: Int = 0,
    var borderColor: RGBA = Colors.BLACK
) {
    val safeBorder get() = max(max(max(blurRadius, dropShadowX.absoluteValue), dropShadowY.absoluteValue), borderSize)
}

val BitmapEffect?.safeBorder get() = this?.safeBorder ?: 0

fun Bitmap32.applyEffectInline(effect: BitmapEffect?) {
    if (effect == null) return

    if (effect.blurRadius != 0) {
        this.blurInplace(effect.blurRadius)
    }
    if (effect.dropShadowRadius != 0 || effect.dropShadowX != 0 || effect.dropShadowY != 0) {
        this.dropShadowInplace(effect.dropShadowX, effect.dropShadowY, effect.dropShadowRadius, effect.dropShadowColor)
    }
    if (effect.borderSize != 0) {
        this.borderInline(effect.borderSize, effect.borderColor)
    }
}

fun Bitmap32.applyEffect(effect: BitmapEffect?): Bitmap32 {
    val safeBorder = effect.safeBorder
    val out = Bitmap32(width + safeBorder * 2, height + safeBorder * 2, premultiplied)
    out.put(this, safeBorder, safeBorder)
    out.applyEffectInline(effect)
    return out
}

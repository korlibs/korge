package samples.fleks.components

import com.soywiz.korge.view.Image
import com.soywiz.korge.view.animation.ImageAnimationView
import com.soywiz.korim.bitmap.Bitmaps

/**
 * The sprite component adds visible details to an entity. By adding sprite to an entity the entity will be
 * visible on the screen.
 */
data class Sprite(
    var imageData: String = "",
    var animation: String = "",
    var isPlaying: Boolean = false,
    var forwardDirection: Boolean = true,
    var loop: Boolean = false,
    // internal data
    var imageAnimView: ImageAnimationView<Image> = ImageAnimationView { Image(Bitmaps.transparent) }.apply { smoothing = false }
)

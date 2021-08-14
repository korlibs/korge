package com.soywiz.korge.view.animation

import com.soywiz.korge.view.*
import com.soywiz.korim.format.*

inline fun Container.imageDataView(
    data: ImageData? = null, animation: String? = null,
    playing: Boolean = false, smoothing: Boolean = true,
    block: @ViewDslMarker ImageDataView.() -> Unit = {}
)
    = ImageDataView(data, animation, playing, smoothing).addTo(this, block)

/**
 * @example
 *
 * val ase = resourcesVfs["vampire.ase"].readImageDataWithAtlas(ASE)
 *
 * val character = imageDataView(ase, "down") { stop() }
 *
 * addUpdater {
 *     val left = keys[Key.LEFT]
 *     val right = keys[Key.RIGHT]
 *     val up = keys[Key.UP]
 *     val down = keys[Key.DOWN]
 *     if (left) character.x -= 2.0
 *     if (right) character.x += 2.0
 *     if (up) character.y -= 2.0
 *     if (down) character.y += 2.0
 *     character.animation = when {
 *         left -> "left"; right -> "right"; up -> "up"; down -> "down"
 *         else -> character.animation
 *     }
 *     if (left || right || up || down) {
 *         character.play()
 *     } else {
 *         character.stop()
 *         character.rewind()
 *     }
 * }
 */
open class ImageDataView(
    data: ImageData? = null,
    animation: String? = null,
    playing: Boolean = false,
    smoothing: Boolean = true,
) : Container() {
    private val animationView = imageAnimationView()

    var smoothing: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                animationView.smoothing = value
            }
        }

    var data: ImageData? = data
        set(value) {
            if (field !== value) {
                field = value
                updatedDataAnimation()
            }
        }

    var animation: String? = animation
        set(value) {
            if (field !== value) {
                field = value
                updatedDataAnimation()
            }
        }

    val animationNames: Set<String> get() = data?.animationsByName?.keys ?: emptySet()

    init {
        updatedDataAnimation()
        if (playing) play() else stop()
        this.smoothing = smoothing
    }

    fun play() { animationView.play() }
    fun stop() { animationView.stop() }
    fun rewind() { animationView.rewind() }

    private fun updatedDataAnimation() {
        animationView.animation = if (animation != null) data?.animationsByName?.get(animation) else data?.defaultAnimation
    }
}

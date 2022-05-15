package com.soywiz.korge.view.animation

import com.soywiz.korge.view.Container
import com.soywiz.korge.view.View
import com.soywiz.korge.view.ViewDslMarker
import com.soywiz.korge.view.addTo
import com.soywiz.korim.format.ImageData

/**
 * With imageDataView it is possible to display an image inside a Container or View.
 * It supports layers and animations. Animations consist of a series of frames which
 * are defined e.g. by tag names in Aseprite files.
 *
 * The image can be repeating in X and/or Y direction. That needs to be enabled by setting
 * repeating to true. The repeating values can be set per layer as repeatX and repeatY.
 */
inline fun Container.imageDataView(
    data: ImageData? = null,
    animation: String? = null,
    playing: Boolean = false,
    smoothing: Boolean = true,
    repeating: Boolean = false,
    block: @ViewDslMarker ImageDataView.() -> Unit = {}
)
    = ImageDataView(data, animation, playing, smoothing, repeating).addTo(this, block)

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
    repeating: Boolean = false
) : Container() {
    private val animationView = if (repeating) repeatedImageAnimationView() else imageAnimationView()

    fun getLayer(name: String): View? {
        return animationView.getLayer(name)
    }

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

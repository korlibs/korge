package korlibs.korge.view.animation

import korlibs.korge.view.*
import korlibs.image.format.ImageData
import korlibs.korge.view.property.*
import korlibs.math.geom.*

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
) : Container(), PixelAnchorable, Anchorable {
    // Here we can create repeated in korge-parallax if required
    protected open fun createAnimationView(): ImageAnimationView<out SmoothedBmpSlice> {
        return imageAnimationView()
    }

    open val animationView: ImageAnimationView<out SmoothedBmpSlice> = createAnimationView()
    override var anchorPixel: Point by animationView::anchorPixel
    override var anchor: Anchor by animationView::anchor

    fun getLayer(name: String): View? {
        return animationView.getLayer(name)
    }

    @ViewProperty
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

    @ViewProperty
    @ViewPropertyProvider(AnimationProvider::class)
    var animation: String? = animation
        set(value) {
            if (field !== value) {
                field = value
                updatedDataAnimation()
            }
        }

    object AnimationProvider : ViewPropertyProvider.ListImpl<ImageDataView, String>() {
        override fun listProvider(instance: ImageDataView): List<String> = instance.animationNames.toList()
    }

    val animationNames: Set<String> get() = data?.animationsByName?.keys ?: emptySet()

    init {
        updatedDataAnimation()
        if (playing) play() else stop()
        this.smoothing = smoothing
    }

    @ViewProperty
    fun play() { animationView.play() }
    @ViewProperty
    fun stop() { animationView.stop() }
    @ViewProperty
    fun rewind() { animationView.rewind() }

    private fun updatedDataAnimation() {
        animationView.animation = if (animation != null) data?.animationsByName?.get(animation) else data?.defaultAnimation
    }
}

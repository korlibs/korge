package com.soywiz.korge.view.animation

import com.soywiz.kds.*
import com.soywiz.kds.iterators.*
import com.soywiz.klock.*
import com.soywiz.kmem.*
import com.soywiz.korge.view.*
import com.soywiz.korge.view.tiles.SingleTile
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*

inline fun Container.imageAnimationView(
    animation: ImageAnimation? = null,
    direction: ImageAnimation.Direction? = null,
    block: @ViewDslMarker ImageAnimationView<Image>.() -> Unit = {}
) = ImageAnimationView(animation, direction) { Image(Bitmaps.transparent) }.addTo(this, block)

inline fun Container.repeatedImageAnimationView(
    animation: ImageAnimation? = null,
    direction: ImageAnimation.Direction? = null,
    block: @ViewDslMarker ImageAnimationView<SingleTile>.() -> Unit = {}
) = ImageAnimationView(animation, direction) { SingleTile(Bitmaps.transparent) }.addTo(this, block)

open class ImageAnimationView<T: SmoothedBmpSlice>(
    animation: ImageAnimation? = null,
    direction: ImageAnimation.Direction? = null,
    val createImage: () -> T
) : Container() {
    private var nframes: Int = 1

    var animation: ImageAnimation? = animation
        set(value) {
            if (field !== value) {
                field = value
                didSetAnimation()
            }
        }
    var direction: ImageAnimation.Direction? = direction

    private val computedDirection: ImageAnimation.Direction get() = direction ?: animation?.direction ?: ImageAnimation.Direction.FORWARD
    private val layers = fastArrayListOf<T>()
    private val layersByName = FastStringMap<T>()
    private var nextFrameIn = 0.milliseconds
    private var nextFrameIndex = 0
    private var dir = +1

    fun getLayer(name: String): View? {
        return layersByName[name] as View?
    }

    var smoothing: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                layers.fastForEach { it.smoothing = value }
            }
        }


    private fun setFrame(frameIndex: Int) {
        val frame = animation?.frames?.getCyclicOrNull(frameIndex)
        if (frame != null) {
            frame.layerData.fastForEach {
                val image = layers[it.layer.index]
                image.bitmap = it.slice
                (image as View).xy(it.targetX, it.targetY)
            }
            nextFrameIn = frame.duration
            dir = when (computedDirection) {
                ImageAnimation.Direction.FORWARD -> +1
                ImageAnimation.Direction.REVERSE -> -1
                ImageAnimation.Direction.PING_PONG -> if (frameIndex + dir !in 0 until nframes) -dir else dir
            }
            nextFrameIndex = (frameIndex + dir) umod nframes
        } else {
            layers.fastForEach { it.bitmap = Bitmaps.transparent }
        }
    }

    private fun setFirstFrame() {
        if (computedDirection == ImageAnimation.Direction.REVERSE) {
            setFrame(nframes - 1)
        } else {
            setFrame(0)
        }
    }

    private fun didSetAnimation() {
        nframes = animation?.frames?.size ?: 1
        layers.clear()
        removeChildren()
        dir = +1
        val animation = this.animation
        if (animation != null) {
            for (layer in animation.layers) {
                val image = createImage()
                image.smoothing = smoothing
                layers.add(image)
                layersByName[layer.name ?: "default"] = image
                addChild(image as View)
            }
        }
        setFirstFrame()
    }

    private var running = true
    fun play() { running = true }
    fun stop() { running = false }
    fun rewind() { setFirstFrame() }

    init {
        didSetAnimation()
        addUpdater {
            //println("running=$running, nextFrameIn=$nextFrameIn, nextFrameIndex=$nextFrameIndex")
            if (running) {
                nextFrameIn -= it
                if (nextFrameIn <= 0.0.milliseconds) {
                    setFrame(nextFrameIndex)
                }
            }
        }
    }
}

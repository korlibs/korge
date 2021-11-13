package com.soywiz.korge.view.animation

import com.soywiz.kds.*
import com.soywiz.kds.iterators.fastForEach
import com.soywiz.klock.milliseconds
import com.soywiz.kmem.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.ImageAnimation

inline fun Container.repeatedImageAnimationView(
    animation: ImageAnimation? = null,
    direction: ImageAnimation.Direction? = null,
    repeatX: Boolean = false,
    repeatY: Boolean = false,
    block: @ViewDslMarker RepeatedImageAnimationView.() -> Unit = {}
) = RepeatedImageAnimationView(animation, direction, repeatX, repeatY).addTo(this, block)

open class RepeatedImageAnimationView(
    animation: ImageAnimation? = null,
    direction: ImageAnimation.Direction? = null,
    var repeatX: Boolean = false,
    var repeatY: Boolean = false
) : AbstractImageAnimationView() {
    private var nframes: Int = 1

    override var animation: ImageAnimation? = animation
        set(value) {
            if (field !== value) {
                field = value
                didSetAnimation()
            }
        }
    var direction: ImageAnimation.Direction? = direction

    private val computedDirection: ImageAnimation.Direction get() = direction ?: animation?.direction ?: ImageAnimation.Direction.FORWARD
    private val layers = fastArrayListOf<RepeatedImageView>()
    private val layersByName = FastStringMap<RepeatedImageView>()
    private var nextFrameIn = 0.milliseconds
    private var nextFrameIndex = 0
    private var dir = +1

    override fun getLayer(name: String): View? {
        return layersByName[name]
    }

    override var smoothing: Boolean = true
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
                image.xy(it.targetX, it.targetY)
            }
            nextFrameIn = frame.duration
            dir = when (computedDirection) {
                ImageAnimation.Direction.FORWARD -> +1
                ImageAnimation.Direction.REVERSE -> -1
                ImageAnimation.Direction.PING_PONG -> if (frame.index + dir !in 0 until nframes) -dir else dir
            }
            nextFrameIndex = (frame.index + dir) umod nframes
        } else {
            layers.fastForEach { it.bitmap = Bitmaps.transparent }
        }
    }

    override fun setFirstFrame() {
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
                val image = RepeatedImageView(Bitmaps.transparent, smoothing, repeatX, repeatY)
                layers.add(image)
                layersByName[layer.name ?: "default"] = image
                addChild(image)
            }
        }
        setFirstFrame()
    }

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


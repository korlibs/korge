package com.soywiz.korge.view

import SpriteAnimation
import com.soywiz.klock.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korio.async.Signal
import com.soywiz.korma.geom.vector.VectorPath

inline fun Container.sprite(
    initialAnimation: SpriteAnimation, anchorX: Double = 0.0, anchorY: Double = 0.0, callback: @ViewsDslMarker Sprite.() -> Unit = {}
): Sprite = Sprite(initialAnimation, anchorX, anchorY).addTo(this).apply(callback)

inline fun Container.sprite(
    texture: BmpSlice, anchorX: Double = 0.0, anchorY: Double = 0.0, callback: @ViewsDslMarker Sprite.() -> Unit = {}
): Sprite = Sprite(texture, anchorX, anchorY).addTo(this).apply(callback)

inline fun Container.sprite(
    texture: Bitmap, anchorX: Double = 0.0, anchorY: Double = 0.0, callback: @ViewsDslMarker Sprite.() -> Unit = {}
): Sprite = Sprite(texture, anchorX, anchorY).addTo(this).apply(callback)

/**
 * A [Sprite] is basically an [Image] with added abilities to display a [SpriteAnimation]
 * The regular usage is to initialize the [Sprite] with one [SpriteAnimation]. The first
 * displayed bitmap will be the first element of the [SpriteAnimation]s spriteStack.
 * @property animationRequested Boolean
 * @property animationNumberOfFramesRequested Int
 * @property onAnimationCompleted Signal<SpriteAnimation>
 * @property onAnimationStopped Signal<SpriteAnimation>
 * @property onAnimationStarted Signal<SpriteAnimation>
 * @property animationLooped Boolean
 * @property lastAnimationFrameTime TimeSpan
 * @property animationRequestedDuration TimeSpan
 * @property spriteDisplayTime TimeSpan
 * @property currentAnimation SpriteAnimation?
 * @property currentSpriteIndex Int
 * @constructor It is possible to initialize a [Sprite] with a static [Bitmap] or [BmpSlice].
 * This will be exchanged when starting a [SpriteAnimation] with one of the available play functions
 */
open class Sprite(
    bitmap : Bitmap,
    anchorX: Double = 0.0,
    anchorY: Double = anchorX,
    hitShape: VectorPath? = null,
    smoothing: Boolean = true) : Image(bitmap, anchorX, anchorY, hitShape, smoothing) {
    constructor(
        bmpSlice : BmpSlice,
        anchorX: Double = 0.0,
        anchorY: Double = anchorX,
        hitShape: VectorPath? = null,
        smoothing: Boolean = true) : this(bmpSlice.bmp, anchorX, anchorY, hitShape, smoothing)
    constructor(
        initialAnimation : SpriteAnimation,
        anchorX: Double = 0.0,
        anchorY: Double = anchorX,
        hitShape: VectorPath? = null,
        smoothing: Boolean = true) : this(initialAnimation.firstSprite, anchorX, anchorY, hitShape, smoothing){
        currentAnimation = initialAnimation
        setFrame(0)
    }

    private var animationRequested = false
    private var animationNumberOfFramesRequested = 0
        set(value) {
            if (value == 0)
                triggerEvent(onAnimationCompleted)
            field = value
        }
    val onAnimationCompleted = Signal<SpriteAnimation>()
    val onAnimationStopped = Signal<SpriteAnimation>()
    val onAnimationStarted = Signal<SpriteAnimation>()

    var spriteDisplayTime : TimeSpan = 50.milliseconds
    private var animationLooped = false
    private var lastAnimationFrameTime  : TimeSpan = 0.milliseconds
    private var animationRequestedDuration : TimeSpan = 0.milliseconds

    private var currentAnimation : SpriteAnimation? = null
    private var currentSpriteIndex = 0
    private var reversed = false

    init {
        addUpdater { frameTime ->
            if (animationRequested){
                nextSprite(frameTime)
            }
        }
    }

    fun playAnimation(
        spriteAnimation: SpriteAnimation? = currentAnimation,
        spriteDisplayTime: TimeSpan = this.spriteDisplayTime,
        startFrame : Int? = null,
        reversed : Boolean = false) =
        updateCurrentAnimation(
            spriteAnimation = spriteAnimation,
            spriteDisplayTime = spriteDisplayTime,
            startFrame = startFrame ?: currentSpriteIndex,
            reversed = reversed)

    fun playAnimation(
        times: Int = 1,
        spriteAnimation: SpriteAnimation? = currentAnimation,
        spriteDisplayTime: TimeSpan = this.spriteDisplayTime,
        startFrame: Int? = null,
        reversed : Boolean = false) =
        updateCurrentAnimation(
            spriteAnimation = spriteAnimation,
            spriteDisplayTime = spriteDisplayTime,
            animationCyclesRequested = times*(currentAnimation?.spriteStackSize ?: 0),
            startFrame = startFrame ?: currentSpriteIndex,
            reversed = reversed
        )

    fun playAnimationForDuration(
        duration: TimeSpan,
        spriteAnimation: SpriteAnimation? = currentAnimation,
        spriteDisplayTime: TimeSpan = this.spriteDisplayTime,
        startFrame: Int? = null,
        reversed : Boolean = false) =
        updateCurrentAnimation(
            spriteAnimation = spriteAnimation,
            spriteDisplayTime = spriteDisplayTime,
            duration = duration,
            startFrame = startFrame ?: currentSpriteIndex,
            reversed = reversed
        )

    fun playAnimationLooped(
        spriteAnimation: SpriteAnimation? = currentAnimation,
        spriteDisplayTime: TimeSpan = this.spriteDisplayTime,
        startFrame: Int? = null,
        reversed : Boolean = false) =
        updateCurrentAnimation(
            spriteAnimation = spriteAnimation,
            spriteDisplayTime = spriteDisplayTime,
            startFrame = startFrame ?: currentSpriteIndex,
            looped = true,
            reversed = reversed
        )

    fun stopAnimation() {
        animationRequested = false
        triggerEvent(onAnimationStopped)
    }

    private fun nextSprite(frameTime : TimeSpan){
        lastAnimationFrameTime+=frameTime
        if ((animationNumberOfFramesRequested > 0 || animationRequestedDuration > 0.milliseconds || animationLooped) && lastAnimationFrameTime+frameTime >= this.spriteDisplayTime){
            setFrame(if (reversed) --currentSpriteIndex else ++currentSpriteIndex)
            animationNumberOfFramesRequested--
            animationRequestedDuration-=(frameTime+spriteDisplayTime)
            lastAnimationFrameTime = 0.milliseconds
        }
    }

    private fun updateCurrentAnimation(
        spriteAnimation: SpriteAnimation?,
        spriteDisplayTime: TimeSpan = this.spriteDisplayTime,
        animationCyclesRequested : Int = 1,
        duration : TimeSpan = 0.milliseconds,
        startFrame: Int = 0,
        looped : Boolean = false,
        reversed : Boolean = false
    ){
        triggerEvent(onAnimationStarted)
        this.spriteDisplayTime = spriteDisplayTime
        currentAnimation = spriteAnimation
        animationRequested = true
        animationLooped = looped
        animationRequestedDuration = duration
        currentSpriteIndex = startFrame
        this.reversed = reversed
        currentAnimation?.let {
            this.animationNumberOfFramesRequested = if (!looped) it.spriteStackSize-1 else 1
        }
    }

    fun setFrame(index : Int)  {
        bitmap = currentAnimation?.getSprite(index) ?:  bitmap
    }

    private fun triggerEvent(signal : Signal<SpriteAnimation>) = currentAnimation?.let { signal.invoke(it) }
}


package com.soywiz.korge.view

import com.soywiz.klock.*
import com.soywiz.korge.view.Image
import com.soywiz.korge.view.addUpdater
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmaps
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
 * @property animationCyclesRequested Int
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
class Sprite(
        bitmap : Bitmap,
        anchorX: Double = 0.0,
        anchorY: Double = anchorX,
        hitShape: VectorPath? = null,
        smoothing: Boolean = true) : Image(bitmap) {
    constructor(
        bmpSlice : BmpSlice,
        anchorX: Double = 0.0,
        anchorY: Double = anchorX,
        hitShape: VectorPath? = null,
        smoothing: Boolean = true) : this(bmpSlice.bmp)
    constructor(
        initialAnimation : SpriteAnimation,
        anchorX: Double = 0.0,
        anchorY: Double = anchorX,
        hitShape: VectorPath? = null,
        smoothing: Boolean = true) : this(initialAnimation.firstSprite){
        currentAnimation = initialAnimation
        bitmap = currentAnimation?.firstSprite ?: Bitmaps.transparent
    }

    private var animationRequested = false
    private var animationCyclesRequested = 0
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

    init {
        addUpdater { frameTime ->
            if (animationRequested){
                nextSprite(frameTime)
            }
        }
    }


    fun playAnimation(spriteAnimation: SpriteAnimation, spriteDisplayTime: TimeSpan = this.spriteDisplayTime) = updateCurrentAnimation(spriteAnimation = spriteAnimation, spriteDisplayTime = spriteDisplayTime)

    fun playAnimation(times: Int = 1, spriteAnimation: SpriteAnimation, spriteDisplayTime: TimeSpan = this.spriteDisplayTime) =
        updateCurrentAnimation(
            spriteAnimation = spriteAnimation,
            spriteDisplayTime = spriteDisplayTime,
            animationCyclesRequested = times*(currentAnimation?.spriteStackSize ?: 0)
        )

    fun playAnimationForDuration(duration: TimeSpan, spriteAnimation: SpriteAnimation, spriteDisplayTime: TimeSpan = this.spriteDisplayTime) =
        updateCurrentAnimation(
            spriteAnimation = spriteAnimation,
            spriteDisplayTime = spriteDisplayTime,
            duration = duration
        )

    fun playAnimationLooped(spriteAnimation: SpriteAnimation, spriteDisplayTime: TimeSpan = this.spriteDisplayTime) =
        updateCurrentAnimation(
            spriteAnimation = spriteAnimation,
            spriteDisplayTime = spriteDisplayTime,
            looped = true
        )

    fun stopAnimation() {
        animationRequested = false
        triggerEvent(onAnimationStopped)
    }

    private fun nextSprite(frameTime : TimeSpan){
        lastAnimationFrameTime+=frameTime
        if ((animationCyclesRequested > 0 || animationRequestedDuration > 0.milliseconds || animationLooped) && lastAnimationFrameTime+frameTime >= this.spriteDisplayTime){
            bitmap = currentAnimation?.getSprite(++currentSpriteIndex) ?: Bitmaps.transparent
            animationCyclesRequested--
            animationRequestedDuration-=(frameTime+spriteDisplayTime)
            lastAnimationFrameTime = 0.milliseconds
        }
    }

    private fun updateCurrentAnimation(
        spriteAnimation: SpriteAnimation,
        spriteDisplayTime: TimeSpan = this.spriteDisplayTime,
        animationCyclesRequested : Int = 1,
        duration : TimeSpan = 0.milliseconds,
        looped : Boolean = false
    ){
        triggerEvent(onAnimationStarted)
        this.spriteDisplayTime = spriteDisplayTime
        currentAnimation = spriteAnimation
        animationRequested = true
        animationLooped = looped
        animationRequestedDuration = duration
        this.animationCyclesRequested = if (!looped) animationCyclesRequested else 1
    }

    private fun triggerEvent(signal : Signal<SpriteAnimation>) = currentAnimation?.let { signal.invoke(it) }
}

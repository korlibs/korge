package me.emig.engineEmi.graphics.animationen

import com.soywiz.klock.*
import com.soywiz.korge.view.Image
import com.soywiz.korge.view.addUpdater
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.Bitmaps
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korio.async.Signal

class Sprite(bitmap : Bitmap) : Image(bitmap) {
    constructor(bmpSlice : BmpSlice) : this(bmpSlice.bmp)
    constructor(initialAnimation : SpriteAnimation) : this(initialAnimation.firstSprite){
        currentAnimation = initialAnimation
        bitmap = currentAnimation?.firstSprite ?: Bitmaps.transparent
    }
    var animationRequested = false
    var animationCyclesRequested = 0
        set(value) {
            if (value == 0)
                currentAnimation?.let {
                    animationCompleted(it)
                }
            field = value
        }
    var animationCompleted = Signal<SpriteAnimation>()
    var animationLooped = false
    var lastAnimationFrameTime  : TimeSpan = 0.milliseconds
    var animationRequestedDuration : TimeSpan = 0.milliseconds
    var spriteDisplayTime : TimeSpan = 25.milliseconds
    var currentAnimation : SpriteAnimation? = null
    var currentSpriteIndex = 0

    init {
        addUpdater { frameTime ->
            if (animationRequested){
                nextSprite(frameTime)
            }
        }
    }

    fun playAnimation(spriteAnimation: SpriteAnimation) = updateCurrentAnimation(spriteAnimation = spriteAnimation)

    fun playAnimationForDuration(duration: TimeSpan, spriteAnimation: SpriteAnimation, spriteDisplayTime: TimeSpan = 25.milliseconds) =
        updateCurrentAnimation(
            spriteAnimation = spriteAnimation,
            spriteDisplayTime = spriteDisplayTime,
            duration = duration
        )

    fun playAnimation(times: Int = 1, spriteAnimation: SpriteAnimation, spriteDisplayTime: TimeSpan = 25.milliseconds) =
        updateCurrentAnimation(
            spriteAnimation = spriteAnimation,
            spriteDisplayTime = spriteDisplayTime,
            animationCyclesRequested = times*(currentAnimation?.spriteStackSize ?: 0)
        )

    fun playAnimationLooped(spriteAnimation: SpriteAnimation, spriteDisplayTime: TimeSpan = 25.milliseconds) =
        updateCurrentAnimation(
            spriteAnimation = spriteAnimation,
            spriteDisplayTime = spriteDisplayTime,
            looped = true
        )

    fun stopAnimation() {
        animationRequested = false
    }

    private fun nextSprite(frameTime : TimeSpan){
        lastAnimationFrameTime+=frameTime
        if ((animationCyclesRequested > 0 || animationRequestedDuration > 0.milliseconds || animationLooped) && lastAnimationFrameTime+frameTime >= this.spriteDisplayTime){
            bitmap = currentAnimation?.getSprite(++currentSpriteIndex) ?: Bitmaps.transparent
            animationCyclesRequested--
            animationRequestedDuration-=frameTime
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
        this.spriteDisplayTime = spriteDisplayTime
        currentAnimation = spriteAnimation
        animationRequested = true
        animationLooped = looped
        animationRequestedDuration = duration
        this.animationCyclesRequested = if (!looped) animationCyclesRequested else 1
    }
}

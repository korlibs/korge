package me.emig.engineEmi.graphics.animationen

import com.soywiz.klock.TimeSpan
import com.soywiz.klock.measureTime
import com.soywiz.klock.milliseconds
import com.soywiz.korge.view.Image
import com.soywiz.korio.async.delay

class Sprite(vararg var animations: SpriteAnimation) : Image(animations[0].firstSprite) {
    var stop = false
    var currentAnimation : SpriteAnimation = animations[0]

    suspend fun playAnimationForDuration(duration: TimeSpan, id : String = "", spriteDisplayTime: TimeSpan = currentAnimation.frameTime) {
        renewCurrentAnimation(id)
        stop = false
        if (duration > 0.milliseconds) {
            var timeCounter = duration
            while (timeCounter > 0.milliseconds && !stop ) {
                val measuredTime = measureTime {
                    nextSprite()
                    delay(spriteDisplayTime)
                }
                timeCounter -= measuredTime
            }
        }
    }

    suspend fun playAnimation(id : String = "") {
        renewCurrentAnimation(id)
        nextSprite()
    }

    suspend fun playAnimation(times: Int = 1,  id : String = "", spriteDisplayTime: TimeSpan = currentAnimation.frameTime) {
        renewCurrentAnimation(id)
        stop = false
        val cycleCount = times*currentAnimation.spriteStackSize
        var cycles = 0
        while (cycles < cycleCount && !stop) {
            nextSprite()
            delay(spriteDisplayTime)
            cycles++
        }
    }

    suspend fun playAnimationLooped( id : String = "", spriteDisplayTime: TimeSpan = currentAnimation.frameTime) {
        renewCurrentAnimation(id)
        stop = false
        while (!stop) {
            nextSprite()
            delay(spriteDisplayTime)
        }
    }

    fun stopAnimation() {
        stop = true
    }

    fun nextSprite(){
        bitmap = currentAnimation.nextSprite()
    }

    fun previousSprite(){
        bitmap = currentAnimation.previousSprite()
    }

    private fun renewCurrentAnimation(id : String){
        currentAnimation = animations.find{ it.id == id } ?: animations[0]
    }
}

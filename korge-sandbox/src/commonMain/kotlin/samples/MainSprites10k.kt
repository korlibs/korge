package samples

import com.soywiz.klock.milliseconds
import com.soywiz.korge.Korge
import com.soywiz.korge.render.BatchBuilder2D
import com.soywiz.korge.scene.Scene
import com.soywiz.korge.view.SContainer
import com.soywiz.korge.view.Sprite
import com.soywiz.korge.view.SpriteAnimation
import com.soywiz.korge.view.addUpdater
import com.soywiz.korge.view.scale
import com.soywiz.korge.view.sprite
import com.soywiz.korge.view.xy
import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.format.readBitmap
import com.soywiz.korio.file.std.resourcesVfs

class MainSprites10k : Scene() {
    override suspend fun SContainer.sceneMain() {
        val numberOfGreen = 5000
        //val numberOfGreen = 20000
        val numberOfRed = numberOfGreen

        val redSpriteMap = resourcesVfs["character.png"].readBitmap()
        val greenSpriteMap = resourcesVfs["character2.png"].readBitmap()

        val greenAnimations = animations(greenSpriteMap)
        val redAnimations = animations(redSpriteMap)

        val greenSprites = Array(numberOfGreen) {
            sprite(greenAnimations[it % greenAnimations.size]).xy((10..views.virtualWidth).random(), (10..views.virtualHeight).random()).scale(2.0)
        }

        val redSprites = Array(numberOfRed) {
            sprite(redAnimations[it % redAnimations.size]).xy((10..views.virtualWidth).random(), (10..views.virtualHeight).random()).scale(2.0)
        }

        greenSprites.forEachIndexed { index, sprite ->
            sprite.playAnimationLooped(greenAnimations[index % greenAnimations.size])
        }
        redSprites.forEachIndexed { index, sprite ->
            sprite.playAnimationLooped(redAnimations[index % redAnimations.size])
        }

        addUpdater {
            val scale = if (it == 0.0.milliseconds) 0.0 else (it / 16.666666.milliseconds)

            greenSprites.forEachIndexed { index, sprite ->
                sprite.walkDirection(index % greenAnimations.size, scale)
            }
            redSprites.forEachIndexed { index, sprite ->
                sprite.walkDirection(index % redAnimations.size, scale)
            }
        }
    }

    fun animations(spriteMap: Bitmap) = arrayOf(
        SpriteAnimation(spriteMap, 16, 32, 96, 1, 4, 1), // left
        SpriteAnimation(spriteMap, 16, 32, 32, 1, 4, 1), // right
        SpriteAnimation(spriteMap, 16, 32, 64, 1, 4, 1), // up
        SpriteAnimation(spriteMap, 16, 32, 0, 1, 4, 1)
    ) // down

    fun Sprite.walkDirection(indexOfAnimation: Int, scale: Double = 1.0) {
        val delta = 2 * scale
        when (indexOfAnimation) {
            0 -> x -= delta
            1 -> x += delta
            2 -> y -= delta
            3 -> y += delta
        }
    }
}

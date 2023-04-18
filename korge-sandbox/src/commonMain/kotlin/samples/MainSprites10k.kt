package samples

import korlibs.image.bitmap.*
import korlibs.image.format.*
import korlibs.io.file.std.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.time.*
import kotlin.random.*

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

        val random = Random(0)
        val randoms = FloatArray(greenSprites.size) { random.nextDouble(0.5, 1.1).toFloat() }

        addUpdater {
            val scale = (if (it == 0.0.milliseconds) 0f else (it / 16.666666.milliseconds))
            //println("time=$it")

            greenSprites.forEachIndexed { index, sprite ->
                sprite.walkDirection(index % greenAnimations.size, scale * randoms[index])
            }
            redSprites.forEachIndexed { index, sprite ->
                sprite.walkDirection(index % redAnimations.size, scale * randoms[index])
            }
        }
    }

    fun animations(spriteMap: Bitmap) = arrayOf(
        SpriteAnimation(spriteMap, 16, 32, 96, 1, 4, 1), // left
        SpriteAnimation(spriteMap, 16, 32, 32, 1, 4, 1), // right
        SpriteAnimation(spriteMap, 16, 32, 64, 1, 4, 1), // up
        SpriteAnimation(spriteMap, 16, 32, 0, 1, 4, 1)
    ) // down

    fun Sprite.walkDirection(indexOfAnimation: Int, scale: Float = 1f) {
        val delta = 2 * scale
        when (indexOfAnimation) {
            0 -> x -= delta
            1 -> x += delta
            2 -> y -= delta
            3 -> y += delta
        }
    }
}

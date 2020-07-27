import com.soywiz.klock.hr.hrMilliseconds
import com.soywiz.korge.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*

suspend fun main() = Korge(width = 1600, height = 1200) {

	val numberOfGreen = 5000
	//val numberOfGreen = 20000
	val numberOfRed = numberOfGreen

	val redSpriteMap = resourcesVfs["character.png"].readBitmap()
	val greenSpriteMap = resourcesVfs["character2.png"].readBitmap()

	val greenAnimations = animations(greenSpriteMap)
	val redAnimations = animations(redSpriteMap)

	val greenSprites = Array(numberOfGreen) {
		sprite(greenAnimations[it % greenAnimations.size]).xy((10..1590).random(), (10..1190).random()).scale(2.0)
	}

	val redSprites = Array(numberOfRed) {
		sprite(redAnimations[it % redAnimations.size]).xy((10..1590).random(), (10..1190).random()).scale(2.0)
	}

	greenSprites.forEachIndexed { index, sprite ->
		sprite.playAnimationLooped(greenAnimations[index % greenAnimations.size])
	}
	redSprites.forEachIndexed { index, sprite ->
		sprite.playAnimationLooped(redAnimations[index % redAnimations.size])
	}

	addHrUpdater {
		val scale = if (it == 0.hrMilliseconds) 0.0 else (it / 16.666666.hrMilliseconds)

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
	SpriteAnimation(spriteMap, 16, 32, 0, 1, 4, 1)) // down

fun Sprite.walkDirection(indexOfAnimation: Int, scale: Double = 1.0) {
	val delta = 2 * scale
	when (indexOfAnimation) {
		0 -> x -= delta
		1 -> x += delta
		2 -> y -= delta
		3 -> y += delta
	}
}

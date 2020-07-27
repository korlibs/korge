import com.soywiz.klock.*
import com.soywiz.korev.*
import com.soywiz.korge.*
import com.soywiz.korge.input.*
import com.soywiz.korge.view.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.std.*

suspend fun main() = Korge(width = 512, height = 512) {
	val spriteMap = resourcesVfs["character.png"].readBitmap()

	val spriteAnimationLeft = SpriteAnimation(
		spriteMap = spriteMap,
		spriteWidth = 16,
		spriteHeight = 32,
		marginTop = 96,
		marginLeft = 1,
		columns = 4,
		rows = 1
	)

	val spriteAnimationRight = SpriteAnimation(
		spriteMap = spriteMap,
		spriteWidth = 16,
		spriteHeight = 32,
		marginTop = 32,
		marginLeft = 1,
		columns = 4,
		rows = 1
	)

	val spriteAnimationUp = SpriteAnimation(
		spriteMap = spriteMap,
		spriteWidth = 16,
		spriteHeight = 32,
		marginTop = 64,
		marginLeft = 1,
		columns = 4,
		rows = 1
	)

	val spriteAnimationDown = SpriteAnimation(
		spriteMap = spriteMap,
		spriteWidth = 16,
		spriteHeight = 32,
		marginTop = 0,
		marginLeft = 1,
		columns = 4,
		rows = 1
	)

	val player1 = Sprite(spriteAnimationDown).apply {
		scale(3.0)
		xy(100, 200)
	}
	val player2 = Sprite(spriteAnimationDown).apply {
		scale(3.0)
		xy(100, 100)
	}

	addChild(player1)
	addChild(player2)

	addUpdater { time ->
		val scale = 16.milliseconds / time
		val disp = 2 * scale
		val keys = views.input.keys
		if (keys[Key.LEFT]) { player1.playAnimation(spriteAnimationLeft); player1.x-=disp }
		if (keys[Key.RIGHT]) { player1.playAnimation(spriteAnimationRight); player1.x+=disp }
		if (keys[Key.DOWN]) { player1.playAnimation(spriteAnimationDown); player1.y+=disp }
		if (keys[Key.UP]) { player1.playAnimation(spriteAnimationUp); player1.y-=disp }
		if (keys[Key.A]) { player2.playAnimation(spriteAnimationLeft); player2.x-=disp }
		if (keys[Key.D]) { player2.playAnimation(spriteAnimationRight); player2.x+=disp }
		if (keys[Key.S]) { player2.playAnimation(spriteAnimationDown); player2.y+=disp }
		if (keys[Key.W]) { player2.playAnimation(spriteAnimationUp); player2.y-=disp }
		if (keys[Key.L]) { player1.playAnimationLooped(spriteAnimationDown, 100.milliseconds) }
		if (keys[Key.T]) { player1.playAnimation(spriteAnimation = spriteAnimationDown, times = 3, spriteDisplayTime = 200.milliseconds) }
		if (keys[Key.C]) { player1.playAnimationForDuration(1.seconds, spriteAnimationDown); player1.y-=2 }
		if (keys[Key.ESCAPE]) { player1.stopAnimation() }
	}
	/*onKeyDown {
		when (it.key) {
			Key.LEFT -> {player1.playAnimation(spriteAnimationLeft); player1.x-=2}
			Key.RIGHT ->{player1.playAnimation(spriteAnimationRight); player1.x+=2}
			Key.DOWN -> {player1.playAnimation(spriteAnimationDown); player1.y+=2}
			Key.UP -> {player1.playAnimation(spriteAnimationUp); player1.y-=2}
			Key.A -> {player2.playAnimation(spriteAnimationLeft); player2.x-=2}
			Key.D -> {player2.playAnimation(spriteAnimationRight); player2.x+=2}
			Key.S -> {player2.playAnimation(spriteAnimationDown); player2.y+=2}
			Key.W -> {player2.playAnimation(spriteAnimationUp); player2.y-=2}
			Key.L -> {player1.playAnimationLooped(spriteAnimationDown, 100.milliseconds)}
			Key.T -> {player1.playAnimation(spriteAnimation = spriteAnimationDown, times = 3, spriteDisplayTime = 200.milliseconds)}
			Key.C -> {player1.playAnimationForDuration(1.seconds, spriteAnimationDown); player1.y-=2}
			Key.ESCAPE -> {player1.stopAnimation()}
			else -> {}
		}
	}*/
}

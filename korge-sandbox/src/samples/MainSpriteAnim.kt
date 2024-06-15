package samples

import korlibs.event.*
import korlibs.image.format.*
import korlibs.io.file.std.*
import korlibs.korge.input.*
import korlibs.korge.scene.*
import korlibs.korge.view.*
import korlibs.korge.view.align.*
import korlibs.time.*

class MainSpriteAnim : ScaledScene(512, 512) {
    companion object {
        const val PADDING = 5.0
    }

    override suspend fun SContainer.sceneMain() {
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

        data class KeyAssignment(
            val key: Key,
            val animation: SpriteAnimation,
            val block: (Float) -> Unit
        )

        /**
         * Extends Sprite with additional state to handle movement/animations
         */
        class PlayerCharacter(
            spriteAnimation: SpriteAnimation,
            upKey: Key, downKey: Key, leftKey: Key, rightKey: Key
        ) : Sprite(spriteAnimation) {

            private val assignments = listOf(
                KeyAssignment(upKey, spriteAnimationUp) { y -= it },
                KeyAssignment(downKey, spriteAnimationDown) { y += it },
                KeyAssignment(leftKey, spriteAnimationLeft) { x -= it },
                KeyAssignment(rightKey, spriteAnimationRight) { x += it },
            )

            /** Allows to know the appropriate moment to stop the movement animation. */
            private var isMoving = false

            val assignedKeyDesc: String
                get() = assignments.map { it.key }.joinToString("/")

            fun handleKeys(inputKeys: InputKeys, disp: Float) {
                // Let's check if any movement keys were pressed during this frame
                val anyMovement: Boolean = assignments // Iterate all registered movement keys
                    .filter { inputKeys[it.key] } // Check if this movement key was pressed
                    .onEach {
                        // If yes, perform its corresponding action and play the corresponding animation
                        it.block(disp)
                        playAnimation(it.animation)
                    }
                    .any()

                if (anyMovement != isMoving) {
                    if (isMoving) stopAnimation()
                    isMoving = anyMovement
                }
            }
        }

        val player1 = PlayerCharacter(spriteAnimationDown, Key.W, Key.S, Key.A, Key.D).apply {
            scale(3.0)
            xy(100, 100)
        }

        text("Player 1 controls: ${player1.assignedKeyDesc}") { position(PADDING, PADDING) }

        val player2 = PlayerCharacter(spriteAnimationDown, Key.UP, Key.DOWN, Key.LEFT, Key.RIGHT).apply {
            scale(3.0)
            xy(300, 100)
        }

        text("Player 2 controls: ${player2.assignedKeyDesc}") {
            positionY(PADDING)
            alignRightToRightOf(parent!!, PADDING)
        }


        addChild(player1)
        addChild(player2)

        addFastUpdater { time ->
            val scale = 16.fastMilliseconds / time
            val disp = 2 * scale
            val keys = views.input.keys

            player1.handleKeys(keys, disp.toFloat())
            player2.handleKeys(keys, disp.toFloat())

            if (keys[Key.L]) { player1.playAnimationLooped(spriteAnimationDown, 100.fastMilliseconds) }
            if (keys[Key.T]) { player1.playAnimation(spriteAnimation = spriteAnimationDown, times = 3, spriteDisplayTime = 200.fastMilliseconds) }
            if (keys[Key.C]) { player1.playAnimationForDuration(1.seconds, spriteAnimationDown); player1.y -= 2 }
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
}

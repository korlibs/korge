package samples.asteroids

import korlibs.event.*
import korlibs.image.color.*
import korlibs.image.font.*
import korlibs.io.async.*
import korlibs.korge.tween.*
import korlibs.korge.view.*
import korlibs.korge.view.align.alignTopToBottomOf
import korlibs.korge.view.align.centerOnStage
import korlibs.korge.view.align.centerXOnStage
import korlibs.math.geom.*
import korlibs.math.random.*
import korlibs.time.*
import kotlin.random.*

const val NUMBER_OF_ASTEROIDS = 15
const val BULLET_SIZE = 14


/**
 * A single game state with all views and updaters (gets destroyed on game restart)
 */
class Game(val scene: MainAsteroids) {
    val assets: Assets get() = scene.assets
    val sceneView: SContainer get() = scene.sceneView
    //val stage: Stage get() = scene.stage

    /** A shortcut to check if a given key is pressed */
    private fun Key.isPressed(): Boolean = scene.input.keys[this]

    private var gameOver = false

    private val ship = Ship()

    init {
        spawnRandomAsteroids(ship.image)
    }

    private val stageUpdater = sceneView.addUpdater { time ->
        if (!gameOver) {
            val scale = time / 16.0.milliseconds

            if (ship.bulletReload > 0) ship.bulletReload -= scale

            if (Key.LEFT.isPressed()) ship.image.rotation -= 3.degrees * scale
            if (Key.RIGHT.isPressed()) ship.image.rotation += 3.degrees * scale
            if (Key.UP.isPressed()) ship.image.advance(2.0 * scale)
            if (Key.DOWN.isPressed()) ship.image.advance(-1.5 * scale)

            if (Key.SPACE.isPressed() && ship.bulletReload <= 0) {
                fireBullet()
            }
        } else {
            if (Key.R.isPressed()) {
                scene.restart()
                //this@Game.scene.stage.dispatch(GameRestartEvent())
            }
        }
    }


    private fun fireBullet() {

        ship.bulletReload = 20.0

        with(sceneView) {
            val bullet = image(assets.bulletBitmap)
                .center()
                .position(ship.image.xD, ship.image.yD)
                .rotation(ship.image.rotation)
                .advance(assets.shipSize * 0.75)

            bullet.onCollision {
                if (it is Asteroid) {
                    bullet.removeFromParent()
                    it.divide()
                }
            }

            bullet.addUpdater {
                val scale = it / 16.milliseconds
                bullet.advance(+3.0 * scale)
                // If the bullet flies off the screen, discard it
                if (bullet.xD < -BULLET_SIZE || bullet.yD < -BULLET_SIZE || bullet.xD > WIDTH + BULLET_SIZE || bullet.yD > HEIGHT + BULLET_SIZE) {
                    bullet.removeFromParent()
                }
            }
        }
    }

    private fun spawnRandomAsteroids(ship: Image) {
        val random = Random
        repeat(NUMBER_OF_ASTEROIDS) {
            val asteroid = spawnAsteroid(0.0, 0.0)
            do {
                asteroid.xD = random[0.0, WIDTH.toDouble()]
                asteroid.yD = random[0.0, HEIGHT.toDouble()]
                asteroid.angle = random[0.0, 360.0].degrees
            } while (asteroid.collidesWith(ship) || ship.distanceTo(asteroid) < 100.0)
        }
    }

    private fun spawnAsteroid(x: Double, y: Double): Asteroid {
        return Asteroid(assets).addTo(sceneView).xy(x, y)
    }

    fun setGameOver() {
        gameOver = true

        ship.image.removeFromParent()

        with(sceneView) {
            // Display the Game Over overlay (with a momentary flash)
            val overlay = solidRect(WIDTH, HEIGHT, Colors.YELLOW)
            scene.launch {
                overlay.tween(overlay::color[Colors.RED.withA(64)], time = 300.milliseconds)
            }

            val gameOverText = text("GAME OVER", 64f, font = DefaultTtfFont)
                .centerOnStage()

            text("Press R to restart")
                .centerXOnStage()
                .alignTopToBottomOf(gameOverText, 10f)
        }
    }

    fun detach() {
        stageUpdater.cancel(GameRestart)
    }

    // The ship controlled by the player
    inner class Ship {
        val image = sceneView.image(scene.assets.shipBitmap)
                .center()
                .position(320, 240)

        // A cooldown period to prevent bullet spamming
        var bulletReload = 0.0

        init {
            image.onCollision {
                if (it is Asteroid) {
                    setGameOver()
                }
            }
        }
    }
}

fun View.distanceTo(other: View) = MPoint.distance(xD, yD, other.xD, other.yD)

fun View.advance(amount: Double, rot: Angle = (-90).degrees) = this.apply {
    xD += (this.rotation + rot).cosineD * amount
    yD += (this.rotation + rot).sineD * amount
}

// A dummy throwable to cancel updatables
object GameRestart : Throwable()

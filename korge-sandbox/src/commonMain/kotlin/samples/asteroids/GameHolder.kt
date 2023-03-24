package samples.asteroids

import korlibs.time.milliseconds
import korlibs.korge.tween.get
import korlibs.korge.tween.tween
import korlibs.korge.view.SContainer
import korlibs.korge.view.solidRect
import korlibs.image.color.Colors
import korlibs.io.async.launchImmediately

class GameHolder(private val scene: MainAsteroids) {
    val sceneView: SContainer get() = scene.sceneView
    private var game = Game(scene)

    // A flag to prevent multiple restarts from happening at the same time
    private var restarting = false

    fun restart() {
        // TODO check if this code is thread-safe
        if (restarting) return
        restarting = true

        scene.launchImmediately {
            game.detach()

            // Before restarting, display and wait for a fadeout overlay
            val fadeoutOverlay = sceneView.solidRect(WIDTH, HEIGHT, color = Colors.BLACK.withA(0))
            fadeoutOverlay.tween(fadeoutOverlay::color[Colors.BLACK], time = 500.milliseconds)

            sceneView.removeChildren()

            // Recreate the game
            game = Game(scene)

            restarting = false
        }
    }
}

import com.soywiz.klock.milliseconds
import com.soywiz.korge.tween.get
import com.soywiz.korge.tween.tween
import com.soywiz.korge.view.Stage
import com.soywiz.korge.view.solidRect
import com.soywiz.korim.color.Colors
import com.soywiz.korio.async.launchImmediately

class GameHolder(private val stage: Stage) {

    private var game = Game(stage)

    // A flag to prevent multiple restarts from happening at the same time
    private var restarting = false

    fun restart() {
        // TODO check if this code is thread-safe
        if (restarting) return
        restarting = true

        stage.launchImmediately {
            game.detach()

            // Before restarting, display and wait for a fadeout overlay
            val fadeoutOverlay = stage.solidRect(WIDTH, HEIGHT, color = Colors.BLACK.withA(0))
            fadeoutOverlay.tween(fadeoutOverlay::color[Colors.BLACK], time = 500.milliseconds)

            stage.removeChildren()

            // Recreate the game
            game = Game(stage)

            restarting = false
        }
    }
}

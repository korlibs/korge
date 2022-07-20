package samples.asteroids

import com.soywiz.korev.Event
import com.soywiz.korev.addEventListener
import com.soywiz.korge.scene.ScaledScene
import com.soywiz.korge.view.SContainer

const val WIDTH = 640
const val HEIGHT = 480
const val SHIP_SIZE = 24

class MainAsteroids : ScaledScene(WIDTH, HEIGHT) {
    lateinit var gameHolder: GameHolder

    override suspend fun SContainer.sceneMain() {
        gameHolder = GameHolder(this@MainAsteroids)
        views.gameWindow.icon = assets.shipBitmap

        addOnEvent<GameRestartEvent> {
            restart()
        }
    }

    fun restart() {
        gameHolder.restart()
    }
}

class GameRestartEvent : Event()

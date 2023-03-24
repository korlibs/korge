package samples.asteroids

import korlibs.event.*
import korlibs.korge.scene.ScaledScene
import korlibs.korge.view.SContainer

const val WIDTH = 640
const val HEIGHT = 480
const val SHIP_SIZE = 24

class MainAsteroids : ScaledScene(WIDTH, HEIGHT) {
    lateinit var gameHolder: GameHolder

    override suspend fun SContainer.sceneMain() {
        gameHolder = GameHolder(this@MainAsteroids)
        views.gameWindow.icon = assets.shipBitmap

        onEvent(GameRestartEvent) {
            restart()
        }
    }

    fun restart() {
        gameHolder.restart()
    }
}

class GameRestartEvent : Event(), TEvent<GameRestartEvent> {
    override val type: EventType<GameRestartEvent> = GameRestartEvent
    companion object : EventType<GameRestartEvent>
}
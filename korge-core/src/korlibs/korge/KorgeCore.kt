package korlibs.korge

import korlibs.io.async.*
import korlibs.render.*
import kotlinx.coroutines.*

/**
 * ```kotlin
 * suspend fun main() = KorgeCore {
 *     val stopWatch = Stopwatch().start()
 *     onRenderEvent {
 *         ag.clear(ag.mainFrameBuffer, color = Colors.RED.interpolateWith((stopWatch.elapsed.seconds % 1.0).toRatio(), Colors.WHITE))
 *     }
 * }
 * ```
 */
suspend fun KorgeCore(
    config: GameWindowCreationConfig = GameWindowCreationConfig.DEFAULT,
    gameWindow: GameWindow = CreateDefaultGameWindow(config),
    block: suspend GameWindow.() -> Unit
) {
    //withContext(PreferSyncIo(preferSyncIo = true)) {
    withContext(PreferSyncIo(preferSyncIo = null)) {
        gameWindow.loop {
            block()
        }
    }
}

/*
suspefun test() {
    KorgeCore {
        val gameWindow = this
        onRenderEvent {
            gameWindow.ag.clear(gameWindow.ag.mainFrameBuffer, color = Colors.RED)
        }
    }
}
*/

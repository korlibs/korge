package korlibs.render

import korlibs.io.async.*
import korlibs.js.*
import korlibs.math.geom.*
import korlibs.platform.*
import korlibs.sdl.*
import kotlinx.coroutines.*
import org.w3c.dom.*
import kotlin.js.*

class DenoJsGameWindow(
    size: Size = Size(640, 480),
    config: GameWindowCreationConfig = GameWindowCreationConfig.DEFAULT,
) : SDLGameWindow(size, config) {
    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        launchImmediately(getCoroutineDispatcherWithCurrentContext()) {
            entry()
        }

        jsGlobalThis.setInterval({
            //println("INTERVAL")
            updateSDLEvents()
            frame()
            afterFrame()
        }, 16)

        //CompletableDeferred<Unit>().await()
    }

    override fun close(exitCode: Int) {
        super.close(exitCode)
        Deno.exit(exitCode)
    }
}

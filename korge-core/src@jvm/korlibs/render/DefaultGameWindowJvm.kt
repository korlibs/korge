package korlibs.render

import korlibs.graphics.*
import korlibs.render.awt.*

actual fun CreateDefaultGameWindow(config: GameWindowCreationConfig): GameWindow = when {
    System.getenv("KORGE_HEADLESS") == "true" -> AwtOffscreenGameWindow(config)
    else -> AwtGameWindow(config)
}

object JvmAGFactory : AGFactory {
    override val supportsNativeFrame: Boolean = true

    override fun create(nativeControl: Any?, config: AGConfig): AG {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createFastWindow(title: String, width: Int, height: Int): AGWindow {
        return CreateDefaultGameWindow().apply {
            this.title = title
            this.setSize(width, height)
        }
    }
}

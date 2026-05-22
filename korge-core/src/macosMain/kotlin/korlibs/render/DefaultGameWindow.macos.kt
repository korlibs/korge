package korlibs.render

import korlibs.graphics.AG
import korlibs.graphics.AGConfig
import korlibs.graphics.gl.AGOpenglFactory

private class MacosGameWindow(
    private val config: GameWindowCreationConfig,
) : GameWindow() {
    override val ag: AG by lazy {
        AGOpenglFactory.create(null).create(null, AGConfig())
    }

    override var title: String = config.title

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        entry(this)
    }

    override fun close(exitCode: Int) {
        super.close(exitCode)
    }
}

actual fun CreateDefaultGameWindow(config: GameWindowCreationConfig): GameWindow {
    return MacosGameWindow(config)
}

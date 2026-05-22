package korlibs.render

import korlibs.graphics.AG

private class WatchosGameWindow(
    private val config: GameWindowCreationConfig,
) : GameWindow() {
    override val ag: AG
        get() = error("Rendering is not supported on watchOS yet")

    override var title: String = config.title

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        entry(this)
    }

    override fun close(exitCode: Int) {
        super.close(exitCode)
    }
}

actual fun CreateDefaultGameWindow(config: GameWindowCreationConfig): GameWindow {
    return WatchosGameWindow(config)
}

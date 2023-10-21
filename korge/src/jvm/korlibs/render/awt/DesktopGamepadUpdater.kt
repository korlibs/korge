package korlibs.render.awt

import korlibs.event.gamepad.*
import korlibs.platform.*
import korlibs.render.*

internal object DesktopGamepadUpdater {
    fun updateGamepads(window: GameWindow) {
        when {
            Platform.isWindows -> xinputEventAdapter.updateGamepads(window.gamepadEmitter)
            Platform.isLinux -> linuxJoyEventAdapter.updateGamepads(window.gamepadEmitter)
            Platform.isMac -> macosGamepadEventAdapter.updateGamepads(window)
            else -> Unit //println("undetected OS: ${OS.rawName}")
        }
    }

    private val xinputEventAdapter by lazy { XInputGamepadEventAdapter() }
    private val linuxJoyEventAdapter by lazy { LinuxJoyEventAdapter() }
    private val macosGamepadEventAdapter by lazy { MacosGamepadEventAdapter() }
}

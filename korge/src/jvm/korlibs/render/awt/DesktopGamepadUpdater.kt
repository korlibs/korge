package korlibs.render.awt

import korlibs.event.gamepad.*
import korlibs.platform.*
import korlibs.render.*
import korlibs.time.*

internal object DesktopGamepadUpdater {
    private var exceptionStopwatch: Stopwatch? = null

    fun updateGamepads(window: GameWindow) {
        if (exceptionStopwatch != null && exceptionStopwatch!!.elapsed < 1.seconds) {
            return
        }
        if (exceptionStopwatch == null) {
            exceptionStopwatch = Stopwatch().start()
        }
        //println("exceptionStopwatch.elapsed=${exceptionStopwatch?.elapsed}")
        try {
            when {
                Platform.isWindows -> xinputEventAdapter.updateGamepads(window.gamepadEmitter)
                Platform.isLinux -> linuxJoyEventAdapter.updateGamepads(window.gamepadEmitter)
                Platform.isMac -> macosGamepadEventAdapter.updateGamepads(window)
                else -> Unit //println("undetected OS: ${OS.rawName}")
            }
        } catch (e: Throwable) {
            exceptionStopwatch?.restart()
            //println("exceptionStopwatch.restart()")
            e.printStackTrace()
        }
    }

    private val xinputEventAdapter by lazy { XInputGamepadEventAdapter() }
    private val linuxJoyEventAdapter by lazy { LinuxJoyEventAdapter() }
    private val macosGamepadEventAdapter by lazy { MacosGamepadEventAdapter() }
}

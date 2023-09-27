package korlibs.render.awt

import korlibs.datastructure.WeakMap
import korlibs.datastructure.getOrPut
import korlibs.datastructure.iterators.*
import korlibs.logger.*
import java.awt.*

private val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
fun Window.getScreenDevice(): GraphicsDevice {
    val location = location
    ge.screenDevices.fastForEach { device ->
        if (device.defaultConfiguration.bounds.contains(location)) {
            return device
        }
        //println("$device: " + device.defaultConfiguration.bounds)
    }
    return ge.defaultScreenDevice
}

private val cachedRefreshRates = WeakMap<GraphicsDevice, Int>()

private val graphicsDeviceLogger = Logger("GraphicsDeviceLogger")

val GraphicsDevice.cachedRefreshRate: Int get() {
    return cachedRefreshRates.getOrPut(this) {
        graphicsDeviceLogger.info { "COMPUTED REFRESH RATE for $it (${it.displayMode.refreshRate})" }
        it.displayMode.refreshRate
    }
}

fun Component.getContainerFrame(): Frame? = getAncestor { it is Frame } as? Frame?

fun Component.getAncestor(cond: (Component) -> Boolean): Component? {
    var current: Component? = this
    while (current != null) {
        if (cond(current)) return current
        current = current.parent
    }
    return null
}

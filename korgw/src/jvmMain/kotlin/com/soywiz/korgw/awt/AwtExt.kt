package com.soywiz.korgw.awt

import com.soywiz.kds.iterators.*
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

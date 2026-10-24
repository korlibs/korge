package korlibs.korge.gradle.targets.ios

import java.io.File

/**
 * Finds the app that shows a simulator's screen for the selected Xcode.
 *
 * Xcode 26 and earlier ship `Contents/Developer/Applications/Simulator.app`, which takes the device as
 * `-CurrentDeviceUDID`. Xcode 27 replaced it with `Contents/Applications/DeviceHub.app`, which ignores those
 * arguments and opens a device through its `devices://device/open?id=<udid>` URL instead.
 */
object IosSimulatorApp {
    /**
     * Returns the command that opens the window for [udid], or null when the Xcode at [developerDir]
     * (the output of `xcode-select -p`) ships neither app.
     */
    fun openCommand(developerDir: File, udid: String): List<String>? {
        val simulatorApp = File(developerDir, "Applications/Simulator.app")
        if (simulatorApp.isDirectory) {
            return listOf("open", simulatorApp.absolutePath, "--args", "-CurrentDeviceUDID", udid)
        }
        val deviceHubApp = File(developerDir, "../Applications/DeviceHub.app").normalize()
        if (deviceHubApp.isDirectory) {
            return listOf("open", "-a", deviceHubApp.absolutePath, "devices://device/open?id=$udid")
        }
        return null
    }
}

package korlibs.korge.gradle.targets.ios

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IosSimulatorAppTest {
    private val udid = "516EAFD1-B0E2-48FF-80E6-71CB68F5813A"

    // Fake Xcode.app/Contents with the given apps and returns its Developer folder (what `xcode-select -p` prints)
    private fun fakeXcode(vararg appPaths: String): File {
        val contents = Files.createTempDirectory("Xcode.app").toFile().resolve("Contents")
        val developer = contents.resolve("Developer").also { it.mkdirs() }
        for (path in appPaths) contents.resolve(path).mkdirs()
        return developer
    }

    @Test
    fun simulatorAppIsOpenedWithCurrentDeviceUdid() {
        val developer = fakeXcode("Developer/Applications/Simulator.app")
        assertEquals(
            listOf("open", developer.resolve("Applications/Simulator.app").absolutePath, "--args", "-CurrentDeviceUDID", udid),
            IosSimulatorApp.openCommand(developer, udid),
        )
    }

    @Test
    fun deviceHubIsOpenedWithDeviceUrl() {
        val developer = fakeXcode("Applications/DeviceHub.app")
        assertEquals(
            listOf("open", "-a", developer.parentFile.resolve("Applications/DeviceHub.app").absolutePath, "devices://device/open?id=$udid"),
            IosSimulatorApp.openCommand(developer, udid),
        )
    }

    @Test
    fun simulatorAppWinsWhenBothExist() {
        val developer = fakeXcode("Developer/Applications/Simulator.app", "Applications/DeviceHub.app")
        assertEquals("--args", IosSimulatorApp.openCommand(developer, udid)?.get(2))
    }

    @Test
    fun neitherAppMeansNoCommand() {
        assertNull(IosSimulatorApp.openCommand(fakeXcode(), udid))
    }
}

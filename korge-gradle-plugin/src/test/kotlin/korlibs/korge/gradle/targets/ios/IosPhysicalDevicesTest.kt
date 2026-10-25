package korlibs.korge.gradle.targets.ios

import korlibs.korge.gradle.targets.ios.IosPhysicalDevices.Device
import korlibs.korge.gradle.typedresources.getResourceText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class IosPhysicalDevicesTest {
    // Captured with Xcode 27.1: one iPhone connected by cable, plus simulators (listed as paired, transport "sameMachine")
    private val devices = IosPhysicalDevices.parse(getResourceText("ios/devicectl-devices-xcode27.1.json"))

    private fun phone(name: String, transport: String, pairing: String = "paired") =
        Device("udid-$name", name, "iOS", "physical", pairing, transport)

    @Test
    fun parsesFixture() {
        assertEquals(listOf("physical", "simulated", "simulated", "simulated"), devices.map { it.reality })
        assertEquals("wired", devices.first().transportType)
    }

    @Test
    fun connectedIphoneIsChosenOverSimulators() {
        val device = IosPhysicalDevices.select(devices)
        assertEquals("Test iPhone", device.name)
        assertEquals("00008150-000000000000001C", device.udid)
    }

    @Test
    fun cableIsPreferredOverNetwork() {
        val device = IosPhysicalDevices.select(listOf(phone("Wireless", "localNetwork"), phone("Cable", "wired")))
        assertEquals("Cable", device.name)
    }

    @Test
    fun unreachableOrUnpairedPhonesAreSkipped() {
        val device = IosPhysicalDevices.select(listOf(phone("Unplugged", ""), phone("Unpaired", "wired", pairing = "unpaired"), phone("Wireless", "localNetwork")))
        assertEquals("Wireless", device.name)
    }

    @Test
    fun onlySimulatorsFailsWithClearMessage() {
        val e = assertFailsWith<IllegalStateException> { IosPhysicalDevices.select(devices.filter { it.reality == "simulated" }) }
        assertTrue(e.message!!.contains("No connected iPhone found"), e.message)
    }

    @Test
    fun unreachablePhoneIsNamedInError() {
        val e = assertFailsWith<IllegalStateException> { IosPhysicalDevices.select(listOf(phone("Unplugged", ""))) }
        assertTrue(e.message!!.contains("Unplugged (iOS, pairing=paired, transport=none)"), e.message)
    }
}

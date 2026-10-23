package korlibs.korge.gradle.targets.ios

import korlibs.korge.gradle.targets.ios.IosSimulators.DeviceType
import korlibs.korge.gradle.targets.ios.IosSimulators.Runtime
import korlibs.korge.gradle.typedresources.getResourceText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosSimulatorsTest {
    // Captured with Xcode 27.1: iOS 27.0 (many iPhones) and iOS 27.1 (only "iPhone Duo") runtimes
    private val runtimes = IosSimulators.parseRuntimes(getResourceText("ios/simctl-runtimes-xcode27.1.json"))
    private val devices = IosSimulators.parseDevices(getResourceText("ios/simctl-devices-xcode27.1.json"))

    private fun iphone(id: String, name: String = id.replace('-', ' ')) =
        DeviceType("com.apple.CoreSimulator.SimDeviceType.$id", name, "iPhone")

    private fun runtime(version: String, vararg types: DeviceType, available: Boolean = true) =
        Runtime("com.apple.CoreSimulator.SimRuntime.iOS-${version.replace('.', '-')}", version, available, "iOS", types.toList())

    @Test
    fun parsesFixtures() {
        assertEquals(listOf("27.0", "27.1"), runtimes.map { it.version })
        assertTrue(runtimes.all { it.isAvailable && it.isIos })
        assertEquals(listOf("iPhone Duo"), runtimes.single { it.version == "27.1" }.supportedDeviceTypes.filter { it.productFamily == "iPhone" }.map { it.name })
        assertTrue(devices.any { it.name == "iPhone 17" && it.runtime.endsWith("iOS-27-0") })
    }

    @Test
    fun existingPreferredIphoneIsUsed() {
        val device = IosSimulators.selectIphone(devices.filter { it.isAvailable }, 17)
        assertEquals("iPhone 17", device?.name)
    }

    @Test
    fun existingIphoneOnNewestRuntimeIsUsedWhenPreferredIsMissing() {
        // No "iPhone 8" anywhere: any iPhone is fine, newest runtime first
        val device = IosSimulators.selectIphone(devices.filter { it.isAvailable }, 8)
        assertEquals("iPhone Duo", device?.name)
        assertEquals("com.apple.CoreSimulator.SimRuntime.iOS-27-1", device?.runtime)
    }

    @Test
    fun noAvailableIphoneMeansCreationIsNeeded() {
        val onlyIpadsAndUnavailable = devices.filter { !it.name.contains("iPhone") } +
            devices.first { it.name == "iPhone 17" }.copy(isAvailable = false)
        assertNull(IosSimulators.selectIphone(onlyIpadsAndUnavailable.filter { it.isAvailable }, 17))
    }

    @Test
    fun preferredIsCreatedOnNewestRuntimeThatSupportsIt() {
        // iOS 27.1 is the newest runtime but only supports "iPhone Duo"; iOS 27.0 supports iPhone 17
        val plan = IosSimulators.planIphoneCreation(runtimes, 17)
        assertEquals("27.0", plan.runtime.version)
        assertEquals("com.apple.CoreSimulator.SimDeviceType.iPhone-17", plan.deviceType.identifier)
        assertEquals("iPhone 17", plan.deviceType.name)
    }

    @Test
    fun preferredUnsupportedEverywhereFallsBackToNewestRuntime() {
        // No runtime supports iPhone 8: use the newest runtime (27.1) and its newest iPhone type
        val plan = IosSimulators.planIphoneCreation(runtimes, 8)
        assertEquals("27.1", plan.runtime.version)
        assertEquals("com.apple.CoreSimulator.SimDeviceType.iPhone-Duo", plan.deviceType.identifier)
        assertEquals("iPhone Duo", plan.deviceType.name)
    }

    @Test
    fun preferredSupportedIsChosen() {
        val plan = IosSimulators.planIphoneCreation(runtimes.filter { it.version == "27.0" }, 17)
        assertEquals("com.apple.CoreSimulator.SimDeviceType.iPhone-17", plan.deviceType.identifier)
        assertEquals("iPhone 17", plan.deviceType.name)
    }

    @Test
    fun oldPreferredIsReplacedByNewestNumberedBaseModel() {
        // iPhone 8 is not supported on iOS 27; the newest generation there is 18, which only has Pro models
        val plan = IosSimulators.planIphoneCreation(runtimes.filter { it.version == "27.0" }, 8)
        assertEquals("com.apple.CoreSimulator.SimDeviceType.iPhone-18-Pro", plan.deviceType.identifier)
    }

    @Test
    fun newestIphoneTypePrefersHighestNumberAndBaseModel() {
        val types = listOf(iphone("iPhone-11"), iphone("iPhone-Air"), iphone("iPhone-17-Pro"), iphone("iPhone-17e"), iphone("iPhone-17"), iphone("iPhone-SE-3rd-generation"))
        assertEquals("com.apple.CoreSimulator.SimDeviceType.iPhone-17", IosSimulators.newestIphoneType(types)?.identifier)
        assertEquals("com.apple.CoreSimulator.SimDeviceType.iPhone-Air", IosSimulators.newestIphoneType(listOf(iphone("iPhone-Air"), iphone("iPhone-Duo")))?.identifier)
        assertNull(IosSimulators.newestIphoneType(listOf(DeviceType("com.apple.CoreSimulator.SimDeviceType.iPad-Air", "iPad Air", "iPad"))))
    }

    @Test
    fun newestRuntimeIsChosenByNumericVersion() {
        val plan = IosSimulators.planIphoneCreation(
            listOf(runtime("10.0", iphone("iPhone-8")), runtime("9.3", iphone("iPhone-8")), runtime("11.0", iphone("iPhone-8"), available = false)),
            8,
        )
        assertEquals("10.0", plan.runtime.version)
        assertTrue(IosSimulators.compareVersions("27.1", "27.0") > 0)
        assertTrue(IosSimulators.compareVersions("10.0", "9.3") > 0)
        assertTrue(IosSimulators.compareVersions("17.10", "17.2") > 0)
        assertEquals(0, IosSimulators.compareVersions("18", "18.0"))
        assertEquals("27.0", IosSimulators.runtimeVersionFromIdentifier("com.apple.CoreSimulator.SimRuntime.iOS-27-0"))
    }

    @Test
    fun noIosRuntimeFailsWithClearMessage() {
        val tvOs = Runtime("com.apple.CoreSimulator.SimRuntime.tvOS-27-0", "27.0", true, "tvOS", emptyList())
        val e = assertFailsWith<IllegalStateException> { IosSimulators.planIphoneCreation(listOf(tvOs), 17) }
        assertTrue(e.message!!.contains("no available iOS simulator runtime"), e.message)
        assertTrue(e.message!!.contains("tvOS-27-0"), e.message)
        assertFailsWith<IllegalStateException> { IosSimulators.planIphoneCreation(emptyList(), 17) }
    }

    @Test
    fun runtimeWithoutIphoneTypesFailsWithClearMessage() {
        val ipadOnly = runtime("27.0", DeviceType("com.apple.CoreSimulator.SimDeviceType.iPad-Air", "iPad Air", "iPad"))
        val e = assertFailsWith<IllegalStateException> { IosSimulators.planIphoneCreation(listOf(ipadOnly), 17) }
        assertTrue(e.message!!.contains("supports no iPhone device type"), e.message)
        assertTrue(e.message!!.contains("iPad-Air"), e.message)
    }
}

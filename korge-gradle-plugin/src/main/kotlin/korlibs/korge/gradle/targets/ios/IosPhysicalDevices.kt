package korlibs.korge.gradle.targets.ios

import korlibs.korge.gradle.util.Dyn
import korlibs.korge.gradle.util.Json
import korlibs.korge.gradle.util.dyn

/**
 * Pure helpers to pick the physical iPhone to install on, from the JSON printed by
 * `xcrun devicectl list devices --json-output -`, so they can be tested without a device.
 */
object IosPhysicalDevices {
    data class Device(
        val udid: String,
        val name: String,
        val platform: String,
        val reality: String,
        val pairingState: String,
        /** `wired`, a network transport, or empty when the Mac can't reach the device right now. */
        val transportType: String,
    ) {
        val isReachableIos: Boolean
            get() = platform == "iOS" && reality == "physical" && pairingState == "paired" && transportType.isNotEmpty()
    }

    fun parse(json: String): List<Device> = Json.parse(json).dyn["result"]["devices"].list.map {
        Device(
            udid = it["hardwareProperties"]["udid"].strOrEmpty(),
            name = it["deviceProperties"]["name"].strOrEmpty(),
            platform = it["hardwareProperties"]["platform"].strOrEmpty(),
            reality = it["hardwareProperties"]["reality"].strOrEmpty(),
            pairingState = it["connectionProperties"]["pairingState"].strOrEmpty(),
            transportType = it["connectionProperties"]["transportType"].strOrEmpty(),
        )
    }

    private fun Dyn.strOrEmpty(): String = toStringOrNull() ?: ""

    /**
     * Returns the first paired iPhone the Mac can reach, preferring one connected by cable.
     * Simulators, which Xcode 27 also lists, are skipped. Throws with what was found when there is none.
     */
    fun select(devices: List<Device>): Device =
        devices.filter { it.isReachableIos }.minByOrNull { if (it.transportType == "wired") 0 else 1 }
            ?: error(buildString {
                append("No connected iPhone found. Connect it with a cable, unlock it, trust this Mac and enable Developer Mode.")
                val physical = devices.filter { it.reality == "physical" }
                if (physical.isNotEmpty()) {
                    append(" Physical devices reported by `xcrun devicectl list devices`: ")
                    append(physical.joinToString { "${it.name} (${it.platform}, pairing=${it.pairingState}, transport=${it.transportType.ifEmpty { "none" }})" })
                }
            })
}

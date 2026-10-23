package korlibs.korge.gradle.targets.ios

import korlibs.korge.gradle.util.Dyn
import korlibs.korge.gradle.util.Json
import korlibs.korge.gradle.util.dyn

/**
 * Pure helpers to pick an iOS simulator device and, when none exists, the runtime and device type to create one with.
 *
 * They work on the JSON printed by `xcrun simctl list -j devices` and `xcrun simctl list -j runtimes`,
 * so they can be tested without Xcode.
 */
object IosSimulators {
    data class DeviceType(val identifier: String, val name: String, val productFamily: String)

    data class Runtime(
        val identifier: String,
        val version: String,
        val isAvailable: Boolean,
        val platform: String,
        val supportedDeviceTypes: List<DeviceType>,
    ) {
        val isIos: Boolean get() = platform == "iOS" || (platform.isEmpty() && identifier.contains("SimRuntime.iOS-"))
    }

    data class CreatePlan(val runtime: Runtime, val deviceType: DeviceType, val reason: String)

    private const val IPHONE_TYPE_PREFIX = "com.apple.CoreSimulator.SimDeviceType.iPhone-"

    fun preferredIphoneTypeIdentifier(iphoneVersion: Int): String = "$IPHONE_TYPE_PREFIX$iphoneVersion"

    fun parseDevices(json: String, os: String = "iOS"): List<IosSdk.IosDevice> {
        val devices = Json.parse(json).dyn["devices"]
        return devices.keys.map { it.str }.filter { it.contains(os) }.flatMap { runtime ->
            devices[runtime].list.map {
                IosSdk.IosDevice(it["state"].str == "Booted", it["isAvailable"].bool, it["name"].str, it["udid"].str, runtime)
            }
        }
    }

    fun parseRuntimes(json: String): List<Runtime> {
        return Json.parse(json).dyn["runtimes"].list.map { runtime ->
            Runtime(
                identifier = runtime["identifier"].strOrEmpty(),
                version = runtime["version"].strOrEmpty(),
                isAvailable = runtime["isAvailable"].bool,
                platform = runtime["platform"].strOrEmpty(),
                supportedDeviceTypes = runtime["supportedDeviceTypes"].list.map {
                    DeviceType(it["identifier"].strOrEmpty(), it["name"].strOrEmpty(), it["productFamily"].strOrEmpty())
                },
            )
        }
    }

    private fun Dyn.strOrEmpty(): String = toStringOrNull() ?: ""

    /** Compares dotted versions numerically, so `10.0 > 9.3` and `27.1 > 27.0`. Missing parts count as 0. */
    fun compareVersions(a: String, b: String): Int {
        val pa = a.split('.', '-').map { it.toIntOrNull() ?: 0 }
        val pb = b.split('.', '-').map { it.toIntOrNull() ?: 0 }
        for (n in 0 until maxOf(pa.size, pb.size)) {
            val c = pa.getOrElse(n) { 0 }.compareTo(pb.getOrElse(n) { 0 })
            if (c != 0) return c
        }
        return 0
    }

    /** `com.apple.CoreSimulator.SimRuntime.iOS-27-0` -> `27.0` */
    fun runtimeVersionFromIdentifier(identifier: String): String =
        identifier.substringAfterLast('.').substringAfter('-', "").replace('-', '.')

    /**
     * Picks `iPhone <iphoneVersion>` if present, otherwise any iPhone.
     * Among several matches the one on the newest runtime wins.
     * Callers filter [devices] first (for example by `isAvailable` or `booted`).
     */
    fun selectIphone(devices: List<IosSdk.IosDevice>, iphoneVersion: Int): IosSdk.IosDevice? {
        val newestFirst = devices.sortedWith { a, b ->
            compareVersions(runtimeVersionFromIdentifier(b.runtime), runtimeVersionFromIdentifier(a.runtime))
        }
        return newestFirst.firstOrNull { it.name == "iPhone $iphoneVersion" }
            ?: newestFirst.firstOrNull { it.name.contains("iPhone") }
    }

    /**
     * Returns the newest iPhone device type in [types].
     *
     * `simctl` list order is not a reliable age signal across Xcode versions, so this uses the model number from the
     * identifier (`iPhone-17`, `iPhone-17-Pro`, `iPhone-17e`): the highest number wins, and within it the base model
     * (`iPhone-17`) is preferred over variants. Types without a number (`iPhone-Air`, `iPhone-SE-...`) only win when no
     * numbered type exists, in which case the first listed type is used.
     */
    fun newestIphoneType(types: List<DeviceType>): DeviceType? {
        val iphones = types.filter { it.productFamily == "iPhone" }
        fun generation(type: DeviceType): Int? =
            Regex("^${Regex.escape(IPHONE_TYPE_PREFIX)}(\\d+)").find(type.identifier)?.groupValues?.get(1)?.toIntOrNull()
        val maxGeneration = iphones.mapNotNull { generation(it) }.maxOrNull() ?: return iphones.firstOrNull()
        return iphones.firstOrNull { it.identifier == "$IPHONE_TYPE_PREFIX$maxGeneration" }
            ?: iphones.first { generation(it) == maxGeneration }
    }

    /**
     * Chooses the runtime and device type to create a simulator with. The preferred `iPhone-<iphoneVersion>` wins:
     * it is created on the newest available iOS runtime (by numeric version) that supports it. When no runtime
     * supports it, the newest runtime is used with its [newestIphoneType].
     * Throws with a description of what was found when there is no iOS runtime or no iPhone type.
     */
    fun planIphoneCreation(runtimes: List<Runtime>, iphoneVersion: Int): CreatePlan {
        val newestFirst = runtimes.filter { it.isAvailable && it.isIos }.sortedWith { a, b -> compareVersions(b.version, a.version) }
        val newest = newestFirst.firstOrNull()
            ?: error(buildString {
                append("Can't create an iPhone simulator: no available iOS simulator runtime found.")
                append(" Install one from Xcode > Settings > Components.")
                append(" Runtimes reported by `xcrun simctl list -j runtimes`: ")
                append(if (runtimes.isEmpty()) "none" else runtimes.joinToString { "${it.identifier} (available=${it.isAvailable})" })
            })
        val preferredId = preferredIphoneTypeIdentifier(iphoneVersion)
        for (runtime in newestFirst) {
            val preferred = runtime.supportedDeviceTypes.firstOrNull { it.productFamily == "iPhone" && it.identifier == preferredId } ?: continue
            val reason = if (runtime == newest) {
                "preferred $preferredId is supported by the newest iOS runtime ${runtime.version}"
            } else {
                "preferred $preferredId is not supported by the newest iOS runtime ${newest.version}, using the newest runtime that supports it"
            }
            return CreatePlan(runtime, preferred, reason)
        }
        val fallback = newestIphoneType(newest.supportedDeviceTypes)
            ?: error(
                "Can't create an iPhone simulator: iOS runtime ${newest.identifier} supports no iPhone device type. " +
                    "Supported device types: " + newest.supportedDeviceTypes.joinToString { it.identifier }.ifEmpty { "none" }
            )
        return CreatePlan(newest, fallback, "no available iOS runtime supports preferred $preferredId, using the newest iPhone type of the newest runtime ${newest.version}")
    }
}

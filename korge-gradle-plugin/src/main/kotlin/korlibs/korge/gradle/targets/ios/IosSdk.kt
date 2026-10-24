package korlibs.korge.gradle.targets.ios

import java.io.ByteArrayInputStream
import java.io.IOException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Base64
import javax.security.auth.x500.X500Principal
import korlibs.korge.gradle.util.execOutput
import korlibs.korge.gradle.util.projectExtension
import org.gradle.api.Project

val Project.iosSdkExt by projectExtension {
    IosSdk(this)
}

class IosSdk(val project: Project) {
    fun appleGetBootDevice(iphoneVersion: Int): IosDevice {
        val devices = appleGetDevices()
        return IosSimulators.selectIphone(devices.filter { it.isAvailable }, iphoneVersion)
            ?: run {
                val errorMessage = "Can't find suitable available iPhone $iphoneVersion device"
                project.logger.info(errorMessage)
                for (device in devices) project.logger.info("- $device")
                error(errorMessage)
            }
    }

    fun appleGetInstallDevice(iphoneVersion: Int): IosDevice {
        val devices = appleGetDevices()
        return IosSimulators.selectIphone(devices.filter { it.booted }, iphoneVersion)
            ?: error("Can't find suitable booted iPhone $iphoneVersion device")
    }

    fun appleGetBootedDevice(os: String = "iOS"): IosDevice? = appleGetDevices(os).firstOrNull { it.booted }
    fun appleFindSdk(name: String): String = Regex("(${name}.*)").find(project.execOutput("xcrun", "xcodebuild", "-showsdks"))?.groupValues?.get(0) ?: error("Can't find sdk starting with $name")
    fun appleFindIphoneSimSdk(): String = appleFindSdk("iphonesimulator")
    fun appleFindIphoneOsSdk(): String = appleFindSdk("iphoneos")

    /** [runtime] is the simctl runtime identifier the device belongs to, e.g. `com.apple.CoreSimulator.SimRuntime.iOS-27-0`. */
    data class IosDevice(val booted: Boolean, val isAvailable: Boolean, val name: String, val udid: String, val runtime: String = "")

    // https://gist.github.com/luckman212/ec52e9291f27bc39c2eecee07e7a9aa7
    fun appleGetDefaultDeveloperCertificateTeamId(): String? {
        @Throws(IOException::class)
        fun execCmd(vararg cmd: String?): String {
            return Runtime.getRuntime().exec(cmd).inputStream.reader().readText()
        }

        fun certFromFilter(filter: String?): String? {
            return execCmd(*buildList {
                add("security")
                add("find-certificate")
                if (filter != null) {
                    add("-c")
                    add("Apple Development:")
                }
                add("-p")
            }.toTypedArray())
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .lines()
                .joinToString("").trim().takeIf { it.isNotEmpty() }
        }

        val certB64 = certFromFilter("Apple Development:") ?: certFromFilter(null)

        val cert = CertificateFactory.getInstance("X.509").generateCertificate(ByteArrayInputStream(Base64.getDecoder().decode(certB64))) as X509Certificate
        val subjectStr = cert.subjectX500Principal.getName(X500Principal.RFC2253)
        val teamId = Regex("OU=([^,]+)").find(subjectStr)?.groups?.get(1)?.value

        return teamId
    }

    fun appleGetDevices(os: String = "iOS"): List<IosDevice> =
        IosSimulators.parseDevices(project.execOutput("xcrun", "simctl", "list", "-j", "devices"), os)

    fun appleGetRuntimes(): List<IosSimulators.Runtime> =
        IosSimulators.parseRuntimes(project.execOutput("xcrun", "simctl", "list", "-j", "runtimes"))

    fun appleGetPhysicalDevice(): IosPhysicalDevices.Device =
        IosPhysicalDevices.select(IosPhysicalDevices.parse(project.execOutput("xcrun", "devicectl", "list", "devices", "--json-output", "-")))
}

package korlibs.korge.gradle.targets.ios

import korlibs.korge.gradle.util.*
import org.gradle.api.*
import java.io.*
import java.security.cert.*
import java.util.*
import javax.security.auth.x500.*

val Project.iosSdkExt by projectExtension {
    IosSdk(this)
}

class IosSdk(val project: Project) {
    fun appleGetBootDevice(iphoneVersion: Int): IosDevice {
        val devices = appleGetDevices()
        return devices.firstOrNull { it.name == "iPhone $iphoneVersion" && it.isAvailable }
            ?: devices.firstOrNull { it.name.contains("iPhone") && it.isAvailable }
            ?: run {
                val errorMessage = "Can't find suitable available iPhone $iphoneVersion device"
                project.logger.info(errorMessage)
                for (device in devices) project.logger.info("- $device")
                error(errorMessage)
            }
    }

    fun appleGetInstallDevice(iphoneVersion: Int): IosDevice {
        val devices = appleGetDevices()
        return devices.firstOrNull { it.name == "iPhone $iphoneVersion" && it.booted }
            ?: devices.firstOrNull { it.name.contains("iPhone") && it.booted }
            ?: error("Can't find suitable booted iPhone $iphoneVersion device")
    }

    fun appleGetBootedDevice(os: String = "iOS"): IosDevice? = appleGetDevices(os).firstOrNull { it.booted }
    fun appleFindSdk(name: String): String = Regex("(${name}.*)").find(project.execOutput("xcrun", "xcodebuild", "-showsdks"))?.groupValues?.get(0) ?: error("Can't find sdk starting with $name")
    fun appleFindIphoneSimSdk(): String = appleFindSdk("iphonesimulator")
    fun appleFindIphoneOsSdk(): String = appleFindSdk("iphoneos")

    data class IosDevice(val booted: Boolean, val isAvailable: Boolean, val name: String, val udid: String)

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

        //println("CERT=$cert")
        //println("subjectStr=$subjectStr")
        //println("teamId=$teamId")

        return teamId
    }

    fun appleGetDevices(os: String = "iOS"): List<IosDevice> {
        val res = Json.parse(project.execOutput("xcrun", "simctl", "list", "-j", "devices")).dyn
        val devices = res["devices"]
        val oses = devices.keys.map { it.str }
        val iosOses = oses.filter { it.contains(os) }
        return iosOses.map { devices[it].list }.flatten().map {
            //println(it)
            IosDevice(it["state"].str == "Booted", it["isAvailable"].bool, it["name"].str, it["udid"].str).also {
                //println(it)
            }
        }
    }

    //tasks.createThis<Task>("iosLaunchSimulator") {
    //	dependsOn("iosInstallSimulator")
    //	doLast {
    //		val udid = appleGetDevices().firstOrNull { it.name == "iPhone 7" }?.udid ?: error("Can't find iPhone 7 device")
    //		execLogger { commandLine("xcrun", "simctl", "launch", "-w", udid, korge.id) }
    //
    //	}
    //}


    //task iosLaunchSimulator(type: Exec, dependsOn: [iosInstallSimulator]) {
    //	workingDir file("client-mpp-ios.xcodeproj")
    //	executable "sh"
    //	args "-c", "xcrun simctl launch booted io.ktor.samples.mpp.client-mpp-ios"
    //}

    // https://www.objc.io/issues/17-security/inside-code-signing/
    // security find-identity -v -p codesigning
    // codesign -s 'iPhone Developer: Thomas Kollbach (7TPNXN7G6K)' Example.app
    // codesign -f -s 'iPhone Developer: Thomas Kollbach (7TPNXN7G6K)' Example.app

    //osascript -e 'tell application "iOS Simulator" to quit'
    //osascript -e 'tell application "Simulator" to quit'
    //xcrun simctl erase all

    // xcrun lipo

}

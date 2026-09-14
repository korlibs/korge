package korlibs.korge.gradle.targets.ios

import java.io.File
import korlibs.korge.gradle.getProcessResourcesTaskName
import korlibs.korge.gradle.korge
import korlibs.korge.gradle.kotlin
import korlibs.korge.gradle.targets.GROUP_KORGE_INSTALL
import korlibs.korge.gradle.targets.GROUP_KORGE_PACKAGE
import korlibs.korge.gradle.targets.GROUP_KORGE_RUN
import korlibs.korge.gradle.targets.ProjectType
import korlibs.korge.gradle.targets.desktop.prepareKotlinNativeBootstrap
import korlibs.korge.gradle.targets.getIconBytes
import korlibs.korge.gradle.targets.isArm
import korlibs.korge.gradle.targets.native.configureKotlinNativeTarget
import korlibs.korge.gradle.targets.native.getCompileTask
import korlibs.korge.gradle.targets.native.getLinkTask
import korlibs.korge.gradle.util.createThis
import korlibs.korge.gradle.util.createTyped
import korlibs.korge.gradle.util.execLogger
import korlibs.korge.gradle.util.execOutput
import korlibs.korge.gradle.util.get
import korlibs.korge.gradle.util.projectExtension
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink

fun Project.configureNativeIos(projectType: ProjectType) {
    configureNativeIosTarget(projectType, "ios")

    this.project.kotlin.apply {
        iosArm64()
        iosX64()
        iosSimulatorArm64()
    }
}

val Project.xcframework by projectExtension() {
    XCFramework()
}

fun Project.configureNativeIosTarget(projectType: ProjectType, targetName: String) {
    val targetNameCapitalized = targetName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    
    val platformNativeFolderName = "platforms/native-$targetName"
    val platformNativeFolder = File(buildDir, platformNativeFolderName)
    
	val prepareKotlinNativeBootstrapIos = tasks.createThis<Task>("prepareKotlinNativeBootstrap${targetNameCapitalized}") {
        doLast {
            File(platformNativeFolder, "bootstrap.kt").apply {
                parentFile.mkdirs()
                writeText(IosProjectTools.genBootstrapKt(korge.realEntryPoint))
            }
        }
    }

    val iosTargets = when (targetName) {
        "ios" -> listOf(kotlin.iosX64(), kotlin.iosArm64(), kotlin.iosSimulatorArm64())
        else -> TODO()
    }

	kotlin.apply {
        val xcf = XCFramework("$targetName")

        for (target in iosTargets) {
            target.configureKotlinNativeTarget(project)
			target.also { target ->
				target.binaries {
                    framework {
                        baseName = "GameMain"
                        xcf.add(this)
                    }
                }
				target.compilations["main"].also { compilation ->
					compilation.defaultSourceSet.kotlin.srcDir(platformNativeFolder)

                    if (projectType.isExecutable) {
                        afterEvaluate {
                            for (type in listOf(NativeBuildType.DEBUG, NativeBuildType.RELEASE)) {
                                compilation.getCompileTask(NativeOutputKind.FRAMEWORK, type, project).dependsOn(prepareKotlinNativeBootstrapIos)
                                compilation.getLinkTask(NativeOutputKind.FRAMEWORK, type, project).dependsOn("prepareKotlinNative${targetNameCapitalized}Project")
                            }
                        }
                    }
				}
			}
		}
	}

    if (projectType.isExecutable) {
        configureNativeIosTargetRun(targetName)
    }
}

fun Project.configureNativeIosTargetRun(targetName: String) {
    val targetNameCapitalized = targetName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    val iosXcodegenExt = project.iosXcodegenExt
    val iosSdkExt = project.iosSdkExt

    val combinedResourcesFolder = File(buildDir, "combinedResources/resources")
    val processedResourcesFolder = File(buildDir, "processedResources/${targetName}Arm64/main")
    val copyIosResources = tasks.createTyped<Copy>("copy${targetNameCapitalized}Resources") {
        val processResourcesTaskName = getProcessResourcesTaskName("${targetName}Arm64", "main")
        dependsOn(processResourcesTaskName)
        from(processedResourcesFolder)
        into(combinedResourcesFolder)
    }

    val prepareKotlinNativeIosProject = tasks.createThis<Task>("prepareKotlinNative${targetNameCapitalized}Project") {
        dependsOn("prepareKotlinNativeBootstrap${targetNameCapitalized}", prepareKotlinNativeBootstrap, copyIosResources)
        doLast {
            val folder = File(buildDir, "platforms/$targetName")
            IosProjectTools.prepareKotlinNativeIosProject(folder, targetName)
            IosProjectTools.prepareKotlinNativeIosProjectIcons(folder) { korge.getIconBytes(it) }
            IosProjectTools.prepareKotlinNativeIosProjectYml(
                folder,
                id = korge.id,
                name = korge.name,
                team = korge.iosDevelopmentTeam ?: korge.appleDevelopmentTeamId ?: iosSdkExt.appleGetDefaultDeveloperCertificateTeamId(),
                combinedResourcesFolder = combinedResourcesFolder,
                targetName = targetName
            )

            execLogger {
                it.workingDir(folder)
                it.commandLine(iosXcodegenExt.xcodeGenExe)
            }
        }
    }

    tasks.createThis<Task>("${targetName}ShutdownSimulator") {
        doFirst {
            execLogger { it.commandLine("xcrun", "simctl", "shutdown", "booted") }
        }
    }

    val iphoneVersion = korge.preferredIphoneSimulatorVersion

    val iosCreateIphone = tasks.createThis<Task>("${targetName}CreateIphone") {
        onlyIf { iosSdkExt.appleGetDevices().none { it.name == "iPhone $iphoneVersion" } }
        doFirst {
            val result = execOutput("xcrun", "simctl", "list")
            val regex = Regex("com\\.apple\\.CoreSimulator\\.SimRuntime\\.iOS[\\w\\-]+")
            val simRuntime = regex.find(result)?.value ?: error("Can't find SimRuntime. exec: xcrun simctl list")
            logger.info("simRuntime: $simRuntime")
            execLogger { it.commandLine("xcrun", "simctl", "create", "iPhone $iphoneVersion", "com.apple.CoreSimulator.SimDeviceType.iPhone-$iphoneVersion", simRuntime) }
        }
    }

    tasks.createThis<Task>("${targetName}BootSimulator") {
        onlyIf { iosSdkExt.appleGetBootedDevice() == null }
        dependsOn(iosCreateIphone)
        doLast {
            val device = iosSdkExt.appleGetBootDevice(iphoneVersion)
            val udid = device.udid
            logger.info("Booting udid=$udid")
            if (logger.isInfoEnabled) {
                for (device in iosSdkExt.appleGetDevices()) {
                    logger.info(" - $device")
                }
            }
            execLogger { it.commandLine("xcrun", "simctl", "boot", udid) }
            execLogger { it.commandLine("sh", "-c", "open `xcode-select -p`/Applications/Simulator.app/ --args -CurrentDeviceUDID $udid") }
        }
    }

    val installIosDeploy = tasks.findByName("installIosDeploy") ?: tasks.createThis<Task>("installIosDeploy") {
        onlyIf { !iosDeployExt.isInstalled }
        doFirst {
            iosDeployExt.installIfRequired()
        }
    }

    val updateIosDeploy = tasks.findByName("updateIosDeploy") ?: tasks.createThis<Task>("updateIosDeploy") {
        doFirst {
            iosDeployExt.update()
        }
    }

    for (debug in listOf(false, true)) {
        val debugSuffix = if (debug) "Debug" else "Release"
        for (simulator in listOf(false, true)) {
            val simulatorSuffix = if (simulator) "Simulator" else "Device"
            val arch = when {
                simulator -> if (isArm) "SimulatorArm64" else "X64"
                else -> "Arm64"
            }
            val archNoSim = when {
                simulator -> "X64"
                else -> "Arm64"
            }
            val arch2 = when {
                simulator -> if (isArm) "arm64" else "x86_64"
                else -> "arm64"
            }
            val sdkName = if (simulator) "iphonesimulator" else "iphoneos"
            tasks.createThis<Exec>("${targetName}Build$simulatorSuffix$debugSuffix") {
                val linkTaskName = "link${debugSuffix}Framework${targetNameCapitalized}$arch"
                dependsOn(prepareKotlinNativeIosProject, linkTaskName)
                val xcodeProjDir = buildDir["platforms/$targetName/app.xcodeproj"]
                afterEvaluate {
                    val linkTask: KotlinNativeLink = tasks.findByName(linkTaskName) as KotlinNativeLink
                    inputs.dir(linkTask.outputFile)
                    outputs.file(xcodeProjDir["build/Build/Products/$debugSuffix-$sdkName/${korge.name}.app/${korge.name}"])
                }
                workingDir(xcodeProjDir)
                doFirst {
                    commandLine("xcrun", "xcodebuild", "-allowProvisioningUpdates", "-scheme", "app-$arch-$debugSuffix", "-project", ".", "-configuration", debugSuffix, "-derivedDataPath", "build", "-arch", arch2, "-sdk", iosSdkExt.appleFindSdk(sdkName))
                    println("COMMAND: ${commandLine.joinToString(" ")}")
                }
            }
        }


        val installIosSimulator = tasks.createThis<Task>("install${targetNameCapitalized}Simulator$debugSuffix") {
            val buildTaskName = "${targetName}BuildSimulator$debugSuffix"
            group = GROUP_KORGE_INSTALL

            dependsOn(buildTaskName, "${targetName}BootSimulator")
            doLast {
                val appFolder = tasks.getByName(buildTaskName).outputs.files.first().parentFile
                val device = iosSdkExt.appleGetInstallDevice(iphoneVersion)
                execLogger { it.commandLine("xcrun", "simctl", "install", device.udid, appFolder.absolutePath) }
            }
        }

        for (Kind in listOf("Simulator", "Device")) {
            val packageIos = tasks.createThis<Task>("package${targetNameCapitalized}$Kind$debugSuffix") {
                group = GROUP_KORGE_PACKAGE
                dependsOn("${targetName}Build$Kind$debugSuffix")
            }
        }

        val installIosDevice = tasks.createThis<Task>("install${targetNameCapitalized}Device$debugSuffix") {
            group = GROUP_KORGE_INSTALL
            val buildTaskName = "${targetName}BuildDevice$debugSuffix"
            dependsOn(installIosDeploy, buildTaskName)
            doLast {
                val appFolder = tasks.getByName(buildTaskName).outputs.files.first().parentFile
                iosDeployExt.install(appFolder.absolutePath)
            }
        }

        val runIosDevice = tasks.createTyped<Exec>("run${targetNameCapitalized}Device$debugSuffix") {
            group = GROUP_KORGE_RUN
            val buildTaskName = "${targetName}BuildDevice$debugSuffix"
            dependsOn(installIosDeploy, buildTaskName)
            doFirst {
                val appFolder = tasks.getByName(buildTaskName).outputs.files.first().parentFile
                iosDeployExt.installAndRun(appFolder.absolutePath)
            }
        }

        val runIosSimulator = tasks.createTyped<Exec>("run${targetNameCapitalized}Simulator$debugSuffix") {
            group = GROUP_KORGE_RUN
            dependsOn(installIosSimulator)
            doFirst {
                val device = iosSdkExt.appleGetInstallDevice(iphoneVersion)
                // xcrun simctl launch --console 7F49203A-1F16-4DEE-B9A2-7A1BB153DF70 com.sample.demo.app-X64-Debug
                //logger.info(params.joinToString(" "))
                val arch = if (isArm) "SimulatorArm64" else "X64"
                execLogger { it.commandLine("xcrun", "simctl", "launch", "--console", device.udid, "${korge.id}.app-$arch-$debugSuffix") }
            }
        }

        tasks.createTyped<Task>("run${targetNameCapitalized}$debugSuffix") {
            dependsOn(runIosDevice)
        }
    }

    tasks.createThis<Task>("${targetName}EraseAllSimulators") {
        doLast { execLogger { it.commandLine("osascript", "-e", "tell application \"iOS Simulator\" to quit") } }
        doLast { execLogger { it.commandLine("osascript", "-e", "tell application \"Simulator\" to quit") } }
        doLast { execLogger { it.commandLine("xcrun", "simctl", "erase", "all") } }
    }
}

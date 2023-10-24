package korlibs.korge.gradle.targets.ios

import korlibs.korge.gradle.*
import korlibs.korge.gradle.targets.*
import korlibs.korge.gradle.targets.desktop.*
import korlibs.korge.gradle.targets.native.*
import korlibs.korge.gradle.util.*
import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.configurationcache.extensions.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.*
import org.jetbrains.kotlin.gradle.tasks.*
import java.io.*

fun Project.configureNativeIos(projectType: ProjectType) {
    configureNativeIosTvos(projectType, "ios")
    configureNativeIosTvos(projectType, "tvos")

    val exKotlinSourceSetContainer = this.project.exKotlinSourceSetContainer
    this.project.kotlin.apply {
        sourceSets.apply {
            for (target in listOf(iosArm64(), iosX64(), iosSimulatorArm64(), tvosArm64(), tvosX64(), tvosSimulatorArm64())) {
                val native = createPairSourceSet(target.name)
                when {
                    target.isIos -> native.dependsOn(exKotlinSourceSetContainer.ios)
                    target.isTvos -> native.dependsOn(exKotlinSourceSetContainer.tvos)
                }
            }
        }
    }
}

val Project.xcframework by projectExtension() {
    //XCFramework("${targetName}Universal")
    XCFramework()
}

fun Project.configureNativeIosTvos(projectType: ProjectType, targetName: String) {
    val targetNameCapitalized = targetName.capitalized()
    
    val platformNativeFolderName = "platforms/native-$targetName"
    val platformNativeFolder = File(buildDir, platformNativeFolderName)
    
	val prepareKotlinNativeBootstrapIosTvos = tasks.createThis<Task>("prepareKotlinNativeBootstrap${targetNameCapitalized}") {
        doLast {
            File(platformNativeFolder, "bootstrap.kt").apply {
                parentFile.mkdirs()
                writeText(IosProjectTools.genBootstrapKt(korge.realEntryPoint))
            }
        }
    }

    val iosTvosTargets = when (targetName) {
        "ios" -> listOf(kotlin.iosX64(), kotlin.iosArm64(), kotlin.iosSimulatorArm64())
        "tvos" -> listOf(kotlin.tvosX64(), kotlin.tvosArm64(), kotlin.tvosSimulatorArm64())
        else -> TODO()
    }

	kotlin.apply {
        val xcf = XCFramework("$targetName")
        //val xcf = project.xcframework
        //val xcf = XCFramework()

        for (target in iosTvosTargets) {
            target.configureKotlinNativeTarget(project)
            //createCopyToExecutableTarget(target.name)
			//for (target in listOf(iosX64())) {
			target.also { target ->
				//target.attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
				target.binaries {
                    framework {
                        baseName = "GameMain"
                        xcf.add(this)
                        embedBitcodeMode.set(Framework.BitcodeEmbeddingMode.BITCODE)
                    }
                }
				target.compilations["main"].also { compilation ->
					//for (type in listOf(NativeBuildType.DEBUG, NativeBuildType.RELEASE)) {
					//	//getLinkTask(NativeOutputKind.FRAMEWORK, type).embedBitcode = Framework.BitcodeEmbeddingMode.DISABLE
					//}

					//compilation.outputKind(NativeOutputKind.FRAMEWORK)

					compilation.defaultSourceSet.kotlin.srcDir(platformNativeFolder)

                    if (projectType.isExecutable) {
                        afterEvaluate {
                            for (type in listOf(NativeBuildType.DEBUG, NativeBuildType.RELEASE)) {
                                compilation.getCompileTask(NativeOutputKind.FRAMEWORK, type, project).dependsOn(prepareKotlinNativeBootstrapIosTvos)
                                compilation.getLinkTask(NativeOutputKind.FRAMEWORK, type, project).dependsOn("prepareKotlinNative${targetNameCapitalized}Project")
                            }
                        }
                    }
				}
			}
		}
	}

    if (projectType.isExecutable) {
        configureNativeIosTvosRun(targetName)
    }
}

fun Project.configureNativeIosTvosRun(targetName: String) {
    val targetNameCapitalized = targetName.capitalized()

    val iosXcodegenExt = project.iosXcodegenExt
    val iosSdkExt = project.iosSdkExt

    if (tasks.findByName("installXcodeGen") == null) {
        tasks.createThis<Task>("installXcodeGen") {
            onlyIf { !iosXcodegenExt.isInstalled() }
            doLast { iosXcodegenExt.install() }
        }
    }

    val combinedResourcesFolder = File(buildDir, "combinedResources/resources")
    val processedResourcesFolder = File(buildDir, "processedResources/${targetName}Arm64/main")
    val copyIosTvosResources = tasks.createTyped<Copy>("copy${targetNameCapitalized}Resources") {
        val processResourcesTaskName = getProcessResourcesTaskName("${targetName}Arm64", "main")
        dependsOn(processResourcesTaskName)
        from(processedResourcesFolder)
        into(combinedResourcesFolder)
    }

    val prepareKotlinNativeIosTvosProject = tasks.createThis<Task>("prepareKotlinNative${targetNameCapitalized}Project") {
        dependsOn("installXcodeGen", "prepareKotlinNativeBootstrap${targetNameCapitalized}", prepareKotlinNativeBootstrap, copyIosTvosResources)
        doLast {
            // project.yml requires these folders to be available, or it will fail
            //File(rootDir, "src/commonMain/resources").mkdirs()

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

    val installIosTvosDeploy = tasks.findByName("installIosDeploy") ?: tasks.createThis<Task>("installIosDeploy") {
        onlyIf { !iosTvosDeployExt.isInstalled }
        doFirst {
            iosTvosDeployExt.installIfRequired()
        }
    }

    val updateIosTvosDeploy = tasks.findByName("updateIosDeploy") ?: tasks.createThis<Task>("updateIosDeploy") {
        doFirst {
            iosTvosDeployExt.update()
        }
    }

    for (debug in listOf(false, true)) {
        val debugSuffix = if (debug) "Debug" else "Release"
        for (simulator in listOf(false, true)) {
            val simulatorSuffix = if (simulator) "Simulator" else "Device"
            //val arch = if (simulator) "X64" else "Arm64"
            //val arch2 = if (simulator) "x64" else "armv8"
            val arch = when {
                simulator -> if (isArm) "SimulatorArm64" else "X64"
                else -> "Arm64"
            }
            val arch2 = when {
                simulator -> if (isArm) "arm64" else "x86_64"
                else -> "arm64"
            }
            val sdkName = if (simulator) "iphonesimulator" else "iphoneos"
            tasks.createThis<Exec>("${targetName}Build$simulatorSuffix$debugSuffix") {
                //task.dependsOn(prepareKotlinNativeIosProject, "linkMain${debugSuffix}FrameworkIos$arch")
                val linkTaskName = "link${debugSuffix}Framework${targetNameCapitalized}$arch"
                dependsOn(prepareKotlinNativeIosTvosProject, linkTaskName)
                val xcodeProjDir = buildDir["platforms/$targetName/app.xcodeproj"]
                afterEvaluate {
                    val linkTask: KotlinNativeLink = tasks.findByName(linkTaskName) as KotlinNativeLink
                    inputs.dir(linkTask.outputFile)
                    outputs.file(xcodeProjDir["build/Build/Products/$debugSuffix-$sdkName/${korge.name}.app/${korge.name}"])
                }
                //afterEvaluate {
                //}
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

        val installIosTvosDevice = tasks.createThis<Task>("install${targetNameCapitalized}Device$debugSuffix") {
            group = GROUP_KORGE_INSTALL
            val buildTaskName = "${targetName}BuildDevice$debugSuffix"
            dependsOn(installIosTvosDeploy, buildTaskName)
            doLast {
                val appFolder = tasks.getByName(buildTaskName).outputs.files.first().parentFile
                iosTvosDeployExt.install(appFolder.absolutePath)
            }
        }

        val runIosDevice = tasks.createTyped<Exec>("run${targetNameCapitalized}Device$debugSuffix") {
            group = GROUP_KORGE_RUN
            val buildTaskName = "${targetName}BuildDevice$debugSuffix"
            dependsOn(installIosTvosDeploy, buildTaskName)
            doFirst {
                val appFolder = tasks.getByName(buildTaskName).outputs.files.first().parentFile
                iosTvosDeployExt.installAndRun(appFolder.absolutePath)
            }
        }

        val runIosSimulator = tasks.createTyped<Exec>("run${targetNameCapitalized}Simulator$debugSuffix") {
            group = GROUP_KORGE_RUN
            dependsOn(installIosSimulator)
            doFirst {
                val device = iosSdkExt.appleGetInstallDevice(iphoneVersion)
                // xcrun simctl launch --console 7F49203A-1F16-4DEE-B9A2-7A1BB153DF70 com.sample.demo.app-X64-Debug
                //logger.info(params.joinToString(" "))
                execLogger { it.commandLine("xcrun", "simctl", "launch", "--console", device.udid, "${korge.id}.app-X64-$debugSuffix") }
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

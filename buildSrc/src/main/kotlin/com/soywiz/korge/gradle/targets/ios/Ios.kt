package com.soywiz.korge.gradle.targets.ios

import com.soywiz.korge.gradle.*
import com.soywiz.korge.gradle.targets.*
import com.soywiz.korge.gradle.targets.desktop.*
import com.soywiz.korge.gradle.targets.native.*
import com.soywiz.korge.gradle.util.*
import com.soywiz.korge.gradle.util.get
import com.soywiz.korge.gradle.targets.ios.IosProjectTools
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import java.io.*
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*
import javax.security.auth.x500.X500Principal

fun Project.configureNativeIos() {
	val prepareKotlinNativeBootstrapIos = tasks.create("prepareKotlinNativeBootstrapIos") {
        doLast {
            File(buildDir, "platforms/native-ios/bootstrap.kt").apply {
                parentFile.mkdirs()
                writeText(IosProjectTools.genBootstrapKt(korge.realEntryPoint))
            }
        }
	}

    val iosTargets = listOf(kotlin.iosX64(), kotlin.iosArm64(), kotlin.iosSimulatorArm64())

	kotlin.apply {
		for (target in iosTargets) {
            target.configureKotlinNativeTarget(project)
			//for (target in listOf(iosX64())) {
			target.also { target ->
				//target.attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
				target.binaries { framework {  } }
				target.compilations["main"].also { compilation ->
					//for (type in listOf(NativeBuildType.DEBUG, NativeBuildType.RELEASE)) {
					//	//getLinkTask(NativeOutputKind.FRAMEWORK, type).embedBitcode = Framework.BitcodeEmbeddingMode.DISABLE
					//}

					//compilation.outputKind(NativeOutputKind.FRAMEWORK)

					compilation.defaultSourceSet.kotlin.srcDir(File(buildDir, "platforms/native-ios"))

					afterEvaluate {
						target.binaries {
							for (binary in this) {
								if (binary is Framework) {
									binary.baseName = "GameMain"
									binary.embedBitcode = Framework.BitcodeEmbeddingMode.DISABLE
								}
							}
						}
						for (type in listOf(NativeBuildType.DEBUG, NativeBuildType.RELEASE)) {
							compilation.getCompileTask(NativeOutputKind.FRAMEWORK, type, project).dependsOn(prepareKotlinNativeBootstrapIos)
							compilation.getLinkTask(NativeOutputKind.FRAMEWORK, type, project).dependsOn("prepareKotlinNativeIosProject")
						}
					}
				}
			}
		}
	}

    val iosXcodegenExt = project.iosXcodegenExt
    val iosSdkExt = project.iosSdkExt

    tasks.create("installXcodeGen") {
        onlyIf { !iosXcodegenExt.isInstalled() }
        doLast { iosXcodegenExt.install() }
	}

	val combinedResourcesFolder = File(buildDir, "combinedResources/resources")
	val copyIosResources = tasks.createTyped<Copy>("copyIosResources") {
        val targetName = "iosX64" // @TODO: Should be one per target?
        val compilationName = "main"
		dependsOn(getKorgeProcessResourcesTaskName(targetName, compilationName))
		from(getCompilationKorgeProcessedResourcesFolder(targetName, compilationName))
		from(File(project.projectDir, "src/commonMain/resources")) // @TODO: Use proper source sets to determine this?
		into(combinedResourcesFolder)
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
		doFirst {
			//combinedResourcesFolder.mkdirs()
		}
	}

	val prepareKotlinNativeIosProject = tasks.create("prepareKotlinNativeIosProject") {
		dependsOn("installXcodeGen", "prepareKotlinNativeBootstrapIos", prepareKotlinNativeBootstrap, copyIosResources)
		doLast {
			// project.yml requires these folders to be available or it will fail
			//File(rootDir, "src/commonMain/resources").mkdirs()

			val folder = File(buildDir, "platforms/ios")
            IosProjectTools.prepareKotlinNativeIosProject(folder)
            IosProjectTools.prepareKotlinNativeIosProjectIcons(folder) { korge.getIconBytes(it) }
            IosProjectTools.prepareKotlinNativeIosProjectYml(
                folder,
                id = korge.id,
                name = korge.name,
                team = korge.iosDevelopmentTeam ?: korge.appleDevelopmentTeamId ?: iosSdkExt.appleGetDefaultDeveloperCertificateTeamId(),
                combinedResourcesFolder = combinedResourcesFolder
            )

			execLogger {
				it.workingDir(folder)
				it.commandLine(iosXcodegenExt.xcodeGenExe)
			}
		}
	}

	tasks.create("iosShutdownSimulator", Task::class.java) {
		doFirst {
			execLogger { it.commandLine("xcrun", "simctl", "shutdown", "booted") }
		}
	}

    val iphoneVersion = korge.preferredIphoneSimulatorVersion

	val iosCreateIphone = tasks.create("iosCreateIphone", Task::class.java) {
		onlyIf { iosSdkExt.appleGetDevices().none { it.name == "iPhone $iphoneVersion" } }
		doFirst {
            val result = execOutput("xcrun", "simctl", "list")
            val regex = Regex("com\\.apple\\.CoreSimulator\\.SimRuntime\\.iOS[\\w\\-]+")
            val simRuntime = regex.find(result)?.value ?: error("Can't find SimRuntime. exec: xcrun simctl list")
            logger.info("simRuntime: $simRuntime")
			execLogger { it.commandLine("xcrun", "simctl", "create", "iPhone $iphoneVersion", "com.apple.CoreSimulator.SimDeviceType.iPhone-$iphoneVersion", simRuntime) }
		}
	}

	tasks.create("iosBootSimulator", Task::class.java) {
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

	for (debug in listOf(false, true)) {
		val debugSuffix = if (debug) "Debug" else "Release"
		for (simulator in listOf(false, true)) {
			val simulatorSuffix = if (simulator) "Simulator" else "Device"
			//val arch = if (simulator) "X64" else "Arm64"
			//val arch2 = if (simulator) "x64" else "armv8"
			val arch = if (simulator) "X64" else "Arm64"
			val arch2 = if (simulator) "x86_64" else "arm64"
			val sdkName = if (simulator) "iphonesimulator" else "iphoneos"
			tasks.create("iosBuild$simulatorSuffix$debugSuffix", Exec::class.java) {
				//task.dependsOn(prepareKotlinNativeIosProject, "linkMain${debugSuffix}FrameworkIos$arch")
                val linkTaskName = "link${debugSuffix}FrameworkIos$arch"
				dependsOn(prepareKotlinNativeIosProject, linkTaskName)
				val xcodeProjDir = buildDir["platforms/ios/app.xcodeproj"]
                afterEvaluate {
                    val linkTask: KotlinNativeLink = tasks.findByName(linkTaskName) as KotlinNativeLink
                    inputs.dir(linkTask.outputFile)
                    outputs.file(xcodeProjDir["build/Build/Products/$debugSuffix-$sdkName/${korge.name}.app/${korge.name}"])
                }
				//afterEvaluate {
				//}
                workingDir(xcodeProjDir)
                doFirst {
                    commandLine("xcrun", "xcodebuild", "-scheme", "app-$arch-$debugSuffix", "-project", ".", "-configuration", debugSuffix, "-derivedDataPath", "build", "-arch", arch2, "-sdk", iosSdkExt.appleFindSdk(sdkName))
                    println("COMMAND: ${commandLine.joinToString(" ")}")
                }
			}
		}

		val installIosSimulator = tasks.create("installIosSimulator$debugSuffix", Task::class.java) {
			val buildTaskName = "iosBuildSimulator$debugSuffix"
			group = GROUP_KORGE_INSTALL

			dependsOn(buildTaskName, "iosBootSimulator")
			doLast {
				val appFolder = tasks.getByName(buildTaskName).outputs.files.first().parentFile
                val device = iosSdkExt.appleGetInstallDevice(iphoneVersion)
				execLogger { it.commandLine("xcrun", "simctl", "install", device.udid, appFolder.absolutePath) }
			}
		}

		val installIosDevice = tasks.create("installIosDevice$debugSuffix", Task::class.java) {
			group = GROUP_KORGE_INSTALL
			val buildTaskName = "iosBuildDevice$debugSuffix"
			dependsOn("installIosDeploy", buildTaskName)
			doLast {
				val appFolder = tasks.getByName(buildTaskName).outputs.files.first().parentFile
                iosDeployExt.command("--bundle", appFolder.absolutePath)
			}
		}

		tasks.createTyped<Exec>("runIosDevice$debugSuffix") {
			group = GROUP_KORGE_RUN
			val buildTaskName = "iosBuildDevice$debugSuffix"
			dependsOn("installIosDeploy", buildTaskName)
			doFirst {
				val appFolder = tasks.getByName(buildTaskName).outputs.files.first().parentFile
                iosDeployExt.command("--noninteractive", "-d", "--bundle", appFolder.absolutePath)
			}
		}

        tasks.createTyped<Exec>("runIosSimulator$debugSuffix") {
            group = GROUP_KORGE_RUN
            dependsOn(installIosSimulator)
            doFirst {
                val device = iosSdkExt.appleGetInstallDevice(iphoneVersion)
                // xcrun simctl launch --console 7F49203A-1F16-4DEE-B9A2-7A1BB153DF70 com.sample.demo.app-X64-Debug
                //logger.info(params.joinToString(" "))
                execLogger { it.commandLine("xcrun", "simctl", "launch", "--console", device.udid, "${korge.id}.app-X64-$debugSuffix") }
            }
        }
    }

	tasks.create("iosEraseAllSimulators") {
		doLast { execLogger { it.commandLine("osascript", "-e", "tell application \"iOS Simulator\" to quit") } }
		doLast { execLogger { it.commandLine("osascript", "-e", "tell application \"Simulator\" to quit") } }
		doLast { execLogger { it.commandLine("xcrun", "simctl", "erase", "all") } }
	}

	tasks.create("installIosDeploy", Task::class.java) {
		onlyIf { !iosDeployExt.isInstalled }
        doFirst {
            iosDeployExt.installIfRequired()
        }
	}

    tasks.create("updateIosDeploy", Task::class.java) {
        doFirst {
            iosDeployExt.update()
        }
    }
}


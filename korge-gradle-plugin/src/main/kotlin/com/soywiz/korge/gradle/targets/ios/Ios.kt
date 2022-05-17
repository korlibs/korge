package com.soywiz.korge.gradle.targets.ios

import com.soywiz.korge.gradle.*
import com.soywiz.korge.gradle.targets.*
import com.soywiz.korge.gradle.targets.desktop.*
import com.soywiz.korge.gradle.targets.native.*
import com.soywiz.korge.gradle.util.*
import com.soywiz.korge.gradle.util.get
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
                writeText("""
                    import ${korge.realEntryPoint}
                    
                    @ThreadLocal
                    object NewAppDelegate : com.soywiz.korgw.KorgwBaseNewAppDelegate() {
                        override fun applicationDidFinishLaunching(app: platform.UIKit.UIApplication) { applicationDidFinishLaunching(app) { ${korge.realEntryPoint}() } }
                    }
                """.trimIndent())
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

    val korlibsFolder = File(System.getProperty("user.home") + "/.korlibs").apply { mkdirs() }
    val xcodeGenFolder = korlibsFolder["XcodeGen"]
    val xcodeGenLocalExecutable = File("/usr/local/bin/xcodegen")
    val xcodeGenExecutable = FileList(
        xcodeGenFolder[".build/release/xcodegen"],
        xcodeGenFolder[".build/apple/Products/Release/xcodegen"],
    )
    val xcodeGenGitTag = "2.25.0"

    tasks.create("installXcodeGen") {
        onlyIf { !xcodeGenLocalExecutable.exists() && !xcodeGenExecutable.exists() }
        doLast {
            if (!xcodeGenFolder[".git"].isDirectory) {
                execLogger {
                    //it.commandLine("git", "clone", "--depth", "1", "--branch", xcodeGenGitTag, "https://github.com/yonaskolb/XcodeGen.git")
                    it.commandLine("git", "clone", "https://github.com/yonaskolb/XcodeGen.git")
                    it.workingDir(korlibsFolder)
                }
            }
            execLogger {
                it.commandLine("git", "checkout", xcodeGenGitTag)
                it.workingDir(xcodeGenFolder)
            }
            execLogger {
                it.commandLine("make", "build")
                it.workingDir(xcodeGenFolder)
            }
        }
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
            folder["app/main.m"].ensureParents().writeText("""
                #import <UIKit/UIKit.h>
                #import <GameMain/GameMain.h>

                @interface AppDelegate : UIResponder <UIApplicationDelegate>
                @property (strong, nonatomic) UIWindow *window;
                @end

                int main(int argc, char * argv[]) {
                    @autoreleasepool { return UIApplicationMain(argc, argv, nil, NSStringFromClass([AppDelegate class])); }
                }

                @implementation AppDelegate
                - (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
                    [[GameMainNewAppDelegate getNewAppDelegate] applicationDidFinishLaunchingApp: application];
                    return YES;
                }
                - (void)applicationWillResignActive:(UIApplication *)application {
                    [[GameMainNewAppDelegate getNewAppDelegate] applicationWillResignActiveApp: application];
                }
                - (void)applicationDidEnterBackground:(UIApplication *)application {
                    [[GameMainNewAppDelegate getNewAppDelegate] applicationDidEnterBackgroundApp: application];
                }
                - (void)applicationWillEnterForeground:(UIApplication *)application {
                    [[GameMainNewAppDelegate getNewAppDelegate] applicationWillEnterForegroundApp: application];
                }
                - (void)applicationDidBecomeActive:(UIApplication *)application {
                    [[GameMainNewAppDelegate getNewAppDelegate] applicationDidBecomeActiveApp: application];
                }
                - (void)applicationWillTerminate:(UIApplication *)application {
                    [[GameMainNewAppDelegate getNewAppDelegate] applicationWillTerminateApp: application];
                }
                @end
            """.trimIndent())
			folder["app/Info.plist"].ensureParents().writeText(Indenter {
				line("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
				line("<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">")
				line("<plist version=\"1.0\">")
				line("<dict>")
				indent {
					line("<key>CFBundleDevelopmentRegion</key>")
					line("<string>$(DEVELOPMENT_LANGUAGE)</string>")
					line("<key>CFBundleExecutable</key>")
					line("<string>$(EXECUTABLE_NAME)</string>")
					line("<key>CFBundleIdentifier</key>")
					line("<string>$(PRODUCT_BUNDLE_IDENTIFIER)</string>")
					line("<key>CFBundleInfoDictionaryVersion</key>")
					line("<string>6.0</string>")
					line("<key>CFBundleName</key>")
					line("<string>$(PRODUCT_NAME)</string>")
					line("<key>CFBundlePackageType</key>")
					line("<string>APPL</string>")
					line("<key>CFBundleShortVersionString</key>")
					line("<string>1.0</string>")
					line("<key>CFBundleVersion</key>")
					line("<string>1</string>")
					line("<key>LSRequiresIPhoneOS</key>")
					line("<true/>")
					line("<key>UILaunchStoryboardName</key>")
					line("<string>LaunchScreen</string>")
					//line("<key>UIMainStoryboardFile</key>")
					//line("<string>Main</string>")
					line("<key>UIRequiredDeviceCapabilities</key>")
					line("<array>")
					indent {
						line("<string>armv7</string>")
					}
					line("</array>")
					line("<key>UISupportedInterfaceOrientations</key>")
					line("<array>")
					indent {
						line("<string>UIInterfaceOrientationPortrait</string>")
						line("<string>UIInterfaceOrientationLandscapeLeft</string>")
						line("<string>UIInterfaceOrientationLandscapeRight</string>")
					}
					line("</array>")
					line("<key>UISupportedInterfaceOrientations~ipad</key>")
					line("<array>")
					indent {
						line("<string>UIInterfaceOrientationPortrait</string>")
						line("<string>UIInterfaceOrientationPortraitUpsideDown</string>")
						line("<string>UIInterfaceOrientationLandscapeLeft</string>")
						line("<string>UIInterfaceOrientationLandscapeRight</string>")
					}
					line("</array>")
				}
				line("</dict>")
				line("</plist>")
			})

			folder["app/Base.lproj/LaunchScreen.storyboard"].ensureParents().writeText("""
				<?xml version="1.0" encoding="UTF-8" standalone="no"?>
				<document type="com.apple.InterfaceBuilder3.CocoaTouch.Storyboard.XIB" version="3.0" toolsVersion="13122.16" targetRuntime="iOS.CocoaTouch" propertyAccessControl="none" useAutolayout="YES" launchScreen="YES" useTraitCollections="YES" useSafeAreas="YES" colorMatched="YES" initialViewController="01J-lp-oVM">
					<dependencies>
						<plugIn identifier="com.apple.InterfaceBuilder.IBCocoaTouchPlugin" version="13104.12"/>
						<capability name="Safe area layout guides" minToolsVersion="9.0"/>
						<capability name="documents saved in the Xcode 8 format" minToolsVersion="8.0"/>
					</dependencies>
					<scenes>
						<!--View Controller-->
						<scene sceneID="EHf-IW-A2E">
							<objects>
								<viewController id="01J-lp-oVM" sceneMemberID="viewController">
									<view key="view" contentMode="scaleToFill" id="Ze5-6b-2t3">
										<rect key="frame" x="0.0" y="0.0" width="375" height="667"/>
										<autoresizingMask key="autoresizingMask" widthSizable="YES" heightSizable="YES"/>
										<color key="backgroundColor" red="1" green="1" blue="1" alpha="1" colorSpace="custom" customColorSpace="sRGB"/>
										<viewLayoutGuide key="safeArea" id="6Tk-OE-BBY"/>
									</view>
								</viewController>
								<placeholder placeholderIdentifier="IBFirstResponder" id="iYj-Kq-Ea1" userLabel="First Responder" sceneMemberID="firstResponder"/>
							</objects>
							<point key="canvasLocation" x="53" y="375"/>
						</scene>
					</scenes>
				</document>
			""".trimIndent())

			folder["app/Assets.xcassets/Contents.json"].ensureParents().writeText("""
				{
				  "info" : {
					"version" : 1,
					"author" : "xcode"
				  }
				}
			""".trimIndent())

			data class IconConfig(val idiom: String, val size: Number, val scale: Int) {
				val sizeStr = "${size}x$size"
				val scaleStr = "${scale}x"
				val realSize = (size.toDouble() * scale).toInt()
				val fileName = "icon$realSize.png"
			}
			val icons = listOf(
				IconConfig("iphone", 20, 2),
				IconConfig("iphone", 20, 3),
				IconConfig("iphone", 29, 2),
				IconConfig("iphone", 20, 3),
				IconConfig("iphone", 40, 2),
				IconConfig("iphone", 40, 3),
				IconConfig("iphone", 60, 2),
				IconConfig("iphone", 60, 3),
				IconConfig("ipad", 20, 1),
				IconConfig("ipad", 20, 2),
				IconConfig("ipad", 29, 1),
				IconConfig("ipad", 29, 2),
				IconConfig("ipad", 40, 1),
				IconConfig("ipad", 40, 2),
				IconConfig("ipad", 76, 1),
				IconConfig("ipad", 76, 2),
				IconConfig("ipad", 83.5, 2),
				IconConfig("ios-marketing", 1024, 1)
			)

			for (icon in icons.distinctBy { it.realSize }) {
				folder["app/Assets.xcassets/AppIcon.appiconset/${icon.fileName}"].ensureParents().writeBytes(korge.getIconBytes(icon.realSize))
			}

			folder["app/Assets.xcassets/AppIcon.appiconset/Contents.json"].ensureParents().writeText(
				Indenter {
					line("{")
					indent {
						line("\"images\" : [")
						indent {

							for ((index, config) in icons.withIndex()) {
								val isLast = (index == icons.lastIndex)
								val tail = if (isLast) "" else ","
								line("{ \"idiom\" : ${config.idiom.quoted}, \"size\" : ${config.sizeStr.quoted}, \"scale\" : ${config.scaleStr.quoted}, \"filename\" : ${config.fileName.quoted} }$tail")
							}

						}
						line("],")
						line("\"info\" : { \"version\": 1, \"author\": \"xcode\" }")
					}
					line("}")
				}
			)


			folder["project.yml"].ensureParents().writeText(Indenter {
				line("name: app")
				line("options:")
				indent {
					line("bundleIdPrefix: ${korge.id}")
					line("minimumXcodeGenVersion: 2.0.0")
				}
				line("settings:")
				indent {
					line("PRODUCT_NAME: ${korge.name}")
					line("ENABLE_BITCODE: NO")
                    val team = korge.appleDevelopmentTeamId ?: appleGetDefaultDeveloperCertificateTeamId()
					if (team != null) {
						line("DEVELOPMENT_TEAM: $team")
					}
				}
				line("targets:")

				indent {
					for (debug in listOf(false, true)) {
						val debugSuffix = if (debug) "Debug" else "Release"
						for (target in listOf("X64", "Arm64", "Arm32")) {
							line("app-$target-$debugSuffix:")
							indent {
								line("platform: iOS")
								line("type: application")
								line("deploymentTarget: \"10.0\"")
								line("sources:")
								indent {
									line("- app")
									//for (path in listOf("../../../src/commonMain/resources", "../../../build/genMainResources")) {
									for (path in listOf(combinedResourcesFolder.relativeTo(folder))) {
										line("- path: $path")
										indent {
											line("name: assets")
											line("optional: true")
											line("buildPhase:")
											indent {
												line("copyFiles:")
												indent {
													line("destination: resources")
													line("subpath: include/app")
												}
											}
											line("type: folder")
										}
									}
								}
                                if (korge.iosDevelopmentTeam != null) {
                                    line("settings:")
                                    line("  DEVELOPMENT_TEAM: ${korge.iosDevelopmentTeam}")
                                }
                                line("dependencies:")
                                line("  - framework: ../../bin/ios$target/${debugSuffix.toLowerCase()}Framework/GameMain.framework")
							}
						}
					}
				}
			}.replace("\t", "  "))

			execLogger {
				it.workingDir(folder)
				it.commandLine(xcodeGenExecutable.takeIfExists() ?: xcodeGenLocalExecutable.takeIfExists() ?: error("Can't find xcodegen"))
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
		onlyIf { appleGetDevices().none { it.name == "iPhone $iphoneVersion" } }
		doFirst {
            val result = execOutput("xcrun", "simctl", "list")
            val regex = Regex("com\\.apple\\.CoreSimulator\\.SimRuntime\\.iOS[\\w\\-]+")
            val simRuntime = regex.find(result)?.value ?: error("Can't find SimRuntime. exec: xcrun simctl list")
            logger.info("simRuntime: $simRuntime")
			execLogger { it.commandLine("xcrun", "simctl", "create", "iPhone $iphoneVersion", "com.apple.CoreSimulator.SimDeviceType.iPhone-$iphoneVersion", simRuntime) }
		}
	}

	tasks.create("iosBootSimulator", Task::class.java) {
		onlyIf { appleGetBootedDevice() == null }
		dependsOn(iosCreateIphone)
		doLast {
            val device = appleGetBootDevice(iphoneVersion)
            val udid = device.udid
            logger.info("Booting udid=$udid")
            if (logger.isInfoEnabled) {
                for (device in appleGetDevices()) {
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
                    commandLine("xcrun", "xcodebuild", "-scheme", "app-$arch-$debugSuffix", "-project", ".", "-configuration", debugSuffix, "-derivedDataPath", "build", "-arch", arch2, "-sdk", appleFindSdk(sdkName))
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
                val device = appleGetInstallDevice(iphoneVersion)
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
                val device = appleGetInstallDevice(iphoneVersion)
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

data class IosDevice(val booted: Boolean, val isAvailable: Boolean, val name: String, val udid: String)

// https://gist.github.com/luckman212/ec52e9291f27bc39c2eecee07e7a9aa7
fun Project.appleGetDefaultDeveloperCertificateTeamId(): String? {
    @Throws(IOException::class)
    fun execCmd(cmd: String?): String {
        return Runtime.getRuntime().exec(cmd).inputStream.reader().readText()
    }

    val certB64 = execCmd("security find-certificate -p")
        .replace("-----BEGIN CERTIFICATE-----", "")
        .replace("-----END CERTIFICATE-----", "")
        .lines()
        .joinToString("")

    val cert = CertificateFactory.getInstance("X.509").generateCertificate(ByteArrayInputStream(Base64.getDecoder().decode(certB64))) as X509Certificate
    val subjectStr = cert.subjectX500Principal.getName(X500Principal.RFC2253)

    return Regex("OU=(\\w+)").find(subjectStr)?.groups?.get(1)?.value
}

fun Project.appleGetDevices(os: String = "iOS"): List<IosDevice> = KDynamic {
	val res = Json.parse(execOutput("xcrun", "simctl", "list", "-j", "devices"))
	val devices = res["devices"]
	val oses = devices.keys.map { it.str }
	val iosOses = oses.filter { it.contains(os) }
    iosOses.map { devices[it].list }.flatten().map {
        //println(it)
		IosDevice(it["state"].str == "Booted", it["isAvailable"].bool, it["name"].str, it["udid"].str).also {
		    //println(it)
        }
	}
}

fun Project.appleGetBootDevice(iphoneVersion: Int): IosDevice {
    val devices = appleGetDevices()
    return devices.firstOrNull { it.name == "iPhone $iphoneVersion" && it.isAvailable }
        ?: devices.firstOrNull { it.name.contains("iPhone") && it.isAvailable }
        ?: run {
            val errorMessage = "Can't find suitable available iPhone $iphoneVersion device"
            logger.info(errorMessage)
            for (device in devices) logger.info("- $device")
            error(errorMessage)
        }
}

fun Project.appleGetInstallDevice(iphoneVersion: Int): IosDevice {
    val devices = appleGetDevices()
    return devices.firstOrNull { it.name == "iPhone $iphoneVersion" && it.booted }
        ?: devices.firstOrNull { it.name.contains("iPhone") && it.booted }
        ?: error("Can't find suitable booted iPhone $iphoneVersion device")
}

fun Project.appleGetBootedDevice(os: String = "iOS"): IosDevice? = appleGetDevices(os).firstOrNull { it.booted }
fun Project.appleFindSdk(name: String): String = Regex("(${name}.*)").find(execOutput("xcrun", "xcodebuild", "-showsdks"))?.groupValues?.get(0) ?: error("Can't find sdk starting with $name")
fun Project.appleFindIphoneSimSdk(): String = appleFindSdk("iphonesimulator")
fun Project.appleFindIphoneOsSdk(): String = appleFindSdk("iphoneos")

//tasks.create<Task>("iosLaunchSimulator") {
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

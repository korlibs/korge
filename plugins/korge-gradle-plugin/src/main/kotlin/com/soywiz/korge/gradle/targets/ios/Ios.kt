package com.soywiz.korge.gradle.targets.ios

import com.soywiz.korge.gradle.*
import com.soywiz.korge.gradle.util.*
import com.soywiz.korge.gradle.util.get
import org.gradle.api.*
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import java.io.*

fun Project.configureNativeIos() {
	tasks.create("prepareKotlinNativeBootstrapIos") { task ->
		task.apply {
			doLast {
				File(buildDir, "platforms/native-ios/info.kt").apply {
					parentFile.mkdirs()
					writeText("""
						object MyIosGameWindow2 {
							fun setCustomCwd(cwd: String?) = run { com.soywiz.korio.file.std.customCwd = cwd }
							val gameWindow get() = com.soywiz.korgw.MyIosGameWindow
						}
					""".trimIndent())
				}
			}
		}
	}

	kotlin.apply {
		for (target in listOf(iosX64(), iosArm32(), iosArm64())) {
			//for (target in listOf(iosX64())) {
			target.also { target ->
				target.compilations["main"].also { compilation ->
					//for (type in listOf(NativeBuildType.DEBUG, NativeBuildType.RELEASE)) {
					//	//getLinkTask(NativeOutputKind.FRAMEWORK, type).embedBitcode = Framework.BitcodeEmbeddingMode.DISABLE
					//}
					compilation.outputKind(NativeOutputKind.FRAMEWORK)

					compilation.defaultSourceSet.kotlin.srcDir(File(buildDir, "platforms/native-desktop"))
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
							compilation.getLinkTask(NativeOutputKind.FRAMEWORK, type).dependsOn("prepareKotlinNativeIosProject")
						}
					}
				}
			}
		}
	}

	tasks.create("installXcodeGen") { task ->
		task.apply {
			onlyIf { !File("/usr/local/bin/xcodegen").exists() }
			doLast {
				val korlibsFolder = File(System.getProperty("user.home") + "/.korlibs").apply { mkdirs() }
				exec {
					it.commandLine("git", "clone", "https://github.com/yonaskolb/XcodeGen.git")
					it.workingDir(korlibsFolder)

				}
				exec {
					it.commandLine("make")
					it.workingDir(korlibsFolder["XcodeGen"])
				}
			}
		}
	}

	tasks.create("prepareKotlinNativeIosProject") { task ->
		task.dependsOn("installXcodeGen", "prepareKotlinNativeBootstrapIos", "prepareKotlinNativeBootstrap")
		task.doLast {
			val folder = File(buildDir, "platforms/ios")
			val swift = true
			if (swift) {
				folder["app/AppDelegate.swift"].ensureParents().writeText(Indenter {
					line("import UIKit")
					line("@UIApplicationMain")
					line("class AppDelegate: UIResponder, UIApplicationDelegate") {
						line("var window: UIWindow?")
						line("func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool") {
							line("return true")
						}
						line("func applicationWillResignActive(_ application: UIApplication)") {
						}
						line("func applicationDidEnterBackground(_ application: UIApplication)") {
						}
						line("func applicationWillEnterForeground(_ application: UIApplication)") {
						}
						line("func applicationDidBecomeActive(_ application: UIApplication)") {
						}
						line("func applicationWillTerminate(_ application: UIApplication)") {
						}
					}
				})

				folder["app/ViewController.swift"].ensureParents().writeText(Indenter {
					line("import UIKit")
					line("import GLKit")
					line("import GameMain")

					line("class ViewController: GLKViewController") {
						line("var context: EAGLContext? = nil")
						line("var gameWindow2: MyIosGameWindow2? = nil")
						line("var rootGameMain: RootGameMain? = nil")

						line("deinit") {
							line("self.tearDownGL()")

							line("if EAGLContext.current() === self.context") {
								line("EAGLContext.setCurrent(nil)")
							}
						}

						line("override func viewDidLoad()") {
							line("super.viewDidLoad()")

							line("self.gameWindow2 = MyIosGameWindow2.init()")
							line("self.rootGameMain = RootGameMain.init()")

							line("context = EAGLContext(api: .openGLES2)")
							line("if context == nil") {
								line("print(\"Failed to create ES context\")")
							}

							line("let view = self.view as! GLKView")
							line("view.context = self.context!")
							line("view.drawableDepthFormat = .format24")

							line("self.setupGL()")
						}

						line("override func didReceiveMemoryWarning()") {
							line("super.didReceiveMemoryWarning()")

							line("if self.isViewLoaded && self.view.window != nil") {
								line("self.view = nil")

								line("self.tearDownGL()")

								line("if EAGLContext.current() === self.context") {
									line("EAGLContext.setCurrent(nil)")
								}
								line("self.context = nil")
							}
						}

						line("func setupGL()") {
							line("EAGLContext.setCurrent(self.context)")

							// Change the working directory so that we can use C code to grab resource files
							line("if let path = Bundle.main.resourcePath") {
								line("let rpath = \"\\(path)/include/app/resources\"")
								line("FileManager.default.changeCurrentDirectoryPath(rpath)")
								line("self.gameWindow2?.setCustomCwd(cwd: rpath)")
							}

							line("engineInitialize()")

							line("let width = Float(view.frame.size.width) // * view.contentScaleFactor)")
							line("let height = Float(view.frame.size.height) // * view.contentScaleFactor)")
							line("engineResize(width: width, height: height)")
						}

						line("func tearDownGL()") {
							line("EAGLContext.setCurrent(self.context)")

							line("engineFinalize()")
						}

						// MARK: - GLKView and GLKViewController delegate methods
						line("func update()") {
							line("engineUpdate()")
						}

						// Render
						line("var initialized = false")
						line("var reshape = true")
						line("override func glkView(_ view: GLKView, drawIn rect: CGRect)") {
							//glClearColor(1.0, 0.5, Float(n) / 60.0, 1.0);
							line("if !initialized") {
								line("initialized = true")
								line("gameWindow2?.gameWindow.dispatchInitEvent()")
								line("rootGameMain?.runMain()")
								line("reshape = true")
							}
							line("let width = Int32(view.bounds.width * view.contentScaleFactor)")
							line("let height = Int32(view.bounds.height * view.contentScaleFactor)")
							line("if reshape") {
								line("reshape = false")
								line("gameWindow2?.gameWindow.dispatchReshapeEvent(x: 0, y: 0, width: width, height: height)")
							}

							line("gameWindow2?.gameWindow.ag.setViewport(x: 0, y: 0, width: width, height: height)")
							line("gameWindow2?.gameWindow.frame()")
						}

						line("private func engineInitialize()") {
							//print("init[a]")
							//glClearColor(1.0, 0.5, Float(n) / 60.0, 1.0);
							//glClear(GLbitfield(GL_COLOR_BUFFER_BIT));
							//gameWindow2?.gameWindow.frame()
							//print("init[b]")
						}

						line("private func engineFinalize()") {
						}

						line("private func engineResize(width: Float, height: Float)") {
						}

						line("private func engineUpdate()") {
						}
					}
				})
			} else {
				folder["app/main.m"].ensureParents().writeText(Indenter {
					line("#import <UIKit/UIKit.h>")
					line("#import \"AppDelegate.h\"")
					line("int main(int argc, char * argv[])") {
						line("@autoreleasepool") {
							line("return UIApplicationMain(argc, argv, nil, NSStringFromClass([AppDelegate class]));")
						}
					}
				})
			}

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
					line("<key>UIMainStoryboardFile</key>")
					line("<string>Main</string>")
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

			folder["app/Base.lproj/Main.storyboard"].ensureParents().writeText(Indenter {
				line("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
				line("<document type=\"com.apple.InterfaceBuilder3.CocoaTouch.Storyboard.XIB\" version=\"3.0\" toolsVersion=\"14460.31\" targetRuntime=\"iOS.CocoaTouch\" propertyAccessControl=\"none\" useAutolayout=\"YES\" useTraitCollections=\"YES\" useSafeAreas=\"YES\" colorMatched=\"YES\" initialViewController=\"v5j-5E-UgZ\">")
				indent {
					line("<device id=\"retina4_7\" orientation=\"portrait\">")
					indent {
						line("<adaptation id=\"fullscreen\"/>")
					}
					line("</device>")
					line("<dependencies>")
					indent {
						line("<deployment identifier=\"iOS\"/>")
						line("<plugIn identifier=\"com.apple.InterfaceBuilder.IBCocoaTouchPlugin\" version=\"14460.20\"/>")
						line("<capability name=\"Safe area layout guides\" minToolsVersion=\"9.0\"/>")
						line("<capability name=\"documents saved in the Xcode 8 format\" minToolsVersion=\"8.0\"/>")
					}
					line("</dependencies>")
					line("<scenes>")
					indent {
						line("<!--GLKit View Controller-->")
						line("<scene sceneID=\"yeC-8Z-GkV\">")
						indent {
							line("<objects>")
							indent {
								line("""<glkViewController preferredFramesPerSecond="30" id="v5j-5E-UgZ" customClass="ViewController" customModule="demo312321" customModuleProvider="target" sceneMemberID="viewController">""")
								indent {
									line("""<glkView key="view" opaque="NO" clipsSubviews="YES" multipleTouchEnabled="YES" contentMode="center" enableSetNeedsDisplay="NO" id="xs3-K8-37B">""")
									indent {
										line("""<rect key="frame" x="0.0" y="0.0" width="375" height="667"/>""")
										line("""<autoresizingMask key="autoresizingMask" widthSizable="YES" heightSizable="YES"/>""")
										line("""<viewLayoutGuide key="safeArea" id="bUC-GZ-6eT"/>""")
										line("""<connections>""")
										indent {
											line("""<outlet property="delegate" destination="v5j-5E-UgZ" id="2Fl-xi-v2b"/>""")
										}
										line("""</connections>""")
									}
									line("""</glkView>""")
								}
								line("""</glkViewController>""")
								line("""<placeholder placeholderIdentifier="IBFirstResponder" id="6d6-DE-sl0" userLabel="First Responder" sceneMemberID="firstResponder"/>""")
							}
							line("</objects>")
						}
						line("</scene>")
					}
					line("</scenes>")
				}
				line("</document>")
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


			folder["app/Assets.xcassets/AppIcon.appiconset/Contents.json"].ensureParents().writeText("""
				{
				  "images" : [
					{ "idiom" : "iphone", "size" : "20x20", "scale" : "2x" },
					{ "idiom" : "iphone", "size" : "20x20", "scale" : "3x" },
					{ "idiom" : "iphone", "size" : "29x29", "scale" : "2x" },
					{ "idiom" : "iphone", "size" : "29x29", "scale" : "3x" },
					{ "idiom" : "iphone", "size" : "40x40", "scale" : "2x" },
					{ "idiom" : "iphone", "size" : "40x40", "scale" : "3x" },
					{ "idiom" : "iphone", "size" : "60x60", "scale" : "2x" },
					{ "idiom" : "iphone", "size" : "60x60", "scale" : "3x" },
					{ "idiom" : "ipad", "size" : "20x20", "scale" : "1x" },
					{ "idiom" : "ipad", "size" : "20x20", "scale" : "2x" },
					{ "idiom" : "ipad", "size" : "29x29", "scale" : "1x" },
					{ "idiom" : "ipad", "size" : "29x29", "scale" : "2x" },
					{ "idiom" : "ipad", "size" : "40x40", "scale" : "1x"},
					{ "idiom" : "ipad", "size" : "40x40", "scale" : "2x" },
					{ "idiom" : "ipad", "size" : "76x76", "scale" : "1x" },
					{ "idiom" : "ipad", "size" : "76x76", "scale" : "2x" },
					{ "idiom" : "ipad", "size" : "83.5x83.5", "scale" : "2x" },
					{ "idiom" : "ios-marketing", "size" : "1024x1024", "scale" : "1x" }
				  ],
				  "info" : { "version" : 1, "author" : "xcode" }
				}
			""".trimIndent())


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
					if (korge.appleDevelopmentTeamId != null) {
						line("DEVELOPMENT_TEAM: ${korge.appleDevelopmentTeamId}")
					}
				}
				line("targets:")

				indent {
					for (target in listOf("X64", "Arm64", "Arm32")) {
						line("app-$target:")
						indent {
							line("platform: iOS")
							line("type: application")
							line("deploymentTarget: \"10.0\"")
							line("sources:")
							indent {
								line("- app")
								line("- path: ../../../src/commonMain/resources")
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
							line("dependencies:")
							line("  - framework: ../../bin/ios$target/mainDebugFramework/GameMain.framework")
						}
					}
				}
			}.replace("\t", "  "))

			exec {
				it.workingDir(folder)
				it.commandLine("xcodegen")
			}
		}
	}

	tasks.create("iosShutdownSimulator", Exec::class.java) { task ->
		task.commandLine("xcrun", "simctl", "shutdown", "booted")
	}

	tasks.create("iosCreateIphone7", Exec::class.java) { task ->
		task.onlyIf { appleGetDevices().none { it.name == "iPhone 7" } }
		task.doFirst {
			task.commandLine("xcrun", "simctl", "create", "iPhone 7", "com.apple.CoreSimulator.SimDeviceType.iPhone-7", "com.apple.CoreSimulator.SimRuntime.iOS-12-1")
		}
	}

	tasks.create("iosBootSimulator", Task::class.java) { task ->
		task.onlyIf { appleGetBootedDevice() == null }
		task.dependsOn("iosCreateIphone7")
		task.doLast {
			val udid = appleGetDevices().firstOrNull { it.name == "iPhone 7" }?.udid ?: error("Can't find iPhone 7 device")
			exec { it.commandLine("xcrun", "simctl", "boot", udid) }
			exec { it.commandLine("sh", "-c", "open `xcode-select -p`/Applications/Simulator.app/ --args -CurrentDeviceUDID $udid") }
		}
	}

	for (debug in listOf(false, true)) {
		val debugSuffix = if (debug) "Debug" else "Release"
		for (simulator in listOf(false, true)) {
			val simulatorSuffix = if (simulator) "Simulator" else "Device"
			val arch = if (simulator) "X64" else "Arm64"
			val arch2 = if (simulator) "x64" else "arm64"
			val sdkName = if (simulator) "iphonesimulator" else "iphoneos"
			tasks.create("iosBuild$simulatorSuffix$debugSuffix") { task ->
				task.dependsOn("prepareKotlinNativeIosProject", "linkMain${debugSuffix}FrameworkIos$arch")
				val xcodeProjDir = buildDir["platforms/ios/app.xcodeproj"]
				task.outputs.file(xcodeProjDir["build/Build/Products/$debugSuffix-$sdkName/${korge.name}.app/${korge.name}"])
				task.doLast {
					exec {
						it.workingDir(xcodeProjDir)
						it.commandLine("xcrun", "xcodebuild", "-scheme", "app-$arch", "-project", ".", "-configuration", debugSuffix, "-derivedDataPath", "build", "-arch", arch2, "-sdk", appleFindSdk(sdkName))
					}
				}
			}
		}

		tasks.create("iosInstallSimulator$debugSuffix", Exec::class.java) { task ->
			task.dependsOn("iosBuildSimulator$debugSuffix", "iosBootSimulator")
			task.doFirst {
				val appFolder = tasks.getByName("iosBuildSimulator$debugSuffix").outputs.files.first().parentFile
				val udid = appleGetDevices().firstOrNull { it.name == "iPhone 7" }?.udid ?: error("Can't find iPhone 7 device")
				task.commandLine("xcrun", "simctl", "install", udid, appFolder.absolutePath)
			}
		}
	}


	tasks.create("iosEraseAllSimulators") { task ->
		task.doLast { exec { it.commandLine("osascript", "-e", "tell application \"iOS Simulator\" to quit") } }
		task.doLast { exec { it.commandLine("osascript", "-e", "tell application \"Simulator\" to quit") } }
		task.doLast { exec { it.commandLine("xcrun", "simctl", "erase", "all") } }
	}



}

data class IosDevice(val booted: Boolean, val isAvailable: Boolean, val name: String, val udid: String)

fun Project.appleGetDevices(os: String = "iOS"): List<IosDevice> = KDynamic {
	val res = Json.parse(execOutput("xcrun", "simctl", "list", "-j", "devices"))
	val devices = res["devices"]
	val oses = devices.keys.map { it.str }
	//val iosOs = oses.firstOrNull { it.contains(os) } ?: error("No iOS devices created")
	val iosOs = oses.firstOrNull { it.contains(os) } ?: listOf<String>()
	devices[iosOs].list.map {
		IosDevice(it["state"].str == "Booted", it["isAvailable"].bool, it["name"].str, it["udid"].str)
	}
}

fun Project.appleGetBootedDevice(os: String = "iOS"): IosDevice? = appleGetDevices(os).firstOrNull { it.booted }
fun Project.appleFindSdk(name: String): String = Regex("(${name}.*)").find(execOutput("xcrun", "xcodebuild", "-showsdks"))?.groupValues?.get(0) ?: error("Can't find sdk starting with $name")
fun Project.appleFindIphoneSimSdk(): String = appleFindSdk("iphonesimulator")
fun Project.appleFindIphoneOsSdk(): String = appleFindSdk("iphoneos")

//tasks.create<Task>("iosLaunchSimulator") {
//	dependsOn("iosInstallSimulator")
//	doLast {
//		val udid = appleGetDevices().firstOrNull { it.name == "iPhone 7" }?.udid ?: error("Can't find iPhone 7 device")
//		exec { commandLine("xcrun", "simctl", "launch", "-w", udid, korge.id) }
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

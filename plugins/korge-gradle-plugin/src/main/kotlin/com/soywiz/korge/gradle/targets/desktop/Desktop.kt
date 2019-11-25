package com.soywiz.korge.gradle.targets.desktop

import com.soywiz.korge.gradle.*
import com.soywiz.korge.gradle.targets.*
import com.soywiz.korge.gradle.targets.apple.*
import com.soywiz.korge.gradle.targets.native.*
import com.soywiz.korge.gradle.targets.windows.*
import com.soywiz.korge.gradle.util.*
import com.soywiz.korge.gradle.util.get
import com.soywiz.korim.format.ImageData
import com.soywiz.korim.format.ImageFrame
import com.soywiz.korim.format.PNG
import groovy.time.BaseDuration
import org.gradle.api.*
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.tasks.*
import java.io.*

private val RELEASE = NativeBuildType.RELEASE
private val DEBUG = NativeBuildType.DEBUG
private val RELEASE_DEBUG = listOf(NativeBuildType.RELEASE, NativeBuildType.DEBUG)

private val DESKTOP_NATIVE_TARGET = when {
	isWindows -> "mingwX64"
	isMacos -> "macosX64"
	isLinux -> "linuxX64"
	else -> "unknownX64"
}

val DESKTOP_NATIVE_TARGETS = when {
	isWindows -> listOf("mingwX64")
	isMacos -> listOf("macosX64")
	isLinux -> listOf("linuxX64")
	else -> listOf("mingwX64", "linuxX64", "macosX64")
}

private val cnativeTarget = DESKTOP_NATIVE_TARGET.capitalize()

fun Project.configureNativeDesktop() {
	val project = this

	for (preset in DESKTOP_NATIVE_TARGETS) {
        //val target = gkotlin.presets.getAt(preset) as KotlinNativeTargetPreset
		gkotlin.targets.add((gkotlin.presets.getAt(preset) as AbstractKotlinNativeTargetPreset<*>).createTarget(preset).apply {
			//(compilations["main"] as KotlinNativeCompilation).outputKinds("EXECUTABLE")
			binaries {
                executable {
                    //this.entryPoint = "korge.bootstrap.main"
                    //this.entryPoint = "korge.bootstrap"
                }
            }
		})
	}


	val prepareKotlinNativeBootstrap = tasks.create("prepareKotlinNativeBootstrap") { task ->
		task.apply {
			val output = File(buildDir, "platforms/native-desktop/bootstrap.kt")
			outputs.file(output)
			doLast {
				output.parentFile.mkdirs()

				val text = Indenter {
                    //line("package korge.bootstrap")
					line("import ${korge.entryPoint}")
					line("fun main(args: Array<String>): Unit = RootGameMain.runMain(args)")
					line("object RootGameMain") {
						line("fun runMain() = runMain(arrayOf())")
						line("@Suppress(\"UNUSED_PARAMETER\") fun runMain(args: Array<String>): Unit = com.soywiz.korio.Korio { ${korge.entryPoint}() }")
					}
				}
				if (!output.exists() || output.readText() != text) output.writeText(text)
			}
		}
	}

	afterEvaluate {
		//for (target in listOf(kotlin.macosX64(), kotlin.linuxX64(), kotlin.mingwX64(), kotlin.iosX64(), kotlin.iosArm64(), kotlin.iosArm32())) {
		for (target in when {
			isWindows -> listOf(kotlin.mingwX64())
			isMacos -> listOf(kotlin.macosX64())
			isLinux -> listOf(kotlin.linuxX64())
			else -> listOf(kotlin.macosX64(), kotlin.linuxX64(), kotlin.mingwX64())
		}) {
			target.apply {
				compilations["main"].apply {
					//println(this.binariesTaskName)
					for (type in listOf(NativeBuildType.DEBUG, NativeBuildType.RELEASE)) {
						getLinkTask(NativeOutputKind.EXECUTABLE, type, project).dependsOn(prepareKotlinNativeBootstrap)
					}
					defaultSourceSet.kotlin.srcDir(File(buildDir, "platforms/native-desktop"))
				}
				/*
            binaries {
                executable {
                    println("linkTask = $linkTask")
                    linkTask.dependsOn(prepareKotlinNativeBootstrap)
                }
            }
            */
			}
		}
	}

	project.afterEvaluate {
		for (target in DESKTOP_NATIVE_TARGETS) {
			val taskName = "copyResourcesToExecutableTest_${target.capitalize()}"
			val targetTestTask = project.tasks.getByName("${target}Test")
			val task = project.addTask<Copy>(taskName) { task ->
				for (sourceSet in project.gkotlin.sourceSets) {
					task.from(sourceSet.resources)
				}
				task.into(File(targetTestTask.inputs.properties["executable"].toString()).parentFile)
			}
			targetTestTask.dependsOn(task)
		}

		// @TODO: This doesn't work after migrating code to Kotlin.
		//for (target in listOf(project.gkotlin.targets["js"], project.gkotlin.targets["metadata"])) {
		//    for (it in (target["compilations"]["main"]["kotlinSourceSets"]["resources"] as Iterable<SourceDirectorySet>).flatMap { it.srcDirs }) {
		//        (tasks["jsJar"] as Copy).from(it)
		//    }
		//}
	}

	addNativeRun()
}

private fun Project.addNativeRun() {
	val project = this

	fun KotlinNativeCompilation.getBinary(kind: NativeOutputKind, type: NativeBuildType): File {
		return this.getLinkTask(kind, type, project).binary.outputFile.absoluteFile
	}

	afterEvaluate {
	//run {
		for (target in DESKTOP_NATIVE_TARGETS) {
			val ctarget = target.capitalize()
			for (kind in RELEASE_DEBUG) {
				val ckind = kind.name.toLowerCase().capitalize()
				val ctargetKind = "$ctarget$ckind"
				val buildType = if (kind == DEBUG) NativeBuildType.DEBUG else NativeBuildType.RELEASE

				val ktarget = gkotlin.targets[target]
				//(ktarget as KotlinNativeTarget).attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)

				val compilation = ktarget["compilations"]["main"] as KotlinNativeCompilation
				val executableFile = compilation.getBinary(NativeOutputKind.EXECUTABLE, buildType)

				val copyTask = project.addTask<Copy>("copyResourcesToExecutable$ctargetKind") { task ->
					task.dependsOn(compilation.compileKotlinTask)
					for (sourceSet in project.gkotlin.sourceSets) {
						task.from(sourceSet.resources)
					}
					//println("executableFile.parentFile: ${executableFile.parentFile}")
					task.into(executableFile.parentFile)
					if (target == "mingwX64") {
						val appRcFile = buildDir["app.rc"]
						val appRcObjFile = buildDir["app.obj"]
						val appIcoFile = buildDir["icon.ico"]

						doLast {
							val bmp32 = PNG.decode(project.korge.getIconBytes(32))
							val bmp256 = PNG.decode(project.korge.getIconBytes(256))

							appIcoFile.writeBytes(ICO2.encode(ImageData(listOf(ImageFrame(bmp32), ImageFrame(bmp256)))))
							appRcFile.writeText(WindowsRC.generate(korge))
							project.compileWindowsRC(appRcFile, appRcObjFile)

							val linkTask = compilation.getLinkTask(NativeOutputKind.EXECUTABLE, buildType, project)
							val subsystem = "console"
							//val subsystem = "windows"
							linkTask.binary.linkerOpts(
								appRcObjFile.absolutePath, "-Wl,--subsystem,$subsystem"
							)
						}
						//println("compilation:$compilation")
						//compilation.linkerOpts(appRcObjFile.absolutePath, "-Wl,--subsystem,console")
						afterEvaluate {
 						}
					}
				}

				afterEvaluate {
					try {
						compilation.getLinkTask(NativeOutputKind.EXECUTABLE, buildType, project).dependsOn(copyTask)
					} catch (e: Throwable) {
						e.printStackTrace()
					}
				}

				//addTask<Exec>("runNative$ctargetKind", dependsOn = listOf("linkMain${ckind}Executable$ctarget", copyTask), group = GROUP_KORGE) { task ->
				addTask<Exec>("runNative$ctargetKind", dependsOn = listOf("link${ckind}Executable$ctarget", copyTask), group = GROUP_KORGE) { task ->
					task.group = GROUP_KORGE_RUN
					task.executable = executableFile.absolutePath
					task.args = listOf<String>()
				}
			}
			addTask<Task>("runNative$ctarget", dependsOn = listOf("runNative${ctarget}Release"), group = GROUP_KORGE) { task ->
				task.group = GROUP_KORGE_RUN
			}
		}
	}

	addTask<Task>("runNative", dependsOn = listOf("runNative$cnativeTarget"), group = GROUP_KORGE_RUN)
	addTask<Task>("runNativeDebug", dependsOn = listOf("runNative${cnativeTarget}Debug"), group = GROUP_KORGE_RUN)

	afterEvaluate {
		if (isMacos) {
			for (buildType in RELEASE_DEBUG) {
				addTask<Task>(
					"packageMacosX64App${buildType.name.capitalize()}",
					group = GROUP_KORGE_PACKAGE,
					//dependsOn = listOf("linkMain${buildType.name.capitalize()}ExecutableMacosX64")
					dependsOn = listOf("link${buildType.name.capitalize()}ExecutableMacosX64")
				) {
					group = GROUP_KORGE_PACKAGE
					doLast {
						val compilation = gkotlin.targets["macosX64"]["compilations"]["main"] as KotlinNativeCompilation
						val executableFile = compilation.getBinary(NativeOutputKind.EXECUTABLE, buildType)
						val appFolder = buildDir["${korge.name}-$buildType.app"].apply { mkdirs() }
						val appFolderContents = appFolder["Contents"].apply { mkdirs() }
						val appMacOSFolder = appFolderContents["MacOS"].apply { mkdirs() }
						val resourcesFolder = appFolderContents["Resources"].apply { mkdirs() }
						appFolderContents["Info.plist"].writeText(InfoPlistBuilder.build(korge))
						resourcesFolder["${korge.exeBaseName}.icns"].writeBytes(IcnsBuilder.build(korge.getIconBytes()))
						copy { copy ->
							for (sourceSet in project.gkotlin.sourceSets) {
								copy.from(sourceSet.resources)
							}
							copy.into(resourcesFolder)
						}
						executableFile.copyTo(appMacOSFolder[korge.exeBaseName], overwrite = true)
						appMacOSFolder[korge.exeBaseName].setExecutable(true)
					}
				}
			}
		}
		if (isWindows) {
			val target = "mingwX64"
			val compilation = gkotlin.targets[target]["compilations"]["main"] as KotlinNativeCompilation
			for (kind in RELEASE_DEBUG) {
				val buildType = if (kind == DEBUG) NativeBuildType.DEBUG else NativeBuildType.RELEASE
				val linkTask = compilation.getLinkTask(NativeOutputKind.EXECUTABLE, buildType, project)

				addTask<Task>(
					"packageMingwX64App${kind.name.capitalize()}",
					group = GROUP_KORGE_PACKAGE,
					dependsOn = listOf(linkTask)
				) {
					val inputFile = linkTask.binary.outputFile
					val strippedFile = inputFile.parentFile[inputFile.nameWithoutExtension + "-stripped.exe"]
					doLast {
						val bytes = inputFile.readBytes()
						bytes[0xDC] = 2 // Convert exe into WINDOW_GUI(2) subsystem (prevents opening a console window)
						strippedFile.writeBytes(bytes)
						stripWindowsExe(strippedFile)
					}
				}
			}
		}
	}
}

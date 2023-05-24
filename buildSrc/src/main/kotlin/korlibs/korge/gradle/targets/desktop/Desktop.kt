package korlibs.korge.gradle.targets.desktop

import korlibs.korge.gradle.*
import korlibs.korge.gradle.gkotlin
import korlibs.korge.gradle.targets.*
import korlibs.korge.gradle.targets.apple.*
import korlibs.korge.gradle.targets.native.*
import korlibs.korge.gradle.targets.windows.*
import korlibs.korge.gradle.util.*
import korlibs.*
import korlibs.korge.gradle.module.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.native.tasks.*
import java.io.*

fun Project.configureNativeDesktop(projectType: ProjectType) {
	val project = this

    /*
    val common = gkotlin.sourceSets.createPairSourceSet("common") { test ->
        dependencies {
            if (test) {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            } else {
                implementation(kotlin("stdlib-common"))
            }
        }
    }
    val concurrent = gkotlin.sourceSets.createPairSourceSet("concurrent", common)
    val nonNativeCommon = gkotlin.sourceSets.createPairSourceSet("nonNativeCommon", common)
    val nonJs = gkotlin.sourceSets.createPairSourceSet("nonJs", common)
    val nonJvm = gkotlin.sourceSets.createPairSourceSet("nonJvm", common)
    val jvmAndroid = gkotlin.sourceSets.createPairSourceSet("jvmAndroid", common)
    val nativeCommon by lazy { gkotlin.sourceSets.createPairSourceSet("nativeCommon", concurrent) }
    */

	for (preset in DESKTOP_NATIVE_TARGETS) {
        //val target = gkotlin.presets.getAt(preset) as KotlinNativeTargetPreset
		gkotlin.targets.add((gkotlin.presets.getAt(preset) as AbstractKotlinNativeTargetPreset<*>).createTarget(preset).apply {
            configureKotlinNativeTarget(project)
            //val target = this
            //val native = gkotlin.sourceSets.createPairSourceSet(target.name, common, nativeCommon, nonJvm, nonJs)
            //native.dependsOn(nativeCommon)

            //(compilations["main"] as KotlinNativeCompilation).outputKinds("EXECUTABLE")
			binaries {
                executable {
                    //this.entryPoint = "korge.bootstrap.main"
                    //this.entryPoint = "korge.bootstrap"
                }
            }
		})
	}

	//project.afterEvaluate {}


	afterEvaluate {
		for (target in convertNamesToNativeTargets(DESKTOP_NATIVE_TARGETS)) {
			val mainCompilation = target.compilations["main"]
			for (type in NativeBuildTypes.TYPES) {
				mainCompilation.getCompileTask(NativeOutputKind.EXECUTABLE, type, project).dependsOn(prepareKotlinNativeBootstrap)
			}
			mainCompilation.defaultSourceSet.kotlin.srcDir(project.file("build/platforms/native-desktop/"))
		}
	}

	project.afterEvaluate {
		for (target in DESKTOP_NATIVE_TARGETS) {
            createCopyToTestExecutableTarget(target)
		}

		// @TODO: This doesn't work after migrating code to Kotlin.
		//for (target in listOf(project.gkotlin.targets["js"], project.gkotlin.targets["metadata"])) {
		//    for (it in (target["compilations"]["main"]["kotlinSourceSets"]["resources"] as Iterable<SourceDirectorySet>).flatMap { it.srcDirs }) {
		//        (tasks["jsJar"] as Copy).from(it)
		//    }
		//}
	}

    if (projectType.isExecutable) {
        configureNativeDesktopRun()
    }
}

fun Project.createCopyToTestExecutableTarget(target: String) {
    if (isLinux && target.endsWith("Arm64")) {
        // don't create an Arm64 test task
        return
    }
    val taskName = "copyResourcesToExecutableTest_${target.capitalize()}"
    val targetTestTask = project.tasks.getByName("${target}Test") as KotlinNativeTest
    val task = project.tasks.createThis<Copy>(taskName) {
        for (sourceSet in project.gkotlin.sourceSets) {
            from(sourceSet.resources)
        }
        into(targetTestTask.executableFolder)
    }
    targetTestTask.dependsOn(task)
}

class KNTargetWithBuildType(val project: Project, val target: String, val kind: NativeBuildType) {
    companion object {
        fun buildList(project: Project, target: String): List<KNTargetWithBuildType> = NativeBuildTypes.TYPES.map { KNTargetWithBuildType(project, target, it) }
    }
    val ctarget = target.capitalize()
    val isMingwX64 = target == "mingwX64"

    val ckind = kind.name.toLowerCase().capitalize()
    val ctargetKind = "$ctarget$ckind"
    val buildType = if (kind == NativeBuildType.DEBUG) NativeBuildType.DEBUG else NativeBuildType.RELEASE

    val ktarget = project.gkotlin.targets.getByName(target)
    //(ktarget as KotlinNativeTarget).attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)

    val compilation = ktarget.compilations.main as KotlinNativeCompilation

    //if (isMingwX64) {
    //    //compilation.getLinkTask(NativeOutputKind.EXECUTABLE, buildType, project).dependsOn(mingwX64SelectPatchedMemoryManager)
    //}

    val executableFile = compilation.getBinary(NativeOutputKind.EXECUTABLE, buildType)

    val appResFile = File(project.buildDir, "app.res")
    val appRcFile = File(project.buildDir, "app.rc")
    val appRcObjFile = File(project.buildDir, "app.obj")
    val appIcoFile = File(project.buildDir, "icon.ico")

    val copyTaskName = "copyResourcesToExecutable${ctargetKind}"
}


fun KotlinNativeCompilation.getBinary(kind: NativeOutputKind, type: NativeBuildType): File {
    return this.getLinkTask(kind, type, project).binary.outputFile.absoluteFile
}

fun Project.configureNativeDesktopRun() {
    val project = this

    afterEvaluate {
        //run {
        for (target in DESKTOP_NATIVE_TARGETS) {
            createCopyToExecutableTarget(target)
            val builds = KNTargetWithBuildType.buildList(project, target)
            for (build in builds) {
                tasks.createThis<Exec>("runNative${build.ctargetKind}") {
                    dependsOn("link${build.ckind}Executable${build.ctarget}", build.copyTaskName)
                    group = GROUP_KORGE_RUN
                    executable = build.executableFile.absolutePath
                    args()
                }
            }
            tasks.createThis<Task>("runNative${builds.first().ctarget}") {
                dependsOn("runNative${builds.first().ctarget}Release")
                group = GROUP_KORGE_RUN
            }
        }
    }

    tasks.createThis<Task>("runNative") {
        dependsOn("runNative$cnativeTarget")
        group = GROUP_KORGE_RUN
    }
    tasks.createThis<Task>("runNativeDebug") {
        dependsOn("runNative${cnativeTarget}Debug")
        group = GROUP_KORGE_RUN
    }
    tasks.createThis<Task>("runNativeRelease") {
        dependsOn("runNative${cnativeTarget}Release")
        group = GROUP_KORGE_RUN
    }

    afterEvaluate {
        if (isMacos) {
            for (buildType in NativeBuildTypes.TYPES) {
                val buildTypeLC = buildType.name.toLowerCase()
                for (nativeTargetName in listOf("macosArm64", "macosX64")) {
                    //val nativeTargetName = if (isArm) "macosArm64" else "macosX64"

                    val ktarget = gkotlin.targets[nativeTargetName]
                    //(ktarget as KotlinNativeTarget).attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
                    val compilation = ktarget.compilations.main as KotlinNativeCompilation

                    tasks.createThis<Task>(
                        "package${nativeTargetName.capitalize()}App${buildTypeLC.capitalize()}", // @TODO: What happens on macosArm64?
                        //dependsOn = listOf("link${buildType.name.capitalize()}ExecutableMacosX64")
                    ) {
                        group = GROUP_KORGE_PACKAGE
                        dependsOn(compilation.getLinkTask(NativeOutputKind.EXECUTABLE, buildType, project))
                        doLast {
                            val compilation = gkotlin.targets.getByName(nativeTargetName).compilations.main as KotlinNativeCompilation
                            val executableFile = compilation.getBinary(NativeOutputKind.EXECUTABLE, buildType)
                            val appFolder = File(buildDir, "${korge.name}-${nativeTargetName}-${buildTypeLC}.app").apply { mkdirs() }
                            val appFolderContents = File(appFolder, "Contents").apply { mkdirs() }
                            val appMacOSFolder = File(appFolderContents, "MacOS").apply { mkdirs() }
                            val resourcesFolder = File(appFolderContents, "Resources").apply { mkdirs() }
                            File(appFolderContents, "Info.plist").writeText(InfoPlistBuilder.build(korge))
                            File(resourcesFolder, "${korge.exeBaseName}.icns").writeBytes(IcnsBuilder.build(korge.getIconBytes()))
                            copy { copy ->
                                copy.duplicatesStrategy = DuplicatesStrategy.INCLUDE
                                for (sourceSet in project.gkotlin.sourceSets) {
                                    copy.from(sourceSet.resources)
                                }
                                copy.into(resourcesFolder)
                            }
                            executableFile.copyTo(File(appMacOSFolder, korge.exeBaseName), overwrite = true)
                            File(appMacOSFolder, korge.exeBaseName).setExecutable(true)
                        }
                    }
                }
            }
        }
        if (isWindows) {
            val target = "mingwX64"
            val compilation = gkotlin.targets.getByName(target).compilations.main as KotlinNativeCompilation
            for (kind in NativeBuildTypes.TYPES) {
                val buildType = if (kind == NativeBuildType.DEBUG) NativeBuildType.DEBUG else NativeBuildType.RELEASE
                val linkTask = compilation.getLinkTask(NativeOutputKind.EXECUTABLE, buildType, project)

                tasks.createThis<Task>(
                    "packageMingwX64App${kind.name.capitalize()}",
                ) {
                    dependsOn(linkTask)
                    group = GROUP_KORGE_PACKAGE
                    val inputFile = linkTask.binary.outputFile
                    val strippedFile = File(inputFile.parentFile, inputFile.nameWithoutExtension + "-stripped.exe")
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

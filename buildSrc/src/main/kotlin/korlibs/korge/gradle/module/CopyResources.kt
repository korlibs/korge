package korlibs.korge.gradle.module

import korlibs.korge.gradle.*
import korlibs.korge.gradle.targets.*
import korlibs.korge.gradle.targets.desktop.*
import korlibs.korge.gradle.targets.native.*
import korlibs.korge.gradle.targets.windows.*
import korlibs.korge.gradle.util.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

fun Project.createCopyToExecutableTarget(target: String) {
    for (build in KNTargetWithBuildType.buildList(project, target)) {
        val copyTask = project.tasks.createThis<Copy>(build.copyTaskName) {
            run {
                val processResourcesTaskName = getProcessResourcesTaskName(build.target, build.compilation.name)
                dependsOn(processResourcesTaskName)
                afterEvaluate {
                    afterEvaluate {
                        afterEvaluate {
                            val korgeGenerateResourcesTask = tasks.findByName(processResourcesTaskName) as? Copy?
                            //korgeGenerateResourcesTask?.korgeGeneratedFolder?.let { from(it) }
                            from(korgeGenerateResourcesTask?.outputs)
                        }
                    }
                }
            }
            run {
                val processResourcesTaskName = getKorgeProcessResourcesTaskName(build.target, build.compilation.name)
                dependsOn(processResourcesTaskName)
                afterEvaluate {
                    afterEvaluate {
                        afterEvaluate {
                            val korgeGenerateResourcesTask =
                                tasks.findByName(processResourcesTaskName) as? KorgeGenerateResourcesTask?
                            //korgeGenerateResourcesTask?.korgeGeneratedFolder?.let { from(it) }
                            korgeGenerateResourcesTask?.addToCopySpec(this@createThis, addFrom = false)
                        }
                    }
                }
            }
            dependsOn(build.compilation.compileKotlinTask)
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
            for (sourceSet in project.gkotlin.sourceSets) {
                from(sourceSet.resources)
            }
            //println("executableFile.parentFile: ${executableFile.parentFile}")
            into(build.executableFile.parentFile)
            if (build.isMingwX64) {
                //if (false) {
                doLast {
                    val bmp32 = project.korge.getIconBytes(32).decodeImage()
                    val bmp256 = project.korge.getIconBytes(256).decodeImage()

                    build.appIcoFile.writeBytes(ICO2.encode(listOf(bmp32, bmp256)))

                    val linkTask = build.compilation.getLinkTask(NativeOutputKind.EXECUTABLE, build.buildType, project)

                    val isConsole = korge.enableConsole ?: (build.kind == NativeBuildType.DEBUG)
                    val subsystem = if (isConsole) "console" else "windows"

                    run {
                        // @TODO: https://releases.llvm.org/9.0.0/tools/lld/docs/ReleaseNotes.html#id6
                        // @TODO: lld-link now rejects more than one resource object input files, matching link.exe. Previously, lld-link would silently ignore all but one. If you hit this: Don’t pass resource object files to the linker, instead pass res files to the linker directly. Don’t put resource files in static libraries, pass them on the command line. (r359749)
                        //appRcFile.writeText(WindowsRC.generate(korge))
                        //project.compileWindowsRC(appRcFile, appRcObjFile)
                        //linkTask.binary.linkerOpts(appRcObjFile.absolutePath, "-Wl,--subsystem,$subsystem")
                    }

                    run {
                        // @TODO: Not working either!
                        //appRcFile.writeText(WindowsRC.generate(korge))
                        //val appResFile = project.compileWindowsRES(appRcFile)
                        //linkTask.binary.linkerOpts(appResFile.absolutePath, "-Wl,--subsystem,$subsystem")
                    }

                    run {
                        build.appRcFile.writeText(WindowsRC.generate(korge))
                        project.compileWindowsRES(build.appRcFile, build.appResFile)
                        linkTask.binary.linkerOpts("-Wl,--subsystem,$subsystem")
                    }
                }
                //println("compilation:$compilation")
                //compilation.linkerOpts(appRcObjFile.absolutePath, "-Wl,--subsystem,console")
                afterEvaluate {
                }
            }
        }

        afterEvaluate {
            try {
                val linkTask = build.compilation.getLinkTask(NativeOutputKind.EXECUTABLE, build.buildType, project)
                linkTask.dependsOn(copyTask)
                if (build.isMingwX64) {
                    linkTask.doLast {
                        replaceExeWithRes(linkTask.outputFile.get(), build.appResFile)
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}

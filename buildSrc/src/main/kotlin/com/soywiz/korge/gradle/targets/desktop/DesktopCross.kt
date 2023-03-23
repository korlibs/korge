package com.soywiz.korge.gradle.targets.desktop

import com.soywiz.korge.gradle.kotlin
import com.soywiz.korge.gradle.targets.*
import com.soywiz.korge.gradle.targets.native.*
import com.soywiz.korge.gradle.targets.native.getCompileTask
import com.soywiz.korge.gradle.util.*
import com.soywiz.korlibs.*
import org.gradle.api.*
import org.gradle.api.internal.plugins.*
import org.gradle.api.reporting.*
import org.gradle.api.tasks.*
import org.gradle.testing.base.plugins.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.tasks.*

fun Project.configureNativeDesktopCross() {
    if (com.soywiz.korge.gradle.targets.isWindows) return

    for (target in convertNamesToNativeTargets(DESKTOP_NATIVE_CROSS_TARGETS)) {
        target.apply {
            configureKotlinNativeTarget(project)
            binaries {
                executable {}
            }
        }
        val mainCompilation = target.compilations.main
        for (type in NativeBuildTypes.TYPES) {
            mainCompilation.getCompileTask(NativeOutputKind.EXECUTABLE, type, project).dependsOn(prepareKotlinNativeBootstrap)
        }
        mainCompilation.defaultSourceSet.kotlin.srcDir(project.file("build/platforms/native-desktop/"))
        createCopyToExecutableTarget(target.name)
    }

    //println("!!!!!!!!!configured-cross-native-targets = CrossExecType.VALID_LIST=${CrossExecType.VALID_LIST}")
    afterEvaluate {
        for (type in CrossExecType.VALID_LIST) {
            for (buildType in NativeBuildTypes.TYPES) {
                val deb = buildType.nameType
                val linkTaskName = "link${deb}Executable${type.nameWithArchCapital}"
                val linkTask = project.tasks.findByName(linkTaskName) as? KotlinNativeLink?
                //println("!!!!!!!!!linkTaskName=$linkTaskName :: $linkTask")
                linkTask ?: continue
                project.tasks.createThis<Exec>("runNative${deb}${type.interpCapital}") {
                    group = "run"
                    dependsOn(linkTask)
                    val result = commandLineCross(linkTask.binary.outputFile.absolutePath, type = type)
                    doFirst {
                        result.ensure()
                    }
                    this.environment("WINEDEBUG", "-all")
                    workingDir = linkTask.binary.outputDirectory
                }
            }

            val linkDebugTest = project.tasks.findByName("linkDebugTest${type.nameWithArchCapital}") as? KotlinNativeLink?
            if (linkDebugTest != null) {
                project.tasks.createThis<KotlinNativeCrossTest>("${type.nameWithArch}Test${type.interpCapital}") {
                    val link = linkDebugTest
                    val testResultsDir = project.buildDir.resolve(TestingBasePlugin.TEST_RESULTS_DIR_NAME)
                    val testReportsDir = project.extensions.getByType(ReportingExtension::class.java).baseDir.resolve(TestingBasePlugin.TESTS_DIR_NAME)
                    //this.configureConventions()

                    val htmlReport = DslObject(reports.html)
                    val xmlReport = DslObject(reports.junitXml)
                    xmlReport.conventionMapping.map("destination") { testResultsDir.resolve(name) }
                    htmlReport.conventionMapping.map("destination") { testReportsDir.resolve(name) }

                    this.type = type
                    this.executable = link.binary.outputFile
                    this.workingDir = link.binary.outputDirectory.absolutePath
                    this.binaryResultsDirectory.set(testResultsDir.resolve("$name/binary"))
                    this.environment("WINEDEBUG", "-all")
                    group = "verification"
                    dependsOn(link)
                }
            }
        }
    }
}

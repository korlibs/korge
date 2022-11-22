package com.soywiz.korge.gradle.targets.desktop

import com.soywiz.korge.gradle.gkotlin
import com.soywiz.korge.gradle.kotlin
import com.soywiz.korge.gradle.util.get
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.extra
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import java.io.File
import com.soywiz.korge.gradle.targets.native.NativeBuildTypes
import com.soywiz.korge.gradle.targets.CrossExecType
import com.soywiz.korge.gradle.targets.native.getCompileTask
import com.soywiz.korge.gradle.targets.native.*
import org.gradle.api.Action
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.reporting.ReportingExtension
import org.gradle.testing.base.plugins.TestingBasePlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinNativeTargetPreset

fun Project.configureNativeDesktopCross() {
    if (com.soywiz.korge.gradle.targets.isWindows) return

    for (target in convertNamesToNativeTargets(DESKTOP_NATIVE_CROSS_TARGETS)) {
        target.apply {
            configureKotlinNativeTarget(project)
            binaries {
                executable {}
            }
        }
        val mainCompilation = target.compilations["main"]
        for (type in NativeBuildTypes.TYPES) {
            mainCompilation.getCompileTask(NativeOutputKind.EXECUTABLE, type, project).dependsOn(prepareKotlinNativeBootstrap)
        }
        mainCompilation.defaultSourceSet.kotlin.srcDir(project.file("build/platforms/native-desktop/"))
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
                project.tasks.create("runNative${deb}${type.interpCapital}", Exec::class.java) {
                    group = "run"
                    dependsOn(linkTask)
                    commandLineCross(linkTask.binary.outputFile.absolutePath, type = type)
                    this.environment("WINEDEBUG", "-all")
                    workingDir = linkTask.binary.outputDirectory
                }
            }

            val linkDebugTest = project.tasks.findByName("linkDebugTest${type.nameWithArchCapital}") as? KotlinNativeLink?
            if (linkDebugTest != null) {
                project.tasks.create("${type.nameWithArch}Test${type.interpCapital}", KotlinNativeCrossTest::class.java, Action {
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
                })
            }
        }
    }
}

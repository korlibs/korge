package com.soywiz.korge.gradle.targets.js

import com.soywiz.korge.gradle.*
import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import java.io.*

fun Project.configureEsbuild() {
    val wwwFolder = File(buildDir, "www")
    val esbuildFolder = File(rootProject.buildDir, "esbuild")
    val isWindows = org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS)
    val esbuildCmd = if (isWindows) File(esbuildFolder, "esbuild.cmd") else File(esbuildFolder, "bin/esbuild")

    val npmInstallEsbuild = "npmInstallEsbuild"
    if (rootProject.tasks.findByName(npmInstallEsbuild) == null) {
        rootProject.tasks.create(npmInstallEsbuild, Exec::class) { task ->
            task.dependsOn("kotlinNodeJsSetup")
            task.onlyIf { !esbuildCmd.exists() }

            val esbuildVersion = korge.esbuildVersion
            task.doFirst {
                val env = org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.apply(project.rootProject).requireConfigured()
                val npmCmd = arrayOf(
                    File(env.nodeExecutable),
                    File(env.nodeDir, "lib/node_modules/npm/bin/npm-cli.js").takeIf { it.exists() }
                        ?: File(env.nodeDir, "node_modules/npm/bin/npm-cli.js").takeIf { it.exists() }
                        ?: error("Can't find npm-cli.js in ${env.nodeDir} standard folders")
                )
                task.commandLine(*npmCmd, "-g", "install", "esbuild@$esbuildVersion", "--prefix", esbuildFolder)
            }
        }
    }

    val browserEsbuildResources by tasks.creating(Copy::class) {
        from(project.tasks.getByName("jsProcessResources").outputs.files)
        //for (sourceSet in gkotlin.js().compilations.flatMap { it.kotlinSourceSets }) from(sourceSet.resources)
        into(wwwFolder)
    }

    val browserPrepareEsbuildPrepare by tasks.creating(Task::class) {
        dependsOn(browserEsbuildResources)
        dependsOn("::npmInstallEsbuild")
    }

    val browserPrepareEsbuildDebug by tasks.creating(Task::class) {
        dependsOn("compileDevelopmentExecutableKotlinJs")
        dependsOn(browserPrepareEsbuildPrepare)
    }

    val browserPrepareEsbuildRelease by tasks.creating(Task::class) {
        dependsOn("compileProductionExecutableKotlinJs")
        dependsOn(browserPrepareEsbuildPrepare)
    }

    for (debug in listOf(false, true)) {
        val debugPrefix = if (debug) "Debug" else "Release"
        val productionInfix = if (debug) "Development" else "Production"
        val browserPrepareEsbuild = when {
            debug -> browserPrepareEsbuildDebug
            else -> browserPrepareEsbuildRelease
        }

        //for (run in listOf(false, true)) {
        for (run in listOf(false)) {
            val runSuffix = if (run) "Run" else ""

            // browserDebugEsbuild
            // browserReleaseEsbuild
            tasks.create("browser${debugPrefix}Esbuild${runSuffix}", Exec::class) { task ->
                task.group = "kotlin browser"
                task.dependsOn(browserPrepareEsbuild)

                val jsPath = tasks.getByName("compile${productionInfix}ExecutableKotlinJs").outputs.files.first {
                    it.extension.toLowerCase() == "js"
                }

                val output = File(wwwFolder, "${project.name}.js")
                task.inputs.file(jsPath)
                task.outputs.file(output)
                task.commandLine(ArrayList<Any>().apply {
                    add(esbuildCmd)
                    //add("--watch",)
                    add("--bundle")
                    add("--minify")
                    add("--sourcemap=external")
                    add(jsPath)
                    add("--outfile=$output")
                    // @TODO: Close this command on CTRL+C
                    //if (run) add("--servedir=$wwwFolder")
                })
            }
        }
    }


}

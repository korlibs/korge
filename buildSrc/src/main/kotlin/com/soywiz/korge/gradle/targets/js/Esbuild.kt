package com.soywiz.korge.gradle.targets.js

import com.soywiz.korge.gradle.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import java.io.*

fun Project.configureEsbuild() {
    val userGradleFolder = File(System.getProperty("user.home"), ".gradle")

    val wwwFolder = File(buildDir, "www")

    val esbuildFolder = File(if (userGradleFolder.isDirectory) userGradleFolder else rootProject.buildDir, "esbuild")
    val isWindows = org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS)
    val esbuildCmdUnix = File(esbuildFolder, "bin/esbuild")
    val esbuildCmdCheck = if (isWindows) File(esbuildFolder, "esbuild.cmd") else esbuildCmdUnix
    val esbuildCmd = if (isWindows) File(esbuildFolder, "node_modules/esbuild/esbuild.exe") else esbuildCmdUnix

    val npmInstallEsbuild = "npmInstallEsbuild"

    val env by lazy { org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.apply(project.rootProject).requireConfigured() }
    val ENV_PATH by lazy {
        val NODE_PATH = File(env.nodeExecutable).parent
        val PATH_SEPARATOR = File.pathSeparator
        val OLD_PATH = System.getenv("PATH")
        "$NODE_PATH$PATH_SEPARATOR$OLD_PATH"
    }

    if (rootProject.tasks.findByName(npmInstallEsbuild) == null) {
        rootProject.tasks.create(npmInstallEsbuild, Exec::class) {
            dependsOn("kotlinNodeJsSetup")
            onlyIf { !esbuildCmdCheck.exists() && !esbuildCmd.exists() }

            val esbuildVersion = korge.esbuildVersion
            doFirst {
                val npmCmd = arrayOf(
                    File(env.nodeExecutable),
                    File(env.nodeDir, "lib/node_modules/npm/bin/npm-cli.js").takeIf { it.exists() }
                        ?: File(env.nodeDir, "node_modules/npm/bin/npm-cli.js").takeIf { it.exists() }
                        ?: error("Can't find npm-cli.js in ${env.nodeDir} standard folders")
                )

                environment("PATH", ENV_PATH)
                commandLine(*npmCmd, "-g", "install", "esbuild@$esbuildVersion", "--prefix", esbuildFolder, "--scripts-prepend-node-path", "true")
            }
        }
    }

    val browserEsbuildResources by tasks.creating(Copy::class) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(project.tasks.getByName("jsProcessResources").outputs.files)
        afterEvaluate {
            project.tasks.findByName("korgeProcessedResourcesJsMain")?.outputs?.files?.let {
                from(it)
            }
        }
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
            tasks.create("browser${debugPrefix}Esbuild${runSuffix}", Exec::class) {
                group = "kotlin browser"
                dependsOn(browserPrepareEsbuild)

                val jsPath = tasks.getByName("compile${productionInfix}ExecutableKotlinJs").outputs.files.first {
                    it.extension.toLowerCase() == "js"
                }

                val output = File(wwwFolder, "${project.name}.js")
                inputs.file(jsPath)
                outputs.file(output)
                environment("PATH", ENV_PATH)
                commandLine(ArrayList<Any>().apply {
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

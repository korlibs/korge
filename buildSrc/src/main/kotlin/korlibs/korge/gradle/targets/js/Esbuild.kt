package korlibs.korge.gradle.targets.js

import korlibs.korge.gradle.*
import korlibs.korge.gradle.util.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.targets.js.ir.*
import java.io.*

fun Project.configureEsbuild() {
    try {
        configureErrorableEsbuild()
    } catch (e: Throwable) {
        e.printStackTrace()
    }
}

fun Project.configureErrorableEsbuild() {
    val userGradleFolder = File(System.getProperty("user.home"), ".gradle")

    val wwwFolder = File(buildDir, "www")

    val esbuildFolder = File(if (userGradleFolder.isDirectory) userGradleFolder else rootProject.buildDir, "esbuild")
    val isWindows = org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS)
    val esbuildCommand = File(esbuildFolder, if (isWindows) "esbuild.cmd" else "bin/esbuild")
    val esbuildCmd = if (isWindows) listOf("cmd.exe", "/c", esbuildCommand) else listOf(esbuildCommand)

    val npmInstallEsbuildTaskName = "npmInstallEsbuild"

    val env by lazy { org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.apply(project.rootProject).requireConfigured() }
    val ENV_PATH by lazy {
        val NODE_PATH = File(env.nodeExecutable).parent
        val PATH_SEPARATOR = File.pathSeparator
        val OLD_PATH = System.getenv("PATH")
        "$NODE_PATH$PATH_SEPARATOR$OLD_PATH"
    }

    val npmInstallEsbuild = rootProject.tasks.findByName(npmInstallEsbuildTaskName) ?: rootProject.tasks.createThis<Exec>(npmInstallEsbuildTaskName) {
        dependsOn("kotlinNodeJsSetup")
        onlyIf { !esbuildCommand.exists() }

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

    val browserEsbuildResources = tasks.createThis<Copy>("browserEsbuildResources") {
        val korgeProcessResourcesTaskName = getKorgeProcessResourcesTaskName("js", "main")
        dependsOn(korgeProcessResourcesTaskName)

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(project.tasks.getByName("jsProcessResources").outputs.files)
        //afterEvaluate {
        //    project.tasks.findByName(getKorgeProcessResourcesTaskName("js", "main"))?.outputs?.files?.let {
        //        from(it)
        //    }
        //}
        //for (sourceSet in gkotlin.js().compilations.flatMap { it.kotlinSourceSets }) from(sourceSet.resources)
        into(wwwFolder)
        //afterEvaluate {
        //    afterEvaluate {
        //        val korgeGeneratedTask = project.tasks.findByName(korgeProcessResourcesTaskName) as? KorgeGenerateResourcesTask?
        //        println("korgeGeneratedTaskName=$korgeGeneratedTask : korgeProcessResourcesTaskName=$korgeProcessResourcesTaskName")
        //        korgeGeneratedTask?.addExcludeToCopyTask(this)
        //    }
        //}
    }

    val browserPrepareEsbuildPrepare = tasks.createThis<Task>("browserPrepareEsbuildPrepare") {
        dependsOn(browserEsbuildResources)
        dependsOn(npmInstallEsbuild)
    }

    val browserPrepareEsbuildDebug = tasks.createThis<Task>("browserPrepareEsbuildDebug") {
        dependsOn("compileDevelopmentExecutableKotlinJs")
        dependsOn(browserPrepareEsbuildPrepare)
    }

    val browserPrepareEsbuildRelease = tasks.createThis<Task>("browserPrepareEsbuildRelease") {
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

        // browserDebugEsbuild
        // browserReleaseEsbuild
        tasks.createThis<Exec>("browser${debugPrefix}Esbuild") {
            group = "kotlin browser"
            val compileExecutableKotlinJs = tasks.getByName("compile${productionInfix}ExecutableKotlinJs") as KotlinJsIrLink
            dependsOn(browserPrepareEsbuild)
            dependsOn(compileExecutableKotlinJs)

            //println("compileExecutableKotlinJs:" + compileExecutableKotlinJs::class)
            val jsPath = compileExecutableKotlinJs.outputFileProperty.get()
            val output = File(wwwFolder, "${project.name}.js")
            //println("jsPath=$jsPath")
            //println("jsPath.parentFile=${jsPath.parentFile}")
            //println("outputs=${compileExecutableKotlinJs.outputs.files.toList()}")
            inputs.files(compileExecutableKotlinJs.outputs.files)
            outputs.file(output)
            environment("PATH", ENV_PATH)
            commandLine(buildList {
                addAll(esbuildCmd)
                //add("--watch",)
                add("--bundle")
                if (!debug) {
                    add("--minify")
                    add("--sourcemap=external")
                }
                add(jsPath)
                add("--outfile=$output")
                // @TODO: Close this command on CTRL+C
                //if (run) add("--servedir=$wwwFolder")
            })
        }
    }
}

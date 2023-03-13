package com.soywiz.korge.gradle.targets.jvm

import com.soywiz.korge.gradle.*
import com.soywiz.korge.gradle.kotlin
import com.soywiz.korge.gradle.targets.*
import com.soywiz.korlibs.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.jvm.tasks.*
import java.io.*

open class KorgeJavaExecWithAutoreload : KorgeJavaExec() {
    @get:Input
    var enableRedefinition: Boolean = false

    @get:Input
    var doConfigurationCache: Boolean = true

    companion object {
        const val ARGS_SEPARATOR = "<:/:>"
        const val CMD_SEPARATOR = "<@/@>"
    }

    private var projectPath: String = project.path
    private var rootDir: File = project.rootProject.rootDir
    @get:InputFiles
    lateinit var rootJars: FileCollection
    @get:InputFile
    //private var reloadAgentConfiguration: Configuration = project.configurations.getByName(KORGE_RELOAD_AGENT_CONFIGURATION_NAME)//.resolve().first()
    lateinit var reloadAgentJar: File

    init {
        //val reloadAgent = project.findProject(":korge-reload-agent")
        //if (reloadAgent != null)
        rootJars = (project.tasks.findByName("compileKotlinJvm") as org.jetbrains.kotlin.gradle.tasks.KotlinCompile).outputs.files
        val reloadAgent = project.findProject(":korge-reload-agent")
        reloadAgentJar = when {
            reloadAgent != null -> (project.rootProject.tasks.getByPath(":korge-reload-agent:jar") as Jar).outputs.files.files.first()
            else -> project.configurations.getByName(KORGE_RELOAD_AGENT_CONFIGURATION_NAME).resolve().first()
        }
    }

    override fun autoconfigure() {
        //super.autoconfigure()
        ////println("---------------")
        ////println((project.tasks.findByName("compileKotlinJvm") as org.jetbrains.kotlin.gradle.tasks.KotlinCompile).outputs.files.toList())
//
        //project.afterEvaluate {
        //    //println("++++++++++++++")
        //    rootJars = (project.tasks.findByName("compileKotlinJvm") as org.jetbrains.kotlin.gradle.tasks.KotlinCompile).outputs.files
        //    val reloadAgent = project.findProject(":korge-reload-agent")
        //    reloadAgentJar = when {
        //        reloadAgent != null -> (project.rootProject.tasks.getByPath(":korge-reload-agent:jar") as Jar).outputs.files.files.first()
        //        else -> project.configurations.getByName(KORGE_RELOAD_AGENT_CONFIGURATION_NAME).resolve().first()
        //    }
        //}
    }

    override fun exec() {
        //val gradlewCommand = if (isWindows) "gradlew.bat" else "gradlew"

        println("runJvmAutoreload:reloadAgentJar=$reloadAgentJar")
        //val outputJar = JvmTools.findPathJar(Class.forName("com.soywiz.korge.reloadagent.KorgeReloadAgent"))

        //val agentJarTask: org.gradle.api.tasks.bundling.Jar = project(":korge-reload-agent").tasks.findByName("jar") as org.gradle.api.tasks.bundling.Jar
        //val outputJar = agentJarTask.outputs.files.files.first()
        //println("agentJarTask=$outputJar")

        jvmArgs(
            "-javaagent:$reloadAgentJar=${listOf(
                "$httpPort",
                ArrayList<String>().apply {
                    add("-classpath")
                    add("${rootDir}/gradle/wrapper/gradle-wrapper.jar")
                    add("org.gradle.wrapper.GradleWrapperMain")
                    add("--no-daemon")
                    add("--watch-fs")
                    add("--warn")
                    add("--project-dir=${rootDir}")
                    if (doConfigurationCache) {
                        add("--configuration-cache")
                        add("--configuration-cache-problems=warn")
                    }
                    add("-t")
                    add("${projectPath.trimEnd(':')}:compileKotlinJvmAndNotify")
                }.joinToString(CMD_SEPARATOR),
                "$enableRedefinition",
                rootJars.joinToString(CMD_SEPARATOR) { it.absolutePath }
            ).joinToString(ARGS_SEPARATOR)}"
        )
        environment("KORGE_AUTORELOAD", "true")

        super.exec()
    }
}

open class KorgeJavaExec : JavaExec() {
    //dependsOn(getKorgeProcessResourcesTaskName("jvm", "main"))

    @get:InputFiles
    val korgeClassPath: FileCollection = ArrayList<FileCollection>().apply {
        val mainJvmCompilation = project.mainJvmCompilation
        add(mainJvmCompilation.runtimeDependencyFiles)
        add(mainJvmCompilation.compileDependencyFiles)
        //if (project.korge.searchResourceProcessorsInMainSourceSet) {
        add(mainJvmCompilation.output.allOutputs)
        add(mainJvmCompilation.output.classesDirs)
        //}
        //project.kotlin.jvm()
        add(project.files().from(project.getCompilationKorgeProcessedResourcesFolder(mainJvmCompilation)))
        //add(project.files().from((project.tasks.findByName(jvmProcessedResourcesTaskName) as KorgeProcessedResourcesTask).processedResourcesFolder))
    }
        .reduceRight { l, r -> l + r }

    open fun autoconfigure() {

    }

    override fun exec() {
        val firstThread = firstThread
            ?: (
                System.getenv("KORGE_START_ON_FIRST_THREAD") == "true"
                    || System.getenv("KORGW_JVM_ENGINE") == "sdl"
                //|| project.findProperty("korgw.jvm.engine") == "sdl"
                )

        if (firstThread && isMacos) {
            jvmArgs("-XstartOnFirstThread")
            //println("Executed jvmArgs(\"-XstartOnFirstThread\")")
        } else {
            //println("firstThread=$firstThread, isMacos=$isMacos")
        }
        classpath = korgeClassPath
        for (classPath in classpath.toList()) {
            logger.info("- $classPath")
        }
        super.exec()
    }

    @get:Input
    @Optional
    var firstThread: Boolean? = null

    init {
        systemProperties = (System.getProperties().toMutableMap() as MutableMap<String, Any>) - "java.awt.headless"
        defaultCharacterEncoding = Charsets.UTF_8.toString()
        // https://github.com/korlibs/korge-plugins/issues/25
    }
}

/*
open class KorgeJavaExec : JavaExec() {
    private val jvmCompilation get() = project.kotlin.targets.getByName("jvm").compilations as NamedDomainObjectSet<*>
    private val mainJvmCompilation get() = jvmCompilation.getByName("main") as org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation

    @get:InputFiles
    val korgeClassPath: FileCollection = mainJvmCompilation.runtimeDependencyFiles + mainJvmCompilation.compileDependencyFiles + mainJvmCompilation.output.allOutputs + mainJvmCompilation.output.classesDirs

    override fun exec() {
        systemProperties = (System.getProperties().toMutableMap() as MutableMap<String, Any>) - "java.awt.headless"
        if (!JvmAddOpens.beforeJava9) jvmArgs(*JvmAddOpens.createAddOpensTypedArray())
        classpath = korgeClassPath
        super.exec()
        //project.afterEvaluate {
        //if (firstThread == true && OS.isMac) task.jvmArgs("-XstartOnFirstThread")
        //}
    }
}
*/

/*
open class KorgeJavaExec : JavaExec() {
    private val jvmCompilation get() = project.kotlin.targets.getByName("jvm").compilations as NamedDomainObjectSet<*>
    private val mainJvmCompilation get() = jvmCompilation.getByName("main") as org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation

    @get:InputFiles
    val korgeClassPath: FileCollection = mainJvmCompilation.runtimeDependencyFiles + mainJvmCompilation.compileDependencyFiles + mainJvmCompilation.output.allOutputs + mainJvmCompilation.output.classesDirs

    override fun exec() {
        systemProperties = (System.getProperties().toMutableMap() as MutableMap<String, Any>) - "java.awt.headless"
        if (!JvmAddOpens.beforeJava9) jvmArgs(*JvmAddOpens.createAddOpensTypedArray())
        classpath = korgeClassPath
        super.exec()
        //project.afterEvaluate {
        //if (firstThread == true && OS.isMac) task.jvmArgs("-XstartOnFirstThread")
        //}
    }
}
*/

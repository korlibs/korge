package korlibs.korge.gradle.targets.jvm

import java.io.File
import korlibs.korge.gradle.getCompilationKorgeProcessedResourcesFolder
import korlibs.korge.gradle.targets.isMacos
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.jvm.tasks.Jar
import org.gradle.work.DisableCachingByDefault

fun Project.findAllProjectDependencies(visited: MutableSet<Project> = mutableSetOf()): Set<Project> {
    if (this in visited) return visited
    visited.add(this)
    val dependencies = project.configurations.flatMap { it.dependencies.withType(ProjectDependency::class.java) }
    return (dependencies.flatMap { project.rootProject.project(it.path).findAllProjectDependencies(visited) } + this).toSet()
}

@DisableCachingByDefault
abstract class KorgeJavaExecWithAutoreload : KorgeJavaExec() {
    @get:Input
    var enableRedefinition: Boolean = false

    @get:Input
    var doConfigurationCache: Boolean = true

    companion object {
        const val ARGS_SEPARATOR = "<:/:>"
        const val CMD_SEPARATOR = "<@/@>"
    }

    private lateinit var projectPaths: List<String>
    private var rootDir: File = project.rootProject.rootDir
    @get:InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    lateinit var rootJars: List<File>

    init {
        doFirst {
            val reloadAgent = project.findProject(":korge-reload-agent")
            val reloadAgentJar = when {
                reloadAgent != null -> (project.rootProject.tasks.getByPath(":korge-reload-agent:jar") as Jar).outputs.files.files.first()
                else -> project.configurations.getByName(KORGE_RELOAD_AGENT_CONFIGURATION_NAME).resolve().first()
            }

            val allProjects = project.findAllProjectDependencies()
            projectPaths = listOf(project.path)
            rootJars = allProjects.map {
                it.layout.buildDirectory.file("classes/kotlin/jvm/main").get().asFile
            }

            jvmArgs(
                "-javaagent:$reloadAgentJar=${listOf(
                    "$httpPort",
                    ArrayList<String>().apply {
                        add("-classpath")
                        add("${rootDir}/gradle/wrapper/gradle-wrapper.jar")
                        add("org.gradle.wrapper.GradleWrapperMain")
                        //add("--no-daemon") // This causes: Continuous build does not work when file system watching is disabled
                        add("--watch-fs")
                        add("--warn")
                        add("--project-dir=${rootDir}")
                        if (doConfigurationCache) {
                            add("--configuration-cache")
                            add("--configuration-cache-problems=warn")
                        }
                        add("-t")
                        add("compileKotlinJvm")
                        //add("compileKotlinJvmAndNotify")
                        for (projectPath in projectPaths) {
                            add("${projectPath.trimEnd(':')}:compileKotlinJvmAndNotify")
                        }
                    }.joinToString(CMD_SEPARATOR),
                    "$enableRedefinition",
                    rootJars.joinToString(CMD_SEPARATOR) { it.absolutePath }
                ).joinToString(ARGS_SEPARATOR)}"
            )

            environment("KORGE_AUTORELOAD", "true")
            project.findProperty("korge.ipc")?.toString()?.let { environment("KORGE_IPC", it) }
            project.findProperty("korge.headless")?.toString()?.let { environment("KORGE_HEADLESS", it) }
        }
    }
}

fun Project.getKorgeClassPath(): FileCollection {
    return ArrayList<FileCollection>().apply {
        val mainJvmCompilation = project.mainJvmCompilation
        add(mainJvmCompilation.runtimeDependencyFiles)
        add(mainJvmCompilation.compileDependencyFiles)
        add(mainJvmCompilation.output.allOutputs)
        add(mainJvmCompilation.output.classesDirs)
        add(project.files().from(project.getCompilationKorgeProcessedResourcesFolder(mainJvmCompilation)))
    }
        .reduceRight { l, r -> l + r }
}

@DisableCachingByDefault
abstract class KorgeJavaExec : JavaExec() {
    @get:Input
    var logLevel = "info"

    @get:InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    val korgeClassPath: FileCollection = project.getKorgeClassPath()

    @get:Input
    @Optional
    var firstThread: Boolean? = null

    init {
        systemProperties = (System.getProperties().toMutableMap() as MutableMap<String, Any>) - "java.awt.headless"
        defaultCharacterEncoding = Charsets.UTF_8.toString()

        classpath = korgeClassPath

        doFirst {
            val firstThread = firstThread
                ?: (
                    System.getenv("KORGE_START_ON_FIRST_THREAD") == "true"
                        || System.getenv("KORGW_JVM_ENGINE") == "sdl"
                    )
            if (javaVersion.isCompatibleWith(JavaVersion.VERSION_17)) {
                jvmArgs("-XX:+UnlockExperimentalVMOptions", "-XX:+IgnoreUnrecognizedVMOptions", "-XX:+UseZGC", "-XX:+ZGenerational")
            }
            if (firstThread && isMacos) {
                jvmArgs("-XstartOnFirstThread")
            }

            for (classPath in classpath.files) {
                logger.info("- $classPath")
            }

            environment("LOG_LEVEL", logLevel)
        }
    }
}

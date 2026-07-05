package korlibs.korge.gradle

import java.io.File
import java.net.URI
import java.net.URL
import java.net.URLClassLoader
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import korlibs.downloadFile
import korlibs.invoke
import korlibs.korge.gradle.targets.ProjectType
import korlibs.korge.gradle.targets.all.enableFeaturesOnAllTargets
import korlibs.korge.gradle.targets.isLinux
import korlibs.korge.gradle.targets.linux.LDLibraries
import korlibs.korge.gradle.typedresources.configureTypedResourcesGenerator
import korlibs.korge.gradle.util.AnsiEscape
import korlibs.korge.gradle.util.Json
import korlibs.korge.gradle.util.checkGradleVersion
import korlibs.korge.gradle.util.checkMinimumJavaVersion
import korlibs.korge.gradle.util.createThis
import korlibs.korge.gradle.util.dyn
import korlibs.korge.gradle.util.projectExtension
import korlibs.modules.configureTests
import kotlin.concurrent.thread
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.tasks.diagnostics.DependencyReportTask
import org.gradle.internal.classloader.ClassLoaderHierarchy
import org.gradle.internal.classloader.ClassLoaderVisitor
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.korge.gradle.BuildVersions

class KorgeGradleApply(val project: Project, val projectType: ProjectType) {
    fun apply(includeIndirectAndroid: Boolean = true) = project {
        checkMinimumJavaVersion()
        checkGradleVersion()

        if (isLinux) {
            project.logger.info("LD folders: ${LDLibraries.ldFolders}")
            for (lib in listOf("libGL.so.1")) {
                if (!LDLibraries.hasLibrary(lib)) {
                    System.err.println("Can't find $lib. Please: sudo apt-get -y install freeglut3")
                }
            }
        }

        KorgeVersionsTask.registerShowKorgeVersions(project)

        project.korge.init(includeIndirectAndroid, projectType)

        project.configureRepositories()

        korge.targetJvm()

        project.afterEvaluate {
            project.addGenResourcesTasks()
            project.enableFeaturesOnAllTargets()

            project.configureTests()
        }

        project.configureTypedResourcesGenerator()
    }
}

fun Project.configureBuildScriptClasspathTasks() {
    // https://gist.github.com/xconnecting/4037220
    project.tasks.createThis<DependencyReportTask>("printBuildScriptClasspath") {
        configurations = project.buildscript.configurations
    }
    project.tasks.createThis<Task>("printBuildScriptClasspath2") {
        doFirst {
            fun getClassLoaderChain(
                classLoader: ClassLoader,
                out: ArrayList<ClassLoader> = arrayListOf()
            ): List<ClassLoader> {
                var current: ClassLoader? = classLoader
                while (current != null) {
                    out.add(current)
                    current = current.parent
                }
                return out
            }

            fun printClassLoader(classLoader: ClassLoader) {
                when (classLoader) {
                    is URLClassLoader -> {
                        println(classLoader.urLs.joinToString("\n"))
                    }

                    is ClassLoaderHierarchy -> {
                        classLoader.visit(object : ClassLoaderVisitor() {
                            override fun visitClassPath(classPath: Array<out URL>) {
                                classPath.forEach { println(it) }
                            }
                        })
                    }
                }
            }

            println("Class loaders:")
            val classLoaders = getClassLoaderChain(Thread.currentThread().contextClassLoader)
            for (classLoader in classLoaders.reversed()) {
                println(" - $classLoader")
            }

            for (classLoader in classLoaders.reversed()) {
                println("")
                println("$classLoader:")
                println("--------------")
                printClassLoader(classLoader)
            }
        }
    }
}

val Project.gkotlin get() = properties["kotlin"] as KotlinMultiplatformExtension
val Project.ext get() = extensions.getByType(ExtraPropertiesExtension::class.java)

fun Project.korge(callback: KorgeExtension.() -> Unit) = korge.apply(callback).also { it.finish() }
val Project.kotlin: KotlinMultiplatformExtension
    get() = this.extensions.getByType(
        KotlinMultiplatformExtension::class.java
    )
val Project.korge: KorgeExtension get() = extensionGetOrCreate("korge")

inline fun <reified T : Any> Project.extensionGetOrCreate(name: String): T {
    val extension = project.extensions.findByName(name) as? T?
    return if (extension == null) {
        val newExtension = project.extensions.create(name, T::class.java)
        newExtension
    } else {
        extension
    }
}

val korgeCacheDir: File
    get() = File(
        System.getProperty("user.home"),
        ".korge"
    ).apply { if (!this.isDirectory) mkdirs() }

var Project.korgeCacheData: ConcurrentHashMap<String, String> by projectExtension { ConcurrentHashMap<String, String>() }
var Project.korgeCacheDir: File by projectExtension {
    File(
        System.getProperty("user.home"),
        ".korge"
    ).apply { if (!this.isDirectory) mkdirs() }
}

val Project.korgeInstallUUID: String
    get() {
        return korgeCacheData.getOrPut("korgeInstallUUID") {
            val uuidFile = File(korgeCacheDir, "install-uuid")
            if (!uuidFile.exists()) {
                uuidFile.writeText(
                    UUID.randomUUID().toString().replace('7', '1').replace('3', '9')
                        .replace('a', 'f')
                )
            }
            uuidFile.readText()
        }
    }

fun Project.korgeVersionJson(telemetry: Boolean): String {
    val defaultJson = """{"version": "${BuildVersions.KORGE}", "motd": "Fallback"}"""
    return korgeCacheData.getOrPut("korgeVersionJson") {
        val versionJsonFile = File(korgeCacheDir, "version.json")
        if (!versionJsonFile.isFile && System.currentTimeMillis() - versionJsonFile.lastModified() >= 24 * 3600 * 1000L) {
            val base = "https://version.korge.org/version.json?source=gradle"
            val props: Map<String, String> = mapOf(
                "version" to BuildVersions.KORGE,
                "install.uuid" to korgeInstallUUID,
                "ci" to (System.getenv("CI") == "true").toString(),
                "os.name" to System.getProperty("os.name"),
                "os.arch" to System.getProperty("os.arch"),
                "os.version" to System.getProperty("os.version"),
            )

            fun Map<String, String>.toQueryString(): String {
                return this.map {
                    URLEncoder.encode(it.key, Charsets.UTF_8) + "=" + URLEncoder.encode(
                        it.value,
                        Charsets.UTF_8
                    )
                }.joinToString("&")
            }
            try {
                downloadFile(
                    URI.create(
                        when (telemetry) {
                            true -> "$base&${props.toQueryString()}"
                            else -> base
                        }
                    ).toURL(),
                    versionJsonFile,
                    connectionTimeout = 5_000,
                    readTimeout = 3_000,
                )
            } catch (e: Throwable) {
                logger.info(e.stackTraceToString())
                versionJsonFile.writeText(defaultJson)
            }
        }
        try {
            versionJsonFile.readText()
        } catch (_: Throwable) {
            defaultJson
        }
    }
}

fun Project.korgeCheckVersion(report: Boolean = true, telemetry: Boolean = true): Thread {
    return thread(start = true, isDaemon = false) {
        try {
            val versionJson = Json.parse(korgeVersionJson(telemetry = telemetry)).dyn
            val latestVersion = versionJson["version"].str
            val motd = versionJson["motd"].str

            if (report && latestVersion != BuildVersions.KORGE) {
                logger.warn(AnsiEscape {
                    listOf(
                        "You are using KorGE '${BuildVersions.KORGE}', but there is a new version available '$latestVersion' : $motd".yellow.bgGreen,
                        "- You can change your KorGE version typically in the file `gradle/libs.versions.toml` or in your `build.gradle.kts`".yellow,
                        "- You can disable this notice by changing `korge { checkVersion(report = false) }` in your `build.gradle.kts`".yellow,
                    ).joinToString("\n")
                })
                //}
            }
        } catch (e: Throwable) {
            logger.info(e.stackTraceToString())
        }
    }
}

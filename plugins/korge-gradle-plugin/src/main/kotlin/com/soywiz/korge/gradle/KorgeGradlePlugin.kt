package com.soywiz.korge.gradle

import groovy.text.*
import groovy.util.*
import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.file.*
import org.gradle.api.plugins.*
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.tasks.*
import java.io.*
import java.net.*

val Project.gkotlin get() = properties["kotlin"] as KotlinMultiplatformExtension
val Project.ext get() = extensions.getByType(ExtraPropertiesExtension::class.java)
val korgeVersion get() = KorgeBuildServiceProxy.version()

enum class Orientation(val lc: String) { DEFAULT("default"), LANDSCAPE("landscape"), PORTRAIT("portrait") }

data class KorgePluginDescriptor(val name: String, val args: Map<String, String>, val version: String?)

@Suppress("unused")
class KorgeExtension(val project: Project) {
    internal fun init() {
        // Do nothing, but serves to be referenced to be installed
    }

    var id: String = "com.unknown.unknownapp"
    var version: String = "0.0.1"

    var name: String = "unnamed"
    var description: String = "undescripted"
    var orientation: Orientation = Orientation.DEFAULT
    val plugins = arrayListOf<KorgePluginDescriptor>()

    var authorName = "unknown"
    var authorEmail = "unknown@unknown"
    var authorHref = "http://localhost"

    var fullscreen = true

    @JvmOverloads
    fun cordovaPlugin(name: CharSequence, args: Map<String, String> = mapOf(), version: CharSequence? = null) {
        plugins += KorgePluginDescriptor(name.toString(), args, version?.toString())
        //println("cordovaPlugin($name, $args, $version)")
    }

    @JvmOverloads
    fun author(name: String, email: String, href: String) {
        authorName = name
        authorEmail = email
        authorHref = href
    }
}

val Project.korge: KorgeExtension get() {
    val extension = project.extensions.findByName("korge") as? KorgeExtension?
    return if (extension == null) {
        val newExtension = KorgeExtension(this)
        project.extensions.add("korge", newExtension)
        newExtension
    } else {
        extension
    }
}

open class KorgeGradlePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        System.setProperty("java.awt.headless", "true")

        KorgeBuildServiceProxy.init()
        project.addVersionExtension()
        project.configureRepositories()
        project.configureKotlin()
        project.addKorgeTasks()

        project.korge.init()

        //for (res in project.getResourcesFolders()) println("- $res")
    }

    private fun Project.addVersionExtension() {
        ext.set("korgeVersion", korgeVersion)
        ext.set("kotlinVersion", "1.3.11")
        //ext.set("kotlinVersion", KotlinVersion.CURRENT.toString())
    }

    private fun Project.configureRepositories() {
        repositories.apply {
            mavenLocal()
            maven { it.url = URI("https://dl.bintray.com/soywiz/soywiz") }
            jcenter()
            mavenCentral()
        }
    }

    fun <T> Project.closure(callback: () -> T) = GroovyClosure(this, callback)

    private fun Project.configureKotlin() {
        plugins.apply("kotlin-multiplatform")

        for (preset in listOf("macosX64", "mingwX64")) {
            gkotlin.targets.add((gkotlin.presets.getAt(preset) as KotlinNativeTargetPreset).createTarget(preset).apply {
                compilations["main"].outputKinds("EXECUTABLE")
            })
        }

        gkotlin.targets.add((gkotlin.presets.getAt("jvm") as KotlinJvmTargetPreset).createTarget("jvm"))
        gkotlin.targets.add((gkotlin.presets.getAt("js") as KotlinJsTargetPreset).createTarget("js").apply {
            compilations.getAt("main").apply {
                for (task in listOf("compileKotlinJs", "compileTestKotlinJs")) {
                    (project[task] as Kotlin2JsCompile).apply {
                        kotlinOptions.apply {
                            languageVersion = "1.3"
                            sourceMap = true
                            metaInfo = true
                            moduleKind = "umd"
                        }
                    }
                }
            }
        })


        project.dependencies.add("commonMainImplementation", "org.jetbrains.kotlin:kotlin-stdlib-common")
        project.dependencies.add("commonTestImplementation", "org.jetbrains.kotlin:kotlin-test-annotations-common")
        project.dependencies.add("commonTestImplementation", "org.jetbrains.kotlin:kotlin-test-common")

        //println("com.soywiz:korge:$korgeVersion")
        //project.dependencies.add("commonMainImplementation", "com.soywiz:korge:$korgeVersion")

        gkotlin.sourceSets.maybeCreate("commonMain").dependencies {
            api("com.soywiz:korge:$korgeVersion")
        }

        //kotlin.sourceSets.create("")

    }

    fun Project.addKorgeTasks() {
        run {
            try {
                project.dependencies.add("compile", "com.soywiz:korge:$korgeVersion")
            } catch (e: UnknownConfigurationException) {
                //logger.error("KORGE: " + e.message)
            }
        }

        run {
            project.addTask<KorgeResourcesTask>(
                    "genResources", group = "korge", description = "process resources",
                    //overwrite = true, dependsOn = listOf("build")
                    overwrite = true, dependsOn = listOf()
            ) {
                it.debug = true
            }
            try {
                project.tasks["processResources"].dependsOn("genResources")
            } catch (e: UnknownTaskException) {
            }
        }

        run {
            project.addTask<KorgeTestResourcesTask>(
                    "genTestResources", group = "korge", description = "process test resources",
                    //overwrite = true, dependsOn = listOf("build")
                    overwrite = true, dependsOn = listOf()
            ) {
                it.debug = true
            }
            try {
                project.tasks["processTestResources"].dependsOn("genTestResources")
            } catch (e: UnknownTaskException) {
            }
        }

        run {
            // Provide default mainClassName
            if (!project.ext.has("mainClassName")) {
                project.ext.set("mainClassName", "")
            }

            // packageJvmFatJar
            project.addTask<org.gradle.jvm.tasks.Jar>("packageJvmFatJar", group = "korge") { task ->
                project.afterEvaluate {
                    task.manifest { manifest ->
                        manifest.attributes(mapOf(
                                "Implementation-Title" to project.ext.get("mainClassName"),
                                "Implementation-Version" to project.version.toString(),
                                "Main-Class" to project.ext.get("mainClassName")
                        ))
                    }
                    task.baseName = "${project.name}-all"
                    //it.from()
                    //fileTree()
                    task.from(GroovyClosure(project) {
                        (project["kotlin"]["targets"]["jvm"]["compilations"]["main"]["runtimeDependencyFiles"] as FileCollection).map { if (it.isDirectory) it else project.zipTree(it) as Any }
                        //listOf<File>()
                    })
                    task.with(project.getTasksByName("jvmJar", true).first() as CopySpec)
                }
            }
        }

        val jsJar = TaskName("jsJar")

        val jsWeb = project.addTask<Copy>(name = "jsWeb", dependsOn = listOf(jsJar)) { task ->
            project.afterEvaluate {
                val kotlinTargets = project["kotlin"]["targets"]
                val jsCompilations = kotlinTargets["js"]["compilations"]
                task.includeEmptyDirs = false
                task.from("${project.buildDir}/npm/node_modules")
                task.from((jsCompilations["main"] as KotlinCompilation).output.allOutputs)
                task.exclude("**/*.kotlin_metadata", "**/*.kotlin_module", "**/*.MF", "**/*.kjsm", "**/*.map", "**/*.meta.js")
                for (file in (jsCompilations["test"]["runtimeDependencyFiles"] as FileCollection).toList()) {
                    if (file.exists() && !file.isDirectory) {
                        task.from(project.zipTree(file.absolutePath))
                    } else {
                        task.from(file)
                    }
                }
                for (target in listOf(kotlinTargets["js"], kotlinTargets["metadata"])) {
                    val main = (target["compilations"]["main"] as KotlinCompilation)
                    for (sourceSet in main.kotlinSourceSets) {
                        task.from(sourceSet.resources)
                    }
                }
                task.into("${project.buildDir}/web")
            }
            task.doLast {
                project.buildDir["web/index.html"].writeText(SimpleTemplateEngine().createTemplate(project.buildDir["web/index.template.html"].readText()).make(mapOf(
                        "OUTPUT" to project.name,
                        "TITLE" to project.name
                )).toString())
            }
        }

        val cordovaFolder = project.buildDir["cordova"]
        val cordovaConfigXmlFile = cordovaFolder["config.xml"]

        //val korge = KorgeXml(project.file("korge.project.xml"))
        val korge = project.korge

        run {
            val cordovaInstall = project.addTask<Exec>("cordovaInstall") { task ->
                task.onlyIf { !cordovaFolder.exists() }
                task.commandLine("cordova", "create", cordovaFolder.absolutePath, "com.soywiz.sample1", "sample1")
            }

            val cordovaAndroidInstall = project.addTask<Exec>("cordovaAndroidInstall", dependsOn = listOf(cordovaInstall)) { task ->
                task.onlyIf { !cordovaFolder["platforms/android"].exists() }
                task.workingDir = cordovaFolder
                task.commandLine("cordova", "platform", "add", "android")
            }

            val cordovaIosInstall = project.addTask<Exec>("cordovaIosInstall", dependsOn = listOf(cordovaInstall)) { task ->
                task.onlyIf { !cordovaFolder["platforms/ios"].exists() }
                task.workingDir = cordovaFolder
                task.commandLine("cordova", "platform", "add", "ios")
            }

            val cordovaPluginsList = project.addTask<DefaultTask>("cordovaPluginsList", dependsOn = listOf(cordovaInstall)) { task ->
                task.doLast {
                    println("name: ${korge.name}")
                    println("description: ${korge.description}")
                    println("orientation: ${korge.orientation}")
                    println("plugins: ${korge.plugins}")
                }
            }

            fun replaceCordovaPreference(cordovaConfig: QXml, name: String, value: String) {
                // Remove Orientation node and set a new node
                cordovaConfig["preference"].list.filter { it.attributes["name"] == name }.forEach { it.remove() }
                cordovaConfig.appendNode("preference", mapOf("name" to name, "value" to value))
            }

            val cordovaSynchronizeConfigXml = project.addTask<DefaultTask>("cordovaSynchronizeConfigXml", dependsOn = listOf(cordovaInstall)) { task ->
                task.doLast {
                    val cordovaConfigXml = cordovaConfigXmlFile.readText()
                    val cordovaConfig = QXml(xmlParse(cordovaConfigXml))

                    cordovaConfig["name"].setValue(korge.name)
                    cordovaConfig["description"].setValue(korge.description)

                    // https://cordova.apache.org/docs/es/latest/config_ref/
                    replaceCordovaPreference(cordovaConfig, "Orientation", korge.orientation.lc)
                    replaceCordovaPreference(cordovaConfig, "Fullscreen", "true")
                    replaceCordovaPreference(cordovaConfig, "BackgroundColor", "0xff000000")

                    //println(xmlSerialize(cordovaConfig))
                    cordovaConfigXmlFile.writeText(xmlSerialize(cordovaConfig.obj as Node))
                    //project.file("korge.project.xml").text
                }
            }

            val cordovaPluginsInstall = project.addTask<DefaultTask>("cordovaPluginsInstall", dependsOn = listOf(cordovaInstall)) { task ->
                task.doLast {
                    println("korge.plugins: ${korge.plugins}")
                    for (plugin in korge.plugins) {
                        val list = plugin.args.flatMap { listOf("--variable", "${it.key}=${it.value}") }.toTypedArray()
                        project.exec {
                            it.workingDir(cordovaFolder)
                            println(listOf("cordova", "plugin", "add", plugin.name, "--save", *list))
                            it.commandLine("cordova", "plugin", "add", plugin.name, "--save", *list)
                        }
                    }
                }
            }

            val packageJsWeb = project.addTask<Copy>("packageJsWeb", group = "korge", dependsOn = listOf(jsWeb, cordovaInstall, cordovaSynchronizeConfigXml)) { task ->
                task.from(project.buildDir["web"])
                task.into(cordovaFolder["www"])
                task.doLast {
                    val f = cordovaFolder["www/index.html"]
                    f.writeText(f.readText().replace("</head>", "<script type=\"text/javascript\" src=\"cordova.js\"></script></head>"))
                }
            }

            val runJvm = project.addTask<JavaExec>("runJvm", group = "korge") { task ->
                afterEvaluate {
                    task.classpath = project["kotlin"]["targets"]["jvm"]["compilations"]["test"]["runtimeDependencyFiles"] as? FileCollection?
                    task.main = project.ext.get("mainClassName") as? String?
                }
            }

            val runAndroid = project.addTask<Exec>("runAndroid", group = "korge", dependsOn = listOf(cordovaAndroidInstall, cordovaPluginsInstall, packageJsWeb)) { task ->
                task.workingDir = cordovaFolder
                task.commandLine("cordova", "run", "android")
            }

            val runIos = project.addTask<Exec>("runIos", group = "korge", dependsOn = listOf(cordovaIosInstall, cordovaPluginsInstall, packageJsWeb)) { task ->
                task.workingDir = cordovaFolder
                task.commandLine("cordova", "run", "ios", "--device")
            }
        }
    }
}


abstract class KorgeBaseResourcesTask : DefaultTask() {
    var debug = false

    class GeneratePair {
        val input = ArrayList<File>()
        val output = ArrayList<File>()

        val available: Boolean get() = input.isNotEmpty() && output.isNotEmpty()
    }

    abstract var inputSourceSet: String
    abstract var generatedSourceSet: String
    abstract var processResources: String

    @Suppress("unused")
    @TaskAction
    open fun task() {
        logger.info("KorgeResourcesTask ($this)")
        for (p in project.allprojects) {
            for (resourceFolder in p.getResourcesFolders(setOf(inputSourceSet))) {
                if (resourceFolder.exists()) {
                    val output = resourceFolder.parentFile["genresources"]
                    KorgeBuildServiceProxy.processResourcesFolder(resourceFolder, output)
                }
            }
        }
    }
}

operator fun File.get(name: String) = File(this, name)

open class KorgeTestResourcesTask : KorgeBaseResourcesTask() {
    override var inputSourceSet = "test"
    override var generatedSourceSet = "testGenerated"
    override var processResources = "processTestResources"
}

open class KorgeResourcesTask : KorgeBaseResourcesTask() {
    override var inputSourceSet = "main"
    override var generatedSourceSet = "generated"
    override var processResources = "processResources"
}

fun Project.getResourcesFolders(sourceSets: Set<String>? = null): List<File> {
    val out = arrayListOf<File>()
    try {
        for (target in gkotlin.targets.toList()) {
            //println("TARGET: $target")
            for (compilation in target.compilations) {
                //println("  - COMPILATION: $compilation :: name=${compilation.name}")
                if (sourceSets != null && compilation.name !in sourceSets) continue
                for (sourceSet in compilation.allKotlinSourceSets) {
                    //println("    - SOURCE_SET: $sourceSet")
                    for (resource in sourceSet.resources.srcDirs) {
                        out += resource
                        //println("        - RESOURCE: $resource")
                    }
                }
            }
        }
    } catch (e: Throwable) {
        e.printStackTrace()
    }
    return out.distinct()
}

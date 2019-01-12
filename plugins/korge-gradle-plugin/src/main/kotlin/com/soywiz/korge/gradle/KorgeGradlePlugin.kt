package com.soywiz.korge.gradle

import com.moowork.gradle.node.NodeExtension
import com.moowork.gradle.node.exec.NodeExecRunner
import com.moowork.gradle.node.npm.NpmTask
import com.moowork.gradle.node.task.NodeTask
import com.soywiz.korge.gradle.apple.IcnsBuilder
import com.soywiz.korge.gradle.apple.InfoPlistBuilder
import groovy.text.*
import groovy.util.*
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.api.file.*
import org.gradle.api.plugins.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.testing.Test
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.gradle.process.ExecResult
import org.gradle.process.ExecSpec
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.KotlinJsDce
import proguard.gradle.ProGuardTask
import java.io.*
import java.net.*

val Project.gkotlin get() = properties["kotlin"] as KotlinMultiplatformExtension
val Project.ext get() = extensions.getByType(ExtraPropertiesExtension::class.java)
val kormaVersion get() = KorgeBuildServiceProxy.kormaVersion()
val korioVersion get() = KorgeBuildServiceProxy.korioVersion()
val korimVersion get() = KorgeBuildServiceProxy.korimVersion()
val korauVersion get() = KorgeBuildServiceProxy.korauVersion()
val koruiVersion get() = KorgeBuildServiceProxy.koruiVersion()
val korevVersion get() = KorgeBuildServiceProxy.korevVersion()
val korgwVersion get() = KorgeBuildServiceProxy.korgwVersion()
val kotlinVersion get() = KorgeBuildServiceProxy.kotlinVersion()
val korgeVersion get() = KorgeBuildServiceProxy.korgeVersion()


enum class Orientation(val lc: String) { DEFAULT("default"), LANDSCAPE("landscape"), PORTRAIT("portrait") }

data class KorgePluginDescriptor(val name: String, val args: Map<String, String>, val version: String?)

enum class GameCategory {
    ACTION, ADVENTURE, ARCADE, BOARD, CARD,
    CASINO, DICE, EDUCATIONAL, FAMILY, KIDS,
    MUSIC, PUZZLE, RACING, ROLE_PLAYING, SIMULATION,
    SPORTS, STRATEGY, TRIVIA, WORD
}

@Suppress("unused")
class KorgeExtension {
    internal fun init() {
        // Do nothing, but serves to be referenced to be installed
    }

    var id: String = "com.unknown.unknownapp"
    var version: String = "0.0.1"

    var exeBaseName: String = "app"

    var name: String = "unnamed"
    var description: String = "undescripted"
    var orientation: Orientation = Orientation.DEFAULT
    val plugins = arrayListOf<KorgePluginDescriptor>()

    var copyright: String = "Copyright (c) 2019 Unknown"

    var authorName = "unknown"
    var authorEmail = "unknown@unknown"
    var authorHref = "http://localhost"

    val icon: File? = File("icon.png")

    var gameCategory: GameCategory? = null

    var fullscreen = true

    var backgroundColor: Int = 0xff000000.toInt()

    var androidMinSdk: String? = null
    internal var _androidAppendBuildGradle: String? = null

    @JvmOverloads
    fun cordovaPlugin(name: CharSequence, args: Map<String, String> = mapOf(), version: CharSequence? = null) {
        plugins += KorgePluginDescriptor(name.toString(), args, version?.toString())
        //println("cordovaPlugin($name, $args, $version)")
    }

    fun androidAppendBuildGradle(str: String) {
        if (_androidAppendBuildGradle == null) {
            _androidAppendBuildGradle = ""
        }
        _androidAppendBuildGradle += str
    }

    fun cordovaUseCrosswalk() {
        // Required to have webgl on android emulator?
        // https://crosswalk-project.org/documentation/cordova.html
        // https://github.com/crosswalk-project/cordova-plugin-crosswalk-webview/issues/205#issuecomment-371669478
        if (androidMinSdk == null) androidMinSdk = "20"
        cordovaPlugin("cordova-plugin-crosswalk-webview", version = "2.4.0")
        androidAppendBuildGradle("""
        	configurations.all {
        		resolutionStrategy {
        			force 'com.android.support:support-v4:27.1.0'
        		}
        	}
        """)
    }

    @JvmOverloads
    fun author(name: String, email: String, href: String) {
        authorName = name
        authorEmail = email
        authorHref = href
    }
}

fun KorgeExtension.updateCordovaXml(cordovaConfig: QXml) {
    val korge = this
    cordovaConfig["name"].setValue(korge.name)
    cordovaConfig["description"].setValue(korge.description)

    cordovaConfig.setAttribute("id", korge.id)
    cordovaConfig.setAttribute("version", korge.version)

    cordovaConfig["author"].apply {
        setAttribute("email", korge.authorEmail)
        setAttribute("href", korge.authorHref)
        setValue(korge.authorName)
    }

    fun replaceCordovaPreference(name: String, value: String) {
        // Remove Orientation node and set a new node
        cordovaConfig.getOrAppendNode("preference", "name" to name).setAttribute("value", value)
        cordovaConfig["preference"].list.filter { it.attributes["name"] == name }.forEach { it.remove() }
        cordovaConfig.appendNode("preference", "name" to name, "value" to value)
    }

    // https://cordova.apache.org/docs/es/latest/config_ref/
    replaceCordovaPreference("Orientation", korge.orientation.lc)
    replaceCordovaPreference("Fullscreen", korge.fullscreen.toString())
    replaceCordovaPreference("BackgroundColor", "0x%08x".format(korge.backgroundColor))

    if (korge.androidMinSdk != null) {
        val android = cordovaConfig.getOrAppendNode("platform", "name" to "android")
        android.getOrAppendNode("preference", "name" to "android-minSdkVersion").setAttribute("value", korge.androidMinSdk!!)
    }

    cordovaConfig["icon"].remove()
    cordovaConfig.appendNode("icon", "src" to "icon.png")
}

fun KorgeExtension.updateCordovaXmlString(cordovaConfig: String): String {
    return updateXml(cordovaConfig) { updateCordovaXml(this) }
}

fun KorgeExtension.updateCordovaXmlFile(cordovaConfigXmlFile: File) {
    val cordovaConfigXml = cordovaConfigXmlFile.readText()
    val cordovaConfig = QXml(xmlParse(cordovaConfigXml))
    this.updateCordovaXml(cordovaConfig)
    cordovaConfigXmlFile.writeText(xmlSerialize(cordovaConfig.obj as Node))

}

val Project.korge: KorgeExtension get() {
    val extension = project.extensions.findByName("korge") as? KorgeExtension?
    return if (extension == null) {
        val newExtension = KorgeExtension()
        project.extensions.add("korge", newExtension)
        newExtension
    } else {
        extension
    }
}

open class JsWebCopy() : Copy() {
    open lateinit var targetDir: File
}

fun ExecSpec.commandLineCompat(vararg args: String): ExecSpec {
    return if (Os.isFamily(Os.FAMILY_WINDOWS)) {
        commandLine("cmd", "/c", *args)
    } else {
        commandLine(*args)
    }
}

class KorgeGradleApply(val project: Project) {
    val korgeCacheDir = File(System.getProperty("user.home"), ".korge").apply { mkdirs() }
    //val node_modules by lazy { project.file("node_modules") }
    val node_modules by lazy { korgeCacheDir["node_modules"] }
    val webMinFolder by lazy { project.buildDir["web-min"] }
    val webFolder by lazy { project.buildDir["web"] }
    val webMinWebpackFolder by lazy { project.buildDir["web-min-webpack"] }
    val mocha_node_modules by lazy { project.buildDir["node_modules"] }

    // Tasks
    lateinit var jsWeb: JsWebCopy
    lateinit var jsWebMin: JsWebCopy
    lateinit var jsWebMinWebpack: DefaultTask

    fun KorgeExtension.getIconBytes(): ByteArray {
        return if (icon != null && icon.exists()) {
            icon.readBytes()
        } else {
            KorgeGradlePlugin::class.java.getResource("/icons/korge.png").readBytes()
        }
    }

    fun apply() {
        System.setProperty("java.awt.headless", "true")

        if (project.gradle.gradleVersion != "4.7") {
            error("Korge only works with Gradle 4.7, but running on Gradle ${project.gradle.gradleVersion}")
        }

        KorgeBuildServiceProxy.init()
        project.addVersionExtension()
        project.configureRepositories()
        project.configureKotlin()
        project.addKorgeTasks()
        project.configureNode()
        project.configureIdea()
        project.addWeb()
        project.addProguard()
        project.addNativeRun()
        project.configureJvmTest()

        project.korge.init()
    }

    val nativeTarget = when {
        Os.isFamily(Os.FAMILY_WINDOWS) -> "mingwX64"
        Os.isFamily(Os.FAMILY_MAC) -> "macosX64"
        Os.isFamily(Os.FAMILY_UNIX) -> "linuxX64"
        else -> "unknownX64"
    }
    val cnativeTarget = nativeTarget.capitalize()

    val nativeTargets = listOf("mingwX64", "linuxX64", "macosX64")
    
    val korgeGroup = "korge"

    val RELEASE = "release"
    val DEBUG = "debug"
    val RELEASE_DEBUG = listOf(RELEASE, DEBUG)

    private fun Project.configureJvmTest() {
        val jvmTest = (tasks.findByName("jvmTest") as Test)
        jvmTest.jvmArgs = (jvmTest.jvmArgs ?: listOf()) + listOf("-Djava.awt.headless=true")
    }

    private fun Project.addNativeRun() {
        afterEvaluate {
            for (target in nativeTargets) {
                val ctarget = target.capitalize()
                for (kind in RELEASE_DEBUG) {
                    val ckind = kind.capitalize()
                    val ctargetKind = "$ctarget$ckind"

                    val compilation = gkotlin.targets[target]["compilations"]["main"] as KotlinNativeCompilation
                    val executableFile = compilation.getBinary("EXECUTABLE", kind)

                    val copyTask = project.addTask<Copy>("copyResourcesToExecutable$ctargetKind") { task ->
                        for (sourceSet in project.gkotlin.sourceSets) {
                            task.from(sourceSet.resources)
                        }
                        task.into(executableFile.parentFile)
                    }

                    addTask<Exec>("runNative$ctargetKind", dependsOn = listOf("link${ckind}Executable$ctarget", copyTask), group = korgeGroup) { task ->
                        task.executable = executableFile.absolutePath
                        task.args = listOf<String>()
                    }
                }
                addTask<Task>("runNative$ctarget", dependsOn = listOf("runNative${ctarget}Release"), group = korgeGroup) { task ->
                }
            }
        }

        addTask<Task>("runNative", dependsOn = listOf("runNative$cnativeTarget"), group = korgeGroup)
        addTask<Task>("runNativeDebug", dependsOn = listOf("runNative${cnativeTarget}Debug"), group = korgeGroup)

        afterEvaluate {
            for (buildType in RELEASE_DEBUG) {
                addTask<Task>("packageMacosX64App${buildType.capitalize()}", group = "korge", dependsOn = listOf("link${buildType.capitalize()}ExecutableMacosX64")) {
                    doLast {
                        val compilation = gkotlin.targets["macosX64"]["compilations"]["main"] as KotlinNativeCompilation
                        val executableFile = compilation.getBinary("EXECUTABLE", buildType)
                        val appFolder = buildDir["${korge.name}-$buildType.app"].apply { mkdirs() }
                        val appFolderContents = appFolder["Contents"].apply { mkdirs() }
                        val appMacOSFolder = appFolderContents["MacOS"].apply { mkdirs() }
                        val resourcesFolder = appFolderContents["Resources"].apply { mkdirs() }
                        appFolderContents["Info.plist"].writeText(InfoPlistBuilder.build(korge))
                        resourcesFolder["${korge.exeBaseName}.icns"].writeBytes(IcnsBuilder.build(korge.getIconBytes()))
                        copy { copy ->
                            for (sourceSet in project.gkotlin.sourceSets) {
                                copy.from(sourceSet.resources)
                            }
                            copy.into(resourcesFolder)
                        }
                        executableFile.copyTo(appMacOSFolder[korge.exeBaseName], overwrite = true)
                        appMacOSFolder[korge.exeBaseName].setExecutable(true)
                    }
                }
            }
        }
    }


    private fun Project.configureIdea() {
        project.plugins.apply("idea")
        (project["idea"] as IdeaModel).apply {
            module { module ->
                for (file in listOf(".gradle", "node_modules", "classes", "docs", "dependency-cache", "libs", "reports", "resources", "test-results", "tmp")) {
                    module.excludeDirs.add(file(".gradle"))
                }
            }
        }
    }

    private fun nodeExec(vararg args: Any, workingDir: File? = null): ExecResult {
        return NodeExecRunner(project).apply {
            this.workingDir = workingDir ?: this.workingDir
            this.environment += mapOf(
                "NODE_PATH" to node_modules
            )
            arguments = args.toList()
        }.execute()
    }

    private fun Project.configureNode() {
        plugins.apply("com.moowork.node")

        (project["node"] as NodeExtension).apply {
            this.version = "10.14.2"
            //this.version = "8.11.4"
            this.download = true

            this.workDir = korgeCacheDir["nodejs"]
            this.npmWorkDir = korgeCacheDir["npm"]
            this.yarnWorkDir = korgeCacheDir["yarn"]
            this.nodeModulesDir = this@KorgeGradleApply.node_modules

            //this.nodeModulesDir = java.io.File(project.buildDir, "npm")
        }

        // Fix for https://github.com/srs/gradle-node-plugin/issues/301
        repositories.whenObjectAdded {
            if (it is IvyArtifactRepository) {
                it.metadataSources {
                    it.artifact()
                }
            }
        }

        val jsInstallMocha = project.addTask<NpmTask>("jsInstallMocha") { task ->
            task.onlyIf { !node_modules["/mocha"].exists() }
            task.setArgs(listOf("install", "mocha@5.2.0"))
        }

        val jsInstallCordova = project.addTask<NpmTask>("jsInstallCordova") { task ->
            task.onlyIf { !node_modules["/cordova"].exists() }
            task.setArgs(listOf("install", "cordova@8.1.2"))
        }

        val jsInstallCanvas = project.addTask<NpmTask>("jsInstallCanvas") { task ->
            task.onlyIf { !node_modules["/canvas"].exists() }
            task.setArgs(listOf("install", "canvas@2.2.0"))
        }

        val jsInstallWebpack = project.addTask<NpmTask>("jsInstallWebpack") { task ->
            task.onlyIf { !node_modules["webpack"].exists() || !node_modules["webpack-cli"].exists() }
            task.setArgs(listOf("install", "webpack@4.28.2", "webpack-cli@3.1.2"))
        }

        val jsInstallMochaHeadlessChrome = project.addTask<NpmTask>("jsInstallMochaHeadlessChrome") { task ->
            task.onlyIf { !node_modules["mocha-headless-chrome"].exists() }
            task.setArgs(listOf("install", "mocha-headless-chrome@2.0.1"))
        }

        val jsCompilations = project["kotlin"]["targets"]["js"]["compilations"]

        val populateNodeModules = project.addTask<DefaultTask>("populateNodeModules") { task ->
            task.doLast {
                copy { copy ->
                    copy.from(jsCompilations["main"]["output"]["allOutputs"])
                    copy.from(jsCompilations["test"]["output"]["allOutputs"])
                    (jsCompilations["test"]["runtimeDependencyFiles"] as Iterable<File>).forEach { file ->
                        if (file.exists() && !file.isDirectory()) {
                            copy.from(zipTree(file.absolutePath).matching { it.include("*.js") })
                        }
                    }
                    for (sourceSet in project.gkotlin.sourceSets) {
                        copy.from(sourceSet.resources)
                    }
                    copy.into(mocha_node_modules)
                }
                copy { copy ->
                    copy.from("$node_modules/mocha")
                    copy.into("$mocha_node_modules/mocha")
                }
            }
        }

        val jsTestChrome = project.addTask<NodeTask>("jsTestChrome", dependsOn = listOf(jsCompilations["test"]["compileKotlinTaskName"], jsInstallMochaHeadlessChrome, jsInstallMocha, populateNodeModules)) { task ->
            task.doFirst {
                File("$buildDir/node_modules/tests.html").writeText("""<!DOCTYPE html><html>
                    <head>
                        <title>Mocha Tests</title>
                        <meta charset="utf-8">
                        <link rel="stylesheet" href="mocha/mocha.css">
                        <script src="https://cdnjs.cloudflare.com/ajax/libs/require.js/2.3.6/require.min.js"></script>
                    </head>
                    <body>
                    <div id="mocha"></div>
                    <script src="mocha/mocha.js"></script>
                    <script>
                        requirejs.config({'baseUrl': '.', 'paths': { 'tests': '../classes/kotlin/js/test/${project.name}_test' }});
                        mocha.setup('bdd');
                        require(['tests'], function() { mocha.run(); });
                    </script>
                    </body>
                    </html>
                """)
            }
            task.setScript(node_modules["mocha-headless-chrome/bin/start"])
            task.setArgs(listOf("-f", "$buildDir/node_modules/tests.html", "-a", "no-sandbox", "-a", "disable-setuid-sandbox", "-a", "allow-file-access-from-files"))
        }

        val runMocha = project.addTask<NodeTask>("runMocha", dependsOn = listOf(
            jsCompilations["test"]["compileKotlinTaskName"],
            jsInstallMocha, jsInstallCanvas,
            populateNodeModules
        )) { task ->
            task.setEnvironment(mapOf("NODE_MODULES" to "$node_modules${File.pathSeparator}$mocha_node_modules"))
            task.setScript(node_modules["mocha/bin/mocha"])
            task.setWorkingDir(project.file("$buildDir/node_modules"))
            task.setArgs(listOf("--timeout", "15000", "${project.name}_test.js"))
        }

        // Only run JS tests if not in windows
        if (!org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS)) {
            project.tasks.getByName("jsTest")?.dependsOn(runMocha)
        }
    }

    private fun Project.addWeb() {
        fun configureJsWeb(task: JsWebCopy, minimized: Boolean) {
            val excludesNormal = arrayOf("**/*.kotlin_metadata","**/*.kotlin_module","**/*.MF","**/*.kjsm","**/*.map","**/*.meta.js")
            val excludesJs = arrayOf("**/*.js")
            val excludesAll = excludesNormal + excludesJs

            fun CopySpec.configureWeb() {
                if (minimized) {
                    //include("**/require.min.js")
                    exclude(*excludesAll)
                } else {
                    exclude(*excludesNormal)
                }
            }

            task.targetDir = project.buildDir[if (minimized) "web-min" else "web"]
            project.afterEvaluate {
                val kotlinTargets = project["kotlin"]["targets"]
                val jsCompilations = kotlinTargets["js"]["compilations"]
                task.includeEmptyDirs = false
                if (minimized) {
                    task.from((project["runDceJsKotlin"] as KotlinJsDce).destinationDir) { copy -> copy.exclude(*excludesNormal) }
                }
                task.from((jsCompilations["main"] as KotlinCompilation).output.allOutputs) { copy -> copy.configureWeb() }
                task.from("${project.buildDir}/npm/node_modules") { copy -> copy.configureWeb() }
                for (file in (jsCompilations["test"]["runtimeDependencyFiles"] as FileCollection).toList()) {
                    if (file.exists() && !file.isDirectory) {
                        task.from(project.zipTree(file.absolutePath)) { copy -> copy.configureWeb() }
                        task.from(project.zipTree(file.absolutePath)) { copy -> copy.include("**/*.min.js") }
                    } else {
                        task.from(file) { copy -> copy.configureWeb() }
                        task.from(file) { copy -> copy.include("**/*.min.js") }
                    }
                }

                for (target in listOf(kotlinTargets["js"], kotlinTargets["metadata"])) {
                    val main = (target["compilations"]["main"] as KotlinCompilation)
                    for (sourceSet in main.kotlinSourceSets) {
                        task.from(sourceSet.resources) { copy -> copy.configureWeb() }
                    }
                }
                //task.exclude(*excludesNormal)
                task.into(task.targetDir)
            }

            task.doLast {
                task.targetDir["index.html"].writeText(SimpleTemplateEngine().createTemplate(task.targetDir["index.template.html"].readText()).make(mapOf(
                    "OUTPUT" to project.name,
                    "TITLE" to korge.name
                )).toString())
            }
        }

        jsWeb = project.addTask<JsWebCopy>(name = "jsWeb", dependsOn = listOf("jsJar")) { task ->
            configureJsWeb(task, minimized = false)
        }

        jsWebMin = project.addTask<JsWebCopy>(name = "jsWebMin", dependsOn = listOf("runDceJsKotlin")) { task ->
            configureJsWeb(task, minimized = true)
        }

        jsWebMinWebpack = project.addTask<DefaultTask>("jsWebMinWebpack", dependsOn = listOf(
            "jsInstallWebpack",
            "jsWebMin"
        )) { task ->
            task.doLast {
                copy { copy ->
                    copy.from(webMinFolder)
                    copy.into(webMinWebpackFolder)
                    copy.exclude("**/*.js", "**/index.template.html", "**/index.html")
                }

                val webpackConfigJs = buildDir["webpack.config.js"]

                webpackConfigJs.writeText("""
                    const path = require('path');
                    const webpack = require('webpack');
                    const modules = ${webMinFolder.absolutePath.quoted};

                    module.exports = {
                      context: modules,
                      entry: ${"$webMinFolder/${project.name}.js".quoted},
                      resolve: {
                        modules: [ modules ],
                      },
                      output: {
                        path: ${webMinWebpackFolder.absolutePath.quoted},
                        filename: 'bundle.js'
                      },
                      target: 'node',
                      plugins: [
                        new webpack.IgnorePlugin(/^canvas${'$'}/)
                      ]
                    };
                """.trimIndent())

                nodeExec(node_modules["webpack/bin/webpack.js"], "--config", webpackConfigJs)

                val indexHtml = webMinFolder["index.html"].readText()
                webMinWebpackFolder["index.html"].writeText(indexHtml.replace(Regex("<script data-main=\"(.*?)\" src=\"require.min.js\" type=\"text/javascript\"></script>"), "<script src=\"bundle.js\" type=\"text/javascript\"></script>"))
            }
        }

        project.afterEvaluate {
            for (target in nativeTargets) {
                val taskName = "copyResourcesToExecutableTest_${target.capitalize()}"
                val targetTestTask = tasks.getByName("${target}Test")
                val task = project.addTask<Copy>(taskName) { task ->
                    for (sourceSet in project.gkotlin.sourceSets) {
                        task.from(sourceSet.resources)
                    }
                    task.into(File(targetTestTask.inputs.properties["executable"].toString()).parentFile)
                }
                targetTestTask.dependsOn(task)
            }

            // @TODO: This doesn't work after migrating code to Kotlin.
            //for (target in listOf(project.gkotlin.targets["js"], project.gkotlin.targets["metadata"])) {
            //    for (it in (target["compilations"]["main"]["kotlinSourceSets"]["resources"] as Iterable<SourceDirectorySet>).flatMap { it.srcDirs }) {
            //        (tasks["jsJar"] as Copy).from(it)
            //    }
            //}
        }
    }

    private fun Project.addProguard() {
        // Provide default mainClassName
        if (!project.ext.has("mainClassName")) {
            project.ext.set("mainClassName", "")
        }

        // packageJvmFatJar
        val packageJvmFatJar = project.addTask<org.gradle.jvm.tasks.Jar>("packageJvmFatJar", group = korgeGroup) { task ->
            task.baseName = "${project.name}-all"
            project.afterEvaluate {
                task.manifest { manifest ->
                    manifest.attributes(mapOf(
                        "Implementation-Title" to project.ext.get("mainClassName"),
                        "Implementation-Version" to project.version.toString(),
                        "Main-Class" to project.ext.get("mainClassName")
                    ))
                }
                //it.from()
                //fileTree()
                task.from(GroovyClosure(project) {
                    (project["kotlin"]["targets"]["jvm"]["compilations"]["main"]["runtimeDependencyFiles"] as FileCollection).map { if (it.isDirectory) it else project.zipTree(it) as Any }
                    //listOf<File>()
                })
                task.with(project.getTasksByName("jvmJar", true).first() as CopySpec)
            }
        }

        val runJvm = tasks.getByName("runJvm") as JavaExec

        project.addTask<ProGuardTask>("packageJvmFatJarProguard", group = korgeGroup, dependsOn = listOf(packageJvmFatJar)) { task ->
            task.libraryjars("${System.getProperty("java.home")}/lib/rt.jar")
            task.injars(packageJvmFatJar.outputs.files.toList())
            task.outjars(buildDir["/libs/${project.name}-all-proguard.jar"])
            task.dontwarn()
            task.ignorewarnings()
            //task.dontobfuscate()
            task.assumenosideeffects("""
                class kotlin.jvm.internal.Intrinsics {
                    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
                }
            """.trimIndent())

            //task.keep("class jogamp.nativetag.**")
            //task.keep("class jogamp.**")

            task.keep("class com.jogamp.** { *; }")
            task.keep("class jogamp.** { *; }")

            afterEvaluate {
                task.keep("""public class ${runJvm.main} {
                    public static void main(java.lang.String[]);
                }""")
            }
        }
    }

    private fun Project.addVersionExtension() {
        ext.set("korioVersion", korioVersion)
        ext.set("kormaVersion", kormaVersion)
        ext.set("korauVersion", korauVersion)
        ext.set("korimVersion", korimVersion)
        ext.set("koruiVersion", koruiVersion)
        ext.set("korevVersion", korevVersion)
        ext.set("korgwVersion", korgwVersion)
        ext.set("korgeVersion", korgeVersion)
        ext.set("kotlinVersion", kotlinVersion)
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
        plugins.apply("kotlin-dce-js")

        for (preset in nativeTargets) {
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

        project.dependencies.add("jsMainImplementation", "org.jetbrains.kotlin:kotlin-stdlib-js")
        project.dependencies.add("jsTestImplementation", "org.jetbrains.kotlin:kotlin-test-js")

        project.dependencies.add("jvmMainImplementation", "org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        project.dependencies.add("jvmTestImplementation", "org.jetbrains.kotlin:kotlin-test")
        project.dependencies.add("jvmTestImplementation", "org.jetbrains.kotlin:kotlin-test-junit")


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
                "genResources", group = korgeGroup, description = "process resources",
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
                "genTestResources", group = korgeGroup, description = "process test resources",
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

        val cordovaFolder = project.buildDir["cordova"]
        val cordovaConfigXmlFile = cordovaFolder["config.xml"]

        //val korge = KorgeXml(project.file("korge.project.xml"))
        val korge = project.korge

        run {
            val cordova_bin = node_modules["cordova/bin/cordova"]

            fun NodeTask.setCordova(vararg args: String) {
                setWorkingDir(cordovaFolder)
                setScript(cordova_bin)
                setArgs(listOf(*args))
            }

            val cordovaCreate = project.addTask<NodeTask>("cordovaCreate", dependsOn = listOf("jsInstallCordova")) { task ->
                task.onlyIf { !cordovaFolder.exists() }
                task.doFirst {
                    buildDir.mkdirs()
                }
                task.setCordova("create", cordovaFolder.absolutePath, "com.soywiz.sample1", "sample1")
                task.setWorkingDir(project.projectDir)
            }

            val cordovaUpdateIcon = project.addTask<Task>("cordovaUpdateIcon", dependsOn = listOf(cordovaCreate)) { task ->
                task.doLast {
                    cordovaFolder.mkdirs()
                    cordovaFolder["icon.png"].writeBytes(korge.getIconBytes())
                }
            }

            val cordovaPluginsList = project.addTask<DefaultTask>("cordovaPluginsList", dependsOn = listOf(cordovaCreate)) { task ->
                task.doLast {
                    println("name: ${korge.name}")
                    println("description: ${korge.description}")
                    println("orientation: ${korge.orientation}")
                    println("plugins: ${korge.plugins}")
                }
            }

            val cordovaSynchronizeConfigXml = project.addTask<DefaultTask>("cordovaSynchronizeConfigXml", dependsOn = listOf(cordovaCreate, cordovaUpdateIcon)) { task ->
                task.doLast {
                    korge.updateCordovaXmlFile(cordovaConfigXmlFile)
                }
            }

            val cordovaPluginsInstall = project.addTask<Task>("cordovaPluginsInstall", dependsOn = listOf(cordovaCreate)) { task ->
                task.doLast {
                    println("korge.plugins: ${korge.plugins}")
                    for (plugin in korge.plugins) {
                        val list = plugin.args.flatMap { listOf("--variable", "${it.key}=${it.value}") }.toTypedArray()
                        nodeExec(
                            cordova_bin, "plugin", "add", plugin.name, "--save", *list,
                            workingDir = cordovaFolder
                        )
                    }
                }
            }

            val runJvm = project.addTask<JavaExec>("runJvm", group = korgeGroup) { task ->
                afterEvaluate {
                    task.classpath = project["kotlin"]["targets"]["jvm"]["compilations"]["test"]["runtimeDependencyFiles"] as? FileCollection?
                    task.main = project.ext.get("mainClassName") as? String?
                }
            }

            val cordovaPackageJsWeb = project.addTask<Copy>("cordovaPackageJsWeb", group = korgeGroup, dependsOn = listOf("jsWebMinWebpack", cordovaCreate, cordovaPluginsInstall, cordovaSynchronizeConfigXml)) { task ->
                //afterEvaluate {
                //task.from(project.closure { jsWeb.targetDir })
                task.from(project.closure { webMinWebpackFolder })
                task.into(cordovaFolder["www"])
                //}
                task.doLast {
                    val f = cordovaFolder["www/index.html"]
                    f.writeText(f.readText().replace("</head>", "<script type=\"text/javascript\" src=\"cordova.js\"></script></head>"))
                }
            }

            val cordovaPackageJsWebNoMinimized = project.addTask<Copy>("cordovaPackageJsWebNoMinimized", group = korgeGroup, dependsOn = listOf("jsWeb", cordovaCreate, cordovaPluginsInstall, cordovaSynchronizeConfigXml)) { task ->
                task.from(project.closure { webFolder })
                task.into(cordovaFolder["www"])
                //}
                task.doLast {
                    val f = cordovaFolder["www/index.html"]
                    f.writeText(f.readText().replace("</head>", "<script type=\"text/javascript\" src=\"cordova.js\"></script></head>"))
                }
            }

            val cordovaPrepareTargets = project.addTask<Task>("cordovaPrepareTargets", group = korgeGroup, dependsOn = listOf(cordovaCreate)) { task ->
                task.doLast {
                    if (korge._androidAppendBuildGradle != null) {
                        // https://cordova.apache.org/docs/en/8.x/guide/platforms/android/index.html
                        val androidFolder = cordovaFolder["platforms/android"]
                        if (androidFolder.exists()) {
                            androidFolder["build-extras.gradle"].writeText(korge._androidAppendBuildGradle!!)
                        }
                    }
                }
            }

            for (target in listOf("ios", "android", "browser", "osx", "windows")) {
                val Target = target.capitalize()

                val cordovaTargetInstall = project.addTask<NodeTask>("cordova${Target}Install", dependsOn = listOf(cordovaCreate)) { task ->
                    task.onlyIf { !cordovaFolder["platforms/$target"].exists() }
                    task.setCordova("platform", "add", target)
                }

                val compileTarget = project.addTask<NodeTask>("compile$Target", group = korgeGroup, dependsOn = listOf(cordovaTargetInstall, cordovaPackageJsWeb, cordovaPrepareTargets)) { task ->
                    task.setCordova("build", target) // prepare + compile
                }

                val compileTargetRelease = project.addTask<NodeTask>("compile${Target}Release", group = korgeGroup, dependsOn = listOf(cordovaTargetInstall, cordovaPackageJsWeb, cordovaPrepareTargets)) { task ->
                    task.setCordova("build", target, "--release") // prepare + compile
                }

                for (noMinimized in listOf(false, true)) {
                    val NoMinimizedText = if (noMinimized) "NoMinimized" else ""

                    for (emulator in listOf(false, true)) {
                        val EmulatorText = if (emulator) "Emulator" else ""
                        val runTarget = project.addTask<NodeTask>(
                            "run$Target$EmulatorText$NoMinimizedText",
                            group = korgeGroup,
                            dependsOn = listOf(cordovaTargetInstall, if (noMinimized) cordovaPackageJsWebNoMinimized else cordovaPackageJsWeb, cordovaPrepareTargets)
                        ) { task ->
                            task.setCordova("run", target, if (emulator) "--emulator" else "--device")
                        }
                    }
                }
            }
        }
    }
}

open class KorgeGradlePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        KorgeGradleApply(project).apply()

        //for (res in project.getResourcesFolders()) println("- $res")
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

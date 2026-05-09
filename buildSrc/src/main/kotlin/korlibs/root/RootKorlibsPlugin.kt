@file:Suppress("DEPRECATION_ERROR", "DEPRECATION")

package korlibs.root

import java.io.*
import java.nio.file.*
import korlibs.*
import korlibs.korge.gradle.*
import korlibs.korge.gradle.module.*
import korlibs.korge.gradle.targets.*
import korlibs.korge.gradle.targets.all.*
import korlibs.korge.gradle.targets.android.*
import korlibs.korge.gradle.targets.ios.*
import korlibs.korge.gradle.targets.js.*
import korlibs.korge.gradle.targets.jvm.*
import korlibs.korge.gradle.targets.native.*
import korlibs.korge.gradle.targets.wasm.*
import korlibs.korge.gradle.util.*
import korlibs.modules.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.reporting.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.testing.*
import org.gradle.testing.base.plugins.*
import org.jetbrains.dokka.gradle.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.js.nodejs.*
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import org.jetbrains.kotlin.gradle.targets.js.testing.*
import org.jetbrains.kotlin.gradle.targets.js.testing.karma.*
import org.jetbrains.kotlin.gradle.targets.js.testing.mocha.*
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

object RootKorlibsPlugin {
    val KORGE_GROUP = "org.korge.engine"
    val KORGE_RELOAD_AGENT_GROUP = "org.korge.engine"
    val KORGE_GRADLE_PLUGIN_GROUP = "org.korge.gradleplugins"

    @JvmStatic
    fun doInit(rootProject: Project) {
        rootProject.init()
    }

    fun Project.init() {
        plugins.apply(DokkaPlugin::class.java)
        //plugins.apply("js-plain-objects")

        allprojects {
            tasks.withType(AbstractDokkaTask::class.java).configureEach {
                //println("DOKKA=$it")
                it.offlineMode.set(true)
            }
        }

        checkMinimumJavaVersion()
        configureBuildScriptClasspathTasks()
        initPlugins()
        initRootKotlinJvmTarget()
        initVersions()
        initAllRepositories()
        configureIdea()
        initGroupOverrides()
        initNodeJSFixes()
        initDuplicatesStrategy()
        initSymlinkTrees()
        initShowSystemInfoWhenLinkingInWindows()
        korlibs.korge.gradle.KorgeVersionsTask.registerShowKorgeVersions(project)
        initInstallAndCheckLinuxLibs()
        // Disabled by default, since it resolves configurations at configuration time
        if (System.getenv("ENABLE_KOVER") == "true") configureKover()
        initPublishing()
        initKMM()
        initCrossTests()
        initAllTargets()
        initSamples()
    }

    fun Project.initAllTargets() {
        rootProject.afterEvaluate {
            rootProject.rootEnableFeaturesOnAllTargets()
        }
    }

    fun Project.initRootKotlinJvmTarget() {
        // Required by RC
        kotlin {
            // Forced Java8 toolchain
            //jvmToolchain { (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of("8")) }
            jvm()
        }
    }

    fun Project.initVersions() {
        allprojects {
            project.version = getProjectForcedVersion()
        }
    }

    fun Project.initAllRepositories() {
        allprojects {
            configureRepositories()
        }
    }

    fun Project.initGroupOverrides() {
        allprojects {
            val projectName = project.name
            val firstComponent = projectName.substringBefore('-')
            group = RootKorlibsPlugin.KORGE_GROUP
        }
    }

    fun Project.initNodeJSFixes() {
        plugins.applyOnce<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin>()
        rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class.java, Action {
            rootProject.extensions.getByType(NodeJsRootExtension::class.java).nodeVersion = project.nodeVersion
        })
        // https://youtrack.jetbrains.com/issue/KT-48273
        afterEvaluate {
            rootProject.extensions.configure(NodeJsRootExtension::class.java, Action {
                //it.versions.webpackDevServer.version = "4.0.0"
            })
        }
    }

    fun Project.initDuplicatesStrategy() {
        allprojects {
            tasks.withType(Copy::class.java).allThis {
                //this.duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.WARN
                this.duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
                //println("Task $this")
            }
        }
    }

    fun Project.initSymlinkTrees() {
    }

    fun Project.initShowSystemInfoWhenLinkingInWindows() {
        fun Task.configureGCAndSystemInfo() {
            val task = this
            task.doFirst {
                execThis { commandLine("systeminfo") }
                println("jcmd -l; jcmd 0 GC.heap_info; jcmd 0 GC.run")
                execThis { commandLine("jcmd", "-l") }
                execThis { commandLine("jcmd", "0", "GC.heap_info") }
                repeat(5) { execThis { commandLine("jcmd", "0", "GC.run") } }
                execThis { commandLine("systeminfo") }
            }
            task.doLast { execThis { commandLine("systeminfo") } }
        }
    }

    fun Project.initInstallAndCheckLinuxLibs() {
        // Install required libraries in Linux with APT
        if (
            org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_UNIX) &&
            (File("/.dockerenv").exists() || System.getenv("TRAVIS") != null || System.getenv("GITHUB_REPOSITORY") != null) &&
            (File("/usr/bin/apt-get").exists()) &&
            (!(File("/usr/include/GL/glut.h").exists()) || !(File("/usr/include/AL/al.h").exists()))
        ) {
            rootProject.execThis { commandLine("sudo", "apt-get", "update") }
            rootProject.execThis { commandLine("sudo", "apt-get", "-y", "install", "freeglut3") }
            // execThis { commandLine("sudo", "apt-get", "-y", "install", "libgtk-3-dev") }
        }
        if (isLinux) {
            project.logger.info("LD folders: ${LDLibraries.ldFolders}")
            for (lib in listOf("libGL.so.1")) {
                if (!LDLibraries.hasLibrary(lib)) {
                    System.err.println("Can't find $lib. Please: sudo apt-get -y install freeglut3")
                }
            }
        }
    }

    fun Project.initPlugins() {
        // java-base keeps lifecycle tasks without conflicting with kotlin-multiplatform
        plugins.apply("java-base")
        // vanniktech (maven-publish) must be applied BEFORE kotlin-multiplatform; otherwise Gradle 8
        // raises "Plugin 'maven-publish' was applied too late." for the root project.
        plugins.apply("com.vanniktech.maven.publish")
        plugins.apply("kotlin-multiplatform")
        // signing is applied per-project by configureSigning()
    }

    fun Project.initPublishing() {
        // The vanniktech plugin (which applies maven-publish) must be applied during the configuration
        // phase — not inside afterEvaluate. Apply it now for the root project (korge-root) itself.
        rootProject.configurePublishing()

        rootProject.afterEvaluate {
            // Finish signing setup for root project
//            rootProject.configureSigning()

            rootProject.nonSamples {
                if (this.project.isKorgeBenchmarks) return@nonSamples

                val doConfigure = mustAutoconfigureKMM()

                if (doConfigure) {
                    // vanniktech handles maven-publish + Central Portal repository setup
                    configurePublishing()
                    // Existing Signing.kt handles GPG signing of the publications
//                    configureSigning()
                }
            }
        }
    }

    fun Project.initKMM() {
        rootProject.subprojects {
            val doConfigure = mustAutoconfigureKMM()

            if (doConfigure) {
                val isSample = project.isSample
                val isAndroidApp = project.isAndroidApp
                val hasAndroid = doEnableKotlinAndroid && hasAndroidSdk && project.name != "korge-benchmarks"

                // AppData\Local\Android\Sdk\tools\bin>sdkmanager --licenses
                if (!isAndroidApp) plugins.apply("kotlin-multiplatform")
                //plugins.apply(JsPlainObjectsKotlinGradleSubplugin::class.java)

                //initAndroidProject()
                if (hasAndroid) {
                    project.configureAndroidDirect(ProjectType.fromExecutable(isAndroidApp), isKorge = false)
                }

                if (isSample && supportKotlinNative && isMacos) {
                    project.configureNativeIos(projectType = ProjectType.EXECUTABLE)
                }

                if (!isSample && rootProject.plugins.hasPlugin("org.jetbrains.dokka")) {
                    plugins.apply("org.jetbrains.dokka")
                }

                tasks.withType(KotlinCompile::class.java).configureEach {
                    it.kotlinOptions.suppressWarnings = true
                }

                // Configure only multiplatform modules, exclude androidApp modules
                extensions.findByType(KotlinMultiplatformExtension::class.java)?.apply {
                    metadata {
                        compilations.allThis {
                            kotlinOptions.suppressWarnings = true
                        }
                    }
                    jvm {
                        compilations.allThis {
                            compileTaskProvider.configure {
                                (it as org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile)
                                    .compilerOptions
                                    .jvmTarget
                                    .set(JvmTarget.fromTarget(GRADLE_JAVA_VERSION_STR))
                            }
                            //kotlinOptions.freeCompilerArgs.add("-Xno-param-assertions")
                            //kotlinOptions.

                            // @TODO:
                            // Tested on Kotlin 1.4.30:
                            // Class org.luaj.vm2.WeakTableTest.WeakKeyTableTest
                            // java.lang.AssertionError: expected:<null> but was:<mydata-111>
                            //kotlinOptions.useIR = true
                        }
                        AddFreeCompilerArgs.addFreeCompilerArgs(project, this)
                    }
                    if (isWasmEnabled(project)) {
                        configureWasmTarget(executable = false)
                        val wasmBrowserTest = tasks.getByName("wasmJsBrowserTest") as KotlinJsTest
                        // ~/projects/korge/build/js/packages/korge-root-klock-wasm-test
                        wasmBrowserTest.doFirst {
                            logger.info("!!!!! wasmBrowserTest PATCH :: $wasmBrowserTest : ${wasmBrowserTest::class.java}")

                            val npmProjectDir: File = wasmBrowserTest.compilation.npmProject.dir.get().asFile
                            val projectName = npmProjectDir.name
                            val uninstantiatedMjs = File(npmProjectDir, "kotlin/$projectName.uninstantiated.mjs")

                            logger.info("# Updating: $uninstantiatedMjs")

                            try {
                                uninstantiatedMjs.writeText(uninstantiatedMjs.readText().replace(
                                    "'kotlin.test.jsThrow' : (jsException) => { throw e },",
                                    "'kotlin.test.jsThrow' : (jsException) => { throw jsException },",
                                ))
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                        }
                    }
                    js(KotlinJsCompilerType.IR) {
                        browser {
                            compilations.allThis {
                                //kotlinOptions.sourceMap = true
                            }
                        }
                        configureJsTargetOnce()
                        configureJSTestsOnce()
                    }
                    //configureJSTests()

                    tasks.withType(KotlinJsTest::class.java).configureEach {
                        it.onTestFrameworkSet { framework ->
                            //println("onTestFrameworkSet: $it : $framework")
                            when (framework) {
                                is KotlinMocha -> {
                                    framework.timeout = "20s"
                                }
                                is KotlinKarma -> {
                                    File(rootProject.rootDir, "karma.config.d").takeIfExists()?.let {
                                        //println("  -> $it")
                                        framework.useConfigDirectory(it)
                                        //println("       ")
                                    }
                                }
                            }
                        }
                    }

                    val desktopAndMobileTargets = ArrayList<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().apply {
                        if (doEnableKotlinMobile) addAll(mobileTargets(project))
                    }.toList()

                    for (target in desktopAndMobileTargets) {
                        target.configureKotlinNativeTarget(project)
                    }

                    // common
                    //    js
                    //    concurrent // non-js
                    //      jvmAndroid
                    //         android
                    //         jvm
                    //      native
                    //         kotlin-native
                    //    nonNative: [js, jvmAndroid]
                    sourceSets.apply {

                        val common = createPairSourceSet("common", project = project) { test ->
                            dependencies {
                                if (test) {
                                    implementation(kotlin("test-common"))
                                    implementation(kotlin("test-annotations-common"))
                                } else {
                                    implementation(kotlin("stdlib-common"))
                                }
                            }
                        }

                        val concurrent = createPairSourceSet("concurrent", common, project = project)
                        val jvmAndroid = createPairSourceSet("jvmAndroid", concurrent, project = project)

                        // Default source set for JVM-specific sources and dependencies:
                        // JVM-specific tests and their dependencies:
                        val jvm = createPairSourceSet("jvm", jvmAndroid, project = project) { test ->
                            dependencies {
                                if (test) {
                                    implementation(kotlin("test-junit"))
                                } else {
                                    implementation(kotlin("stdlib-jdk8"))
                                }
                            }
                        }

                        if (hasAndroid) {
                            val android = createPairSourceSet("android", jvmAndroid, doTest = false, project = project) { test ->
                                dependencies {
                                    if (test) {
                                        //implementation(kotlin("test"))
                                        //implementation(kotlin("test-junit"))
                                        implementation(kotlin("test-junit"))
                                    } else {
                                        //implementation(kotlin("stdlib"))
                                        //implementation(kotlin("stdlib-jdk8"))
                                    }
                                }
                            }
                        }

                        val js = createPairSourceSet("js", common, project = project) { test ->
                            dependencies {
                                if (test) {
                                    implementation(kotlin("test-js"))
                                } else {
                                    implementation(kotlin("stdlib-js"))
                                }
                            }
                        }

                        if (isWasmEnabled(project)) {
                            val wasm = createPairSourceSet("wasmJs", common, project = project) { test ->
                                dependencies {
                                    if (test) {
                                        implementation(kotlin("test-wasm-js"))
                                    } else {
                                        implementation(kotlin("stdlib-wasm-js"))
                                    }
                                }
                            }
                        }

                        if (supportKotlinNative) {
                            //val iosTvosMacos by lazy { createPairSourceSet("iosTvosMacos", darwin) }
                            //val iosMacos by lazy { createPairSourceSet("iosMacos", iosTvosMacos) }

                            val native by lazy { createPairSourceSet("native", concurrent, project = project) }
                            val posix by lazy { createPairSourceSet("posix", native, project = project) }
                            val apple by lazy { createPairSourceSet("apple", posix, project = project) }
                            val darwin by lazy { createPairSourceSet("darwin", apple, project = project) }
                            val darwinMobile by lazy { createPairSourceSet("darwinMobile", darwin, project = project) }
                            val iosTvos by lazy { createPairSourceSet("iosTvos", darwinMobile/*, iosTvosMacos*/, project = project) }
                            val tvos by lazy { createPairSourceSet("tvos", iosTvos, project = project) }
                            val ios by lazy { createPairSourceSet("ios", iosTvos/*, iosMacos*/, project = project) }

                            for (target in mobileTargets(project)) {
                                val native = createPairSourceSet(target.name, project = project)
                                when {
                                    target.isIos -> native.dependsOn(ios)
                                    target.isTvos -> native.dependsOn(tvos)
                                }
                            }

                            // Copy test resources
                            afterEvaluate {
                                for (targetV in (listOf(iosX64(), iosSimulatorArm64()))) {
                                    val target = targetV.name
                                    val taskName = "copyResourcesToExecutable_$target"
                                    val targetTestTask = tasks.findByName("${target}Test") as? org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest? ?: continue
                                    val compileTestTask = tasks.findByName("compileTestKotlin${target.capitalize()}") ?: continue
                                    val compileMainTask = tasks.findByName("compileKotlin${target.capitalize()}") ?: continue

                                    targetTestTask.inputs.files(
                                        *compileTestTask.outputs.files.files.toTypedArray(),
                                        *compileMainTask.outputs.files.files.toTypedArray()
                                    )

                                    targetTestTask.dependsOn(taskName)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun Project.initSamples() {
        rootProject.samples {
            if (isWasmEnabled(project)) {
                configureWasm(ProjectType.EXECUTABLE, binaryen = false)
            }

            // @TODO: Move to KorGE plugin
            project.configureJvmRunJvm(isRootKorlibs = true)
            tasks.configureEach {
                if (!isWindows) {
                    afterEvaluate {
                        for (type in CrossExecType.VALID_LIST) {
                            for (deb in listOf("Debug", "Release")) {
                                val linkTask = project.tasks.findByName("link${deb}Executable${type.nameWithArchCapital}") as? KotlinNativeLink? ?: continue
                                tasks.createThis<Exec>("runNative${deb}${type.interpCapital}") {
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
                        }
                    }
                }
            }

            // Configure only multiplatform modules, exclude androidApp modules
            extensions.findByType(KotlinMultiplatformExtension::class.java)?.apply {
                jvm {
                }
                js(KotlinJsCompilerType.IR) {
                    browser {
                        binaries.executable()
                    }
                    configureJsTargetOnce()
                }

                tasks.getByName("jsProcessResources").apply {
                    //println(this.outputs.files.toList())
                    doLast {
                        val targetDir = this.outputs.files.first()
                        val jsMainCompilation: KotlinJsCompilation = js(KotlinJsCompilerType.IR).compilations.findByName("main")!!

                        // @TODO: How to get the actual .js file generated/served?
                        val jsFile = File("${project.name}.js").name
                        val resourcesFolders = jsMainCompilation.allKotlinSourceSets
                            .flatMap { it.resources.srcDirs } + listOf(
                                File(rootProject.rootDir, "_template"),
                                File(rootProject.rootDir, "buildSrc/src/main/resources"),
                            )
                        //println("jsFile: $jsFile")
                        //println("resourcesFolders: $resourcesFolders")
                        fun readTextFile(name: String): String {
                            for (folder in resourcesFolders) {
                                val file = File(folder, name)?.takeIf { it.exists() } ?: continue
                                return file.readText()
                            }
                            return ClassLoader.getSystemResourceAsStream(name)?.readBytes()?.toString(Charsets.UTF_8)
                                ?: error("We cannot find suitable '$name'")
                        }

                        val indexTemplateHtml = readTextFile("index.v2.template.html")
                        val customCss = readTextFile("custom-styles.template.css")
                        val customHtmlHead = readTextFile("custom-html-head.template.html")
                        val customHtmlBody = readTextFile("custom-html-body.template.html")

                        //println(File(targetDir, "index.html"))

                        File(targetDir, "index.html").writeText(
                            groovy.text.SimpleTemplateEngine().createTemplate(indexTemplateHtml).make(
                                mapOf(
                                    "OUTPUT" to jsFile,
                                    //"TITLE" to korge.name,
                                    "TITLE" to "TODO",
                                    "CUSTOM_CSS" to customCss,
                                    "CUSTOM_HTML_HEAD" to customHtmlHead,
                                    "CUSTOM_HTML_BODY" to customHtmlBody
                                )
                            ).toString()
                        )
                    }
                }
            }

            project.configureEsbuild()
            project.configureJavascriptRun()
            project.configureDenoRun()
        }
    }

    fun Project.initTests() {
        rootProject.subprojects {
            //tasks.withType(Test::class.java).allThis {
            afterEvaluate {
                it.configureTests()
                project.configureDenoTest()
            }
        }
    }

    fun Project.initCrossTests() {
        rootProject.subprojects {
            tasks.configureEach {
                for (type in CrossExecType.VALID_LIST) {
                    val linkDebugTest = project.tasks.findByName("linkDebugTest${type.nameWithArchCapital}") as? KotlinNativeLink?
                    if (linkDebugTest != null) {
                        tasks.createThis<KotlinNativeCrossTest>("${type.nameWithArch}Test${type.interpCapital}") {
                            val testResultsDir = project.buildDir.resolve(TestingBasePlugin.TEST_RESULTS_DIR_NAME)
                            val testReportsDir = project.extensions
                                .getByType(ReportingExtension::class.java)
                                .baseDirectory
                                .get().asFile
                                .resolve(TestingBasePlugin.TESTS_DIR_NAME)
                            //this.configureConventions()

                            val htmlReport = org.gradle.api.internal.plugins.DslObject(reports.html)
                            val xmlReport = org.gradle.api.internal.plugins.DslObject(reports.junitXml)
                            xmlReport.conventionMapping.map("destination") { testResultsDir.resolve(name) }
                            htmlReport.conventionMapping.map("destination") { testReportsDir.resolve(name) }

                            this.type = type
                            this.executable = linkDebugTest.binary.outputFile
                            this.workingDir = linkDebugTest.binary.outputDirectory.absolutePath
                            this.binaryResultsDirectory.set(testResultsDir.resolve("$name/binary"))
                            this.environment("WINEDEBUG", "-all")
                            group = "verification"
                            dependsOn(linkDebugTest)
                        }
                    }
                }
            }
        }
    }
}

//val headlessTests = true
//val headlessTests = System.getenv("NON_HEADLESS_TESTS") != "true"
val headlessTests: Boolean get() = System.getenv("CI") == "true" || System.getenv("HEADLESS_TESTS") == "true"
//val useMimalloc = false

val Project._libs: Dyn get() = rootProject.extensions.getByName("libs").dyn
val Project.kotlinVersion: String get() = _libs["versions"]["kotlin"].dynamicInvoke("get").casted()
val Project.nodeVersion: String get() = _libs["versions"]["node"].dynamicInvoke("get").casted()
val Project.androidBuildGradleVersion: String get() = _libs["versions"]["android"]["build"]["gradle"].dynamicInvoke("get").casted()
val Project.realKotlinVersion: String get() = (System.getenv("FORCED_KOTLIN_VERSION") ?: kotlinVersion)
val forcedVersion = System.getenv("FORCED_VERSION")

fun Project.getForcedVersion(): String {
    return forcedVersion
        ?.removePrefix("refs/tags/")
        ?.removePrefix("v")
        ?.removePrefix("w")
        ?.removePrefix("z")
        ?: project.version.toString()
}

fun Project.getProjectForcedVersion(): String {
    val res = when {
        this.name.startsWith("korge-gradle-plugin") -> getForcedVersionGradlePluginVersion()
        else -> getForcedVersionLibrariesVersion()
    }
    if (System.getenv("FORCED_VERSION") != null) {
        println(":: PROJECT: name=${project.name}, version=$res")
    }
    return res
}

fun Project.getForcedVersionGradlePluginVersion(): String {
    return getForcedVersion().substringBefore("-only-gradle-plugin-")
}

fun Project.getForcedVersionLibrariesVersion(): String {
    return getForcedVersion().substringAfter("-only-gradle-plugin-")
}

val Project.hasAndroidSdk by LazyExt { AndroidSdk.hasAndroidSdk(project) }
val Project.enabledSandboxResourceProcessor: Boolean get() = rootProject.findProperty("enabledSandboxResourceProcessor") == "true"

val Project.currentJavaVersion by LazyExt { currentJavaVersion() }
fun Project.hasBuildGradle() = listOf("build.gradle", "build.gradle.kts").any { File(projectDir, it).exists() }
val Project.isSample: Boolean get() = project.path.startsWith(":samples:") || project.path.startsWith(":korge-sandbox") || project.path.startsWith(":korge-editor") || project.path.startsWith(":korge-starter-kit")
/**
 * Whether the current project / subproject / module is the android app.
 *
 * This value is used to distinguish between android apps that require different setup due to
 * compatibility with multiplatform projects.
 *
 * Right now we only support module names that are named `androidApp`.
 */
val Project.isAndroidApp: Boolean get() = project.path.endsWith(":androidApp")
fun Project.mustAutoconfigureKMM(): Boolean =
    !project.name.startsWith("korge-gradle-plugin") &&
        project.name != "korge-reload-agent" &&
        project.name != "korge-ipc" &&
        project.name != "korge-kotlin-compiler" &&
        project.name != "korge-benchmarks" &&
        project.hasBuildGradle()

val Project.isKorgeBenchmarks: Boolean get() = path == ":korge-benchmarks"

fun Project.nonSamples(block: Project.() -> Unit) {
    subprojects {
        if (!project.isSample && project.hasBuildGradle()) {
            block()
        }
    }
}

fun Project.samples(block: Project.() -> Unit) {
    subprojects {
        if (project.isSample && project.hasBuildGradle()) {
            block()
        }
    }
}
fun Project.symlinktree(fromFolder: File, intoFolder: File) {
    try {
        if (!intoFolder.isDirectory && !Files.isSymbolicLink(intoFolder.toPath())) {
            runCatching { intoFolder.delete() }
            runCatching { intoFolder.deleteRecursively() }
            intoFolder.parentFile.mkdirs()
            val intoPath = intoFolder.toPath()
            val relativeFromPath = intoFolder.parentFile.toPath().relativize(fromFolder.toPath())
            //if (isWindows) {
            //    exec { it.commandLine("cmd", "/c", "mklink", "/d", intoPath.pathString, relativeFromPath.pathString) }
            //} else {
                Files.createSymbolicLink(intoPath, relativeFromPath)
            //}
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        copy {
            it.from(fromFolder)
            it.into(intoFolder)
            it.duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }
}

fun Project.runServer(blocking: Boolean, debug: Boolean = false) {
    if (_webServer == null) {
        val address = "0.0.0.0"
        val port = 8080
        val server = staticHttpServer(File(project.buildDir, "www"), address = address, port = port)
        _webServer = server
        try {
            val openAddress = when (address) {
                "0.0.0.0" -> "127.0.0.1"
                else -> address
            }
            val SUFFIX = if (debug) "?LOG_LEVEL=debug" else ""
            openBrowser("http://$openAddress:${server.port}/index.html$SUFFIX")
            if (blocking) {
                while (true) {
                    Thread.sleep(1000L)
                }
            }
        } finally {
            if (blocking) {
                println("Stopping web server...")
                server.server.stop(0)
                _webServer = null
            }
        }
    }
    _webServer?.updateVersion?.incrementAndGet()
}

fun Project.execOutput(vararg args: String): String {
    var out = ""
    ByteArrayOutputStream().also { os ->
        val result = execThis {
            commandLine(*args)
            standardOutput = os
        }
        result.assertNormalExitValue()
        out = os.toString()
    }
    return out
}


internal var _webServer: DecoratedHttpServer? = null

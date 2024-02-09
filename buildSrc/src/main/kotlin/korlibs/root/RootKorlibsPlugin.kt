package korlibs.root

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
import korlibs.korge.gradle.util.create
import korlibs.kotlin
import korlibs.modules.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.testing.*
import org.jetbrains.dokka.gradle.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import org.jetbrains.kotlin.gradle.targets.js.testing.*
import org.jetbrains.kotlin.gradle.targets.js.testing.karma.*
import org.jetbrains.kotlin.gradle.targets.js.testing.mocha.*
import org.jetbrains.kotlin.gradle.tasks.*
import java.io.*
import java.nio.file.*

object RootKorlibsPlugin {
    val KORGE_GROUP = "com.soywiz.korge"
    val KORGE_RELOAD_AGENT_GROUP = "com.soywiz.korge"
    val KORGE_GRADLE_PLUGIN_GROUP = "com.soywiz.korlibs.korge.plugins"

    @JvmStatic
    fun doInit(rootProject: Project) {
        rootProject.init()
        rootProject.afterEvaluate {
            rootProject.allprojectsThis {
                tasks.withType(Test::class.java) {
                    //it.ignoreFailures = true // This would cause the test to pass even if we have failing tests!
                }
            }
        }
    }

    fun Project.init() {
        plugins.apply(DokkaPlugin::class.java)

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
        configureMavenCentralRelease()
        initDuplicatesStrategy()
        initSymlinkTrees()
        initShowSystemInfoWhenLinkingInWindows()
        korlibs.korge.gradle.KorgeVersionsTask.registerShowKorgeVersions(project)
        initInstallAndCheckLinuxLibs()
        // Disabled by default, since it resolves configurations at configuration time
        if (System.getenv("ENABLE_KOVER") == "true") configureKover()
        initPublishing()
        initKMM()
        initShortcuts()
        initTests()
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
        allprojectsThis {
            project.version = getProjectForcedVersion()
        }
    }

    fun Project.initAllRepositories() {
        allprojectsThis {
            configureRepositories()
        }
    }

    fun Project.initGroupOverrides() {
        allprojectsThis {
            val projectName = project.name
            val firstComponent = projectName.substringBefore('-')
            group = RootKorlibsPlugin.KORGE_GROUP
        }
    }

    fun Project.initNodeJSFixes() {
        plugins.applyOnce<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin>()
        rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class.java, Action {
            rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().nodeVersion = project.nodeVersion
        })
        // https://youtrack.jetbrains.com/issue/KT-48273
        afterEvaluate {
            rootProject.extensions.configure(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension::class.java, Action {
                //it.versions.webpackDevServer.version = "4.0.0"
            })
        }
    }

    fun Project.initDuplicatesStrategy() {
        allprojectsThis {
            tasks.withType(Copy::class.java).allThis {
                //this.duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.WARN
                this.duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
                //println("Task $this")
            }
        }
    }

    fun Project.initSymlinkTrees() {
        //fileTree(new File(rootProject.projectDir, "buildSrc/src/main/kotlinShared"))
        //copy {
        project.symlinktree(
            fromFolder = File(rootProject.projectDir, "buildSrc/src/main/kotlin"),
            intoFolder = File(rootProject.projectDir, "korge-gradle-plugin/build/srcgen2")
        )

        project.symlinktree(
            fromFolder = File(rootProject.projectDir, "buildSrc/src/test/kotlin"),
            intoFolder = File(rootProject.projectDir, "korge-gradle-plugin/build/testgen2")
        )

        project.symlinktree(
            fromFolder = File(rootProject.projectDir, "buildSrc/src/main/resources"),
            intoFolder = File(rootProject.projectDir, "korge-gradle-plugin/build/srcgen2res")
        )

        project.symlinktree(
            fromFolder = File(rootProject.projectDir, "buildSrc/src/test/resources"),
            intoFolder = File(rootProject.projectDir, "korge-gradle-plugin/build/testgen2res")
        )
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
        plugins.apply("java")
        plugins.apply("kotlin-multiplatform")
        plugins.apply("signing")
        plugins.apply("maven-publish")
    }

    fun Project.initPublishing() {
        rootProject.afterEvaluate {
            rootProject.nonSamples {
                if (this.project.isKorgeBenchmarks) return@nonSamples

                plugins.apply("maven-publish")

                val doConfigure = mustAutoconfigureKMM()

                if (doConfigure) {
                    configurePublishing()
                    configureSigning()
                }
            }
        }
    }

    fun Project.initKMM() {
        rootProject.subprojectsThis {
            val doConfigure = mustAutoconfigureKMM()

            if (doConfigure) {
                val isSample = project.isSample
                val hasAndroid = doEnableKotlinAndroid && hasAndroidSdk && project.name != "korge-benchmarks"
                //val hasAndroid = !isSample && true
                val mustPublish = !isSample

                // AppData\Local\Android\Sdk\tools\bin>sdkmanager --licenses
                plugins.apply("kotlin-multiplatform")

                //initAndroidProject()
                if (hasAndroid) {
                    project.configureAndroidDirect(ProjectType.fromExecutable(isSample), isKorge = false)
                }

                if (isSample && supportKotlinNative && isMacos) {
                    project.configureNativeIos(projectType = ProjectType.EXECUTABLE)
                }

                if (!isSample && rootProject.plugins.hasPlugin("org.jetbrains.dokka")) {
                    plugins.apply("org.jetbrains.dokka")
                }

                if (mustPublish) {
                    plugins.apply("maven-publish")
                }

                tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).configureEach {
                    it.kotlinOptions.suppressWarnings = true
                }

                afterEvaluate {
                    val jvmTest = tasks.findByName("jvmTest")
                    if (jvmTest is org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest) {
                        val jvmTestFix = tasks.createThis<Test>("jvmTestFix") {
                            group = "verification"
                            environment("UPDATE_TEST_REF", "true")
                            testClassesDirs = jvmTest.testClassesDirs
                            classpath = jvmTest.classpath
                            bootstrapClasspath = jvmTest.bootstrapClasspath
                            if (!JvmAddOpens.beforeJava9) jvmArgs(*JvmAddOpens.createAddOpensTypedArray())
                            if (headlessTests) systemProperty("java.awt.headless", "true")
                        }
                        val jvmTestInteractive = tasks.createThis<Test>("jvmTestInteractive") {
                            group = "verification"
                            environment("INTERACTIVE_SCREENSHOT", "true")
                            testClassesDirs = jvmTest.testClassesDirs
                            classpath = jvmTest.classpath
                            bootstrapClasspath = jvmTest.bootstrapClasspath
                            if (!JvmAddOpens.beforeJava9) jvmArgs(*JvmAddOpens.createAddOpensTypedArray())
                        }
                        if (!JvmAddOpens.beforeJava9) jvmTest.jvmArgs(*JvmAddOpens.createAddOpensTypedArray())
                        if (headlessTests) jvmTest.systemProperty("java.awt.headless", "true")
                    }
                }

                kotlin {
                    //explicitApi()
                    //explicitApiWarning()

                    metadata {
                        compilations.allThis {
                            kotlinOptions.suppressWarnings = true
                        }
                    }
                    jvm {
                        compilations.allThis {
                            kotlinOptions.jvmTarget = GRADLE_JAVA_VERSION_STR
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

                            val npmProjectDir = wasmBrowserTest.compilation.npmProject.dir
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
                    js(org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.IR) {
                        browser {
                            compilations.allThis {
                                //kotlinOptions.sourceMap = true
                            }
                        }
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

                            @Suppress("SimplifyBooleanWithConstants")
                            if (
                                false
                                || project.name == "korlibs-time"
                                || project.name == "korlibs-crypto"
                                || project.name == "korlibs-concurrent"
                                || project.name == "korlibs-logger"
                                || project.name == "korlibs-datastructure"
                                || project.name == "korlibs-math-core"
                                || project.name == "korlibs-util"
                                || project.name == "korlibs-memory"
                                || project.name == "korlibs-platform"
                            ) {
                                val macos by lazy { createPairSourceSet("macos", darwin, project = project) }
                                val linux by lazy { createPairSourceSet("linux", posix, project = project) }
                                val mingw by lazy { createPairSourceSet("mingw", native, project = project) }

                                for (target in desktopTargets(project)) {
                                    val native = createPairSourceSet(target.name, project = project)
                                    when {
                                        target.isLinux -> native.dependsOn(linux)
                                        target.isMacos -> native.dependsOn(macos)
                                        target.isMingw -> native.dependsOn(mingw)
                                    }
                                }
                            }

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

                                    //println("$targetTestTask -> $target")

                                    tasks {
                                        create<Copy>(taskName) {
                                            for (sourceSet in kotlin.sourceSets) {
                                                from(sourceSet.resources)
                                            }

                                            into(targetTestTask.executable.parentFile)
                                        }
                                    }

                                    targetTestTask.inputs.files(
                                        *compileTestTask.outputs.files.files.toTypedArray(),
                                        *compileMainTask.outputs.files.files.toTypedArray()
                                    )

                                    targetTestTask.dependsOn(taskName)
                                    //println(".target=$target")
                                }
                            }
                        }
                    }
                }
            }
            project.afterEvaluate {
                project.addGenResourcesTasks()
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
            project.apply {

                project.tasks {
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

                    //val jsRun = createThis<Task>("jsRun") { dependsOn("jsBrowserDevelopmentRun") } // Already available
                    //val jvmRun = createThis("jvmRun") {
                    //    group = "run"
                    //    dependsOn(runJvm)
                    //}
                    //val run by getting(JavaExec::class)

                    //val processResources by getting {
                    //	dependsOn(processResourcesKorge)
                    //}
                }
            }

            kotlin {
                jvm {
                }
                js(org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.IR) {
                    browser {
                        binaries.executable()
                    }
                }

                tasks.getByName("jsProcessResources").apply {
                    //println(this.outputs.files.toList())
                    doLast {
                        val targetDir = this.outputs.files.first()
                        val jsMainCompilation: KotlinJsCompilation = kotlin.js(org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.IR).compilations.findByName("main")!!

                        // @TODO: How to get the actual .js file generated/served?
                        val jsFile = File("${project.name}.js").name
                        val resourcesFolders = jsMainCompilation.allKotlinSourceSets
                            .flatMap { it.resources.srcDirs } + listOf(File(rootProject.rootDir, "_template"))
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
        }
    }

    //println("currentJavaVersion=${korlibs.currentJavaVersion()}")
    ///

    fun Project.initShortcuts() {
        rootProject.subprojectsThis {
            afterEvaluate {
                tasks {
                    val publishKotlinMultiplatformPublicationToMavenLocal = "publishKotlinMultiplatformPublicationToMavenLocal"
                    val publishKotlinMultiplatformPublicationToMavenRepository = "publishKotlinMultiplatformPublicationToMavenRepository"

                    val publishJvmLocal = createThis<Task>("publishJvmLocal") {
                        if (findByName(publishKotlinMultiplatformPublicationToMavenLocal) != null) {
                            dependsOn("publishJvmPublicationToMavenLocal")
                            //dependsOn("publishMetadataPublicationToMavenLocal")
                            dependsOn(publishKotlinMultiplatformPublicationToMavenLocal)
                        } else if (findByName("publishToMavenLocal") != null) {
                            dependsOn("publishToMavenLocal")
                        }
                    }

                    val publishJsLocal = createThis<Task>("publishJsLocal") {
                        if (findByName(publishKotlinMultiplatformPublicationToMavenLocal) != null) {
                            dependsOn("publishJsPublicationToMavenLocal")
                            //dependsOn("publishMetadataPublicationToMavenLocal")
                            dependsOn(publishKotlinMultiplatformPublicationToMavenLocal)
                        }
                    }

                    val publishMacosX64Local = createThis<Task>("publishMacosX64Local") {
                        if (findByName(publishKotlinMultiplatformPublicationToMavenLocal) != null) {
                            dependsOn("publishMacosX64PublicationToMavenLocal")
                            dependsOn(publishKotlinMultiplatformPublicationToMavenLocal)
                        }
                    }
                    val publishMacosArm64Local = createThis<Task>("publishMacosArm64Local") {
                        if (findByName(publishKotlinMultiplatformPublicationToMavenLocal) != null) {
                            dependsOn("publishMacosArm64PublicationToMavenLocal")
                            dependsOn(publishKotlinMultiplatformPublicationToMavenLocal)
                        }
                    }
                    val publishIosX64Local = createThis<Task>("publishIosX64Local") {
                        if (findByName(publishKotlinMultiplatformPublicationToMavenLocal) != null) {
                            dependsOn("publishIosX64PublicationToMavenLocal")
                            dependsOn(publishKotlinMultiplatformPublicationToMavenLocal)
                        }
                    }
                    val publishIosArm64Local = createThis<Task>("publishIosArm64Local") {
                        if (findByName(publishKotlinMultiplatformPublicationToMavenLocal) != null) {
                            dependsOn("publishIosArm64PublicationToMavenLocal")
                            dependsOn(publishKotlinMultiplatformPublicationToMavenLocal)
                        }
                    }
                    val publishMobileLocal = createThis<Task>("publishMobileLocal") {
                        doFirst {
                            //if (currentJavaVersion != 8) error("To use publishMobileRepo, must be used Java8, but used Java$currentJavaVersion")
                        }
                        run {
                            val taskName = "publishJvmPublicationToMavenLocal"
                            if (findByName(taskName) != null) {
                                dependsOn(taskName)
                            }
                        }
                        if (findByName(publishKotlinMultiplatformPublicationToMavenLocal) != null) {
                            dependsOn(publishKotlinMultiplatformPublicationToMavenLocal)
                            dependsOn("publishAndroidPublicationToMavenLocal")
                            dependsOn("publishIosArm64PublicationToMavenLocal")
                            dependsOn("publishIosX64PublicationToMavenLocal")
                        }
                    }

                    val customMavenUser = rootProject.findProperty("KORLIBS_CUSTOM_MAVEN_USER")?.toString()
                    val customMavenPass = rootProject.findProperty("KORLIBS_CUSTOM_MAVEN_PASS")?.toString()
                    val customMavenUrl = rootProject.findProperty("KORLIBS_CUSTOM_MAVEN_URL")?.toString()
                    val customPublishEnabled = forcedVersion != null
                        && !customMavenUser.isNullOrBlank()
                        && !customMavenPass.isNullOrBlank()
                        && !customMavenUrl.isNullOrBlank()

                    val publishMobileRepo = createThis<Task>("publishMobileRepo") {
                        doFirst {
                            if (currentJavaVersion != 8) {
                                error("To use publishMobileRepo, must be used Java8, but used Java$currentJavaVersion")
                            }
                            if (!customPublishEnabled) {
                                error("To use publishMobileRepo, must set `FORCED_VERSION=...` environment variable, and in ~/.gradle/gradle.properties : KORLIBS_CUSTOM_MAVEN_USER, KORLIBS_CUSTOM_MAVEN_PASS & KORLIBS_CUSTOM_MAVEN_URL")
                            }
                        }
                        if (customPublishEnabled) {
                            run {
                                val taskName = "publishJvmPublicationToMavenRepository"
                                if (findByName(taskName) != null) {
                                    dependsOn(taskName)
                                }
                            }
                            if (findByName(publishKotlinMultiplatformPublicationToMavenRepository) != null) {
                                dependsOn(publishKotlinMultiplatformPublicationToMavenRepository)
                                dependsOn("publishAndroidPublicationToMavenRepository")
                                dependsOn("publishIosArm64PublicationToMavenRepository")
                                dependsOn("publishIosX64PublicationToMavenRepository")
                            }
                        }
                    }
                }
            }
        }
    }

    fun Project.initTests() {
        rootProject.subprojectsThis {
            //tasks.withType(Test::class.java).allThis {
            afterEvaluate {
                it.configureTests()
            }
        }
    }

    fun Project.initCrossTests() {
        rootProject.subprojectsThis {
            afterEvaluate {
                tasks {
                    afterEvaluate {
                        for (type in CrossExecType.VALID_LIST) {
                            val linkDebugTest = project.tasks.findByName("linkDebugTest${type.nameWithArchCapital}") as? KotlinNativeLink?
                            if (linkDebugTest != null) {
                                tasks.createThis<KotlinNativeCrossTest>("${type.nameWithArch}Test${type.interpCapital}") {
                                    val link = linkDebugTest
                                    val testResultsDir = project.buildDir.resolve(org.gradle.testing.base.plugins.TestingBasePlugin.TEST_RESULTS_DIR_NAME)
                                    val testReportsDir = project.extensions.getByType(org.gradle.api.reporting.ReportingExtension::class.java).baseDir.resolve(org.gradle.testing.base.plugins.TestingBasePlugin.TESTS_DIR_NAME)
                                    //this.configureConventions()

                                    val htmlReport = org.gradle.api.internal.plugins.DslObject(reports.html)
                                    val xmlReport = org.gradle.api.internal.plugins.DslObject(reports.junitXml)
                                    xmlReport.conventionMapping.map("destination") { testResultsDir.resolve(name) }
                                    htmlReport.conventionMapping.map("destination") { testReportsDir.resolve(name) }

                                    this.type = type
                                    this.executable = link.binary.outputFile
                                    this.workingDir = link.binary.outputDirectory.absolutePath
                                    this.binaryResultsDirectory.set(testResultsDir.resolve("$name/binary"))
                                    this.environment("WINEDEBUG", "-all")
                                    group = "verification"
                                    dependsOn(link)
                                }
                            }
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
fun Project.mustAutoconfigureKMM(): Boolean =
    !project.name.startsWith("korge-gradle-plugin") &&
        project.name != "korge-reload-agent" &&
        project.name != "korge-benchmarks" &&
        project.hasBuildGradle()

val Project.isKorgeBenchmarks: Boolean get() = path == ":korge-benchmarks"

fun Project.nonSamples(block: Project.() -> Unit) {
    subprojectsThis {
        if (!project.isSample && project.hasBuildGradle()) {
            block()
        }
    }
}

fun Project.samples(block: Project.() -> Unit) {
    subprojectsThis {
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
            Files.createSymbolicLink(intoFolder.toPath(), intoFolder.parentFile.toPath().relativize(fromFolder.toPath()))
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

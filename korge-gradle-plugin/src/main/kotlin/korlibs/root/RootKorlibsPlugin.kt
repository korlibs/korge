package korlibs.root

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import korlibs.allThis
import korlibs.currentJavaVersion
import korlibs.korge.gradle.addGenResourcesTasks
import korlibs.korge.gradle.configureBuildScriptClasspathTasks
import korlibs.korge.gradle.configureRepositories
import korlibs.korge.gradle.targets.CrossExecType
import korlibs.korge.gradle.targets.ProjectType
import korlibs.korge.gradle.targets.all.AddFreeCompilerArgs
import korlibs.korge.gradle.targets.all.rootEnableFeaturesOnAllTargets
import korlibs.korge.gradle.targets.android.AndroidSdk
import korlibs.korge.gradle.targets.android.GRADLE_JAVA_VERSION_STR
import korlibs.korge.gradle.targets.android.configureAndroidDirect
import korlibs.korge.gradle.targets.createPairSourceSet
import korlibs.korge.gradle.targets.ios.configureNativeIos
import korlibs.korge.gradle.targets.isIos
import korlibs.korge.gradle.targets.isLinux
import korlibs.korge.gradle.targets.isMacos
import korlibs.korge.gradle.targets.isTvos
import korlibs.korge.gradle.targets.isWindows
import korlibs.korge.gradle.targets.js.configureDenoRun
import korlibs.korge.gradle.targets.js.configureDenoTest
import korlibs.korge.gradle.targets.js.configureEsbuild
import korlibs.korge.gradle.targets.js.configureJSTestsOnce
import korlibs.korge.gradle.targets.js.configureJavascriptRun
import korlibs.korge.gradle.targets.js.configureJsTargetOnce
import korlibs.korge.gradle.targets.js.configureWasm
import korlibs.korge.gradle.targets.jvm.JvmAddOpens
import korlibs.korge.gradle.targets.jvm.configureJvmRunJvm
import korlibs.korge.gradle.targets.native.KotlinNativeCrossTest
import korlibs.korge.gradle.targets.native.commandLineCross
import korlibs.korge.gradle.targets.native.configureKotlinNativeTarget
import korlibs.korge.gradle.targets.supportKotlinNative
import korlibs.korge.gradle.targets.wasm.configureWasmTarget
import korlibs.korge.gradle.targets.wasm.isWasmEnabled
import korlibs.korge.gradle.util.DecoratedHttpServer
import korlibs.korge.gradle.util.Dyn
import korlibs.korge.gradle.util.LDLibraries
import korlibs.korge.gradle.util.LazyExt
import korlibs.korge.gradle.util.applyOnce
import korlibs.korge.gradle.util.checkMinimumJavaVersion
import korlibs.korge.gradle.util.create
import korlibs.korge.gradle.util.createThis
import korlibs.korge.gradle.util.dyn
import korlibs.korge.gradle.util.execThis
import korlibs.korge.gradle.util.openBrowser
import korlibs.korge.gradle.util.staticHttpServer
import korlibs.korge.gradle.util.takeIfExists
import korlibs.kotlin
import korlibs.modules.configurePublishing
import korlibs.modules.configureTests
import korlibs.modules.doEnableKotlinAndroid
import korlibs.modules.doEnableKotlinMobile
import korlibs.modules.mobileTargets
import korlibs.tasks
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.reporting.ReportingExtension
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.base.plugins.TestingBasePlugin
import org.jetbrains.dokka.gradle.AbstractDokkaTask
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.karma.KotlinKarma
import org.jetbrains.kotlin.gradle.targets.js.testing.mocha.KotlinMocha
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink

object RootKorlibsPlugin {
    val KORGE_GROUP = "org.korge.engine"
    val KORGE_RELOAD_AGENT_GROUP = "org.korge.engine"
    val KORGE_GRADLE_PLUGIN_GROUP = "org.korge.gradleplugins"

    @JvmStatic
    fun doInit(rootProject: Project) = with(rootProject) {
        plugins.apply(DokkaPlugin::class.java)

        allprojects {
            tasks.withType(AbstractDokkaTask::class.java).configureEach {
                offlineMode.set(true)
            }
        }

        checkMinimumJavaVersion()
        configureBuildScriptClasspathTasks()
        initPlugins()
        initRootKotlinJvmTarget()
        initVersions()
        initAllRepositories()
        initGroupOverrides()
        initNodeJSFixes()
        initDuplicatesStrategy()
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
            // Forced Java21 toolchain
//            jvmToolchain { (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of("21")) }
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
            group = KORGE_GROUP
        }
    }

    fun Project.initNodeJSFixes() {
        plugins.applyOnce<NodeJsRootPlugin>()
        rootProject.plugins.withType(NodeJsPlugin::class.java, Action {
            rootProject.extensions.configure(NodeJsEnvSpec::class.java, Action {
                version.set(project.nodeVersion)
                download.set(true)
            })
        })
    }

    fun Project.initDuplicatesStrategy() {
        allprojects {
            tasks.withType(Copy::class.java).allThis {
                this.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            }
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
                val hasAndroid = doEnableKotlinAndroid && hasAndroidSdk && project.name != "korge-benchmarks"

                // AppData\Local\Android\Sdk\tools\bin>sdkmanager --licenses
                plugins.apply("kotlin-multiplatform")

                if (hasAndroid) {
                    project.configureAndroidDirect(ProjectType.fromExecutable(isSample), isKorge = false)
                }

                if (isSample && supportKotlinNative && isMacos) {
                    project.configureNativeIos(projectType = ProjectType.EXECUTABLE)
                }

                if (!isSample && rootProject.plugins.hasPlugin("org.jetbrains.dokka")) {
                    plugins.apply("org.jetbrains.dokka")
                }

                tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).configureEach {
                    compilerOptions.suppressWarnings.set(true)
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
                            // Suppress warnings is configured globally for compile tasks above.
                        }
                    }
                    jvm {
                        compilations.allThis {
                            compileTaskProvider.configure {
                                (this as org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile)
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
                    js(org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.IR) {
                        browser()
                        configureJsTargetOnce()
                        configureJSTestsOnce()
                    }

                    tasks.withType(KotlinJsTest::class.java).configureEach {
                        onTestFrameworkSet {
                            when (this) {
                                is KotlinMocha -> {
                                    timeout = "20s"
                                }
                                is KotlinKarma -> {
                                    File(rootProject.rootDir, "karma.config.d").takeIfExists()?.let {
                                        useConfigDirectory(it)
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
                                        implementation(kotlin("test-junit"))
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
                }
            }

            kotlin {
                jvm {
                }
                js(org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.IR) {
                    browser {
                        binaries.executable()
                    }
                    configureJsTargetOnce()
                }

                tasks.getByName("jsProcessResources").apply {
                    doLast {
                        val targetDir = this.outputs.files.first()
                        val jsMainCompilation: KotlinJsCompilation = kotlin.js(org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.IR).compilations.findByName("main")!!

                        // @TODO: How to get the actual .js file generated/served?
                        val jsFile = File("${project.name}.js").name
                        val resourcesFolders = jsMainCompilation.allKotlinSourceSets
                            .flatMap { it.resources.srcDirs } + listOf(
                                File(rootProject.rootDir, "_template"),
                                File(rootProject.rootDir, "buildSrc/src/main/resources"),
                            )
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

    fun Project.initShortcuts() {
        rootProject.subprojects {
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
        rootProject.subprojects {
            afterEvaluate {
                configureTests()
                project.configureDenoTest()
            }
        }
    }

    fun Project.initCrossTests() {
        rootProject.subprojects {
            afterEvaluate {
                tasks {
                    afterEvaluate {
                        for (type in CrossExecType.VALID_LIST) {
                            val linkDebugTest = project.tasks.findByName("linkDebugTest${type.nameWithArchCapital}") as? KotlinNativeLink?
                            if (linkDebugTest != null) {
                                tasks.createThis<KotlinNativeCrossTest>("${type.nameWithArch}Test${type.interpCapital}") {
                                    val testResultsDir = project.layout.buildDirectory.dir(TestingBasePlugin.TEST_RESULTS_DIR_NAME).get().asFile
                                    val testReportsDir = project.extensions
                                        .getByType(ReportingExtension::class.java)
                                        .baseDirectory
                                        .get().asFile
                                        .resolve(TestingBasePlugin.TESTS_DIR_NAME)

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
    }
}

val headlessTests: Boolean get() = System.getenv("CI") == "true" || System.getenv("HEADLESS_TESTS") == "true"

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
            Files.createSymbolicLink(intoPath, relativeFromPath)
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        copy {
            from(fromFolder)
            into(intoFolder)
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
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

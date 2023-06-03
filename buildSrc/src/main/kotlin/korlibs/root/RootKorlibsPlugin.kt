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
import korlibs.korge.gradle.util.*
import korlibs.korge.gradle.util.create
import korlibs.kotlin
import korlibs.modules.*
import org.gradle.api.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.testing.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.js.ir.*
import org.jetbrains.kotlin.gradle.targets.js.npm.*
import org.jetbrains.kotlin.gradle.targets.js.testing.*
import org.jetbrains.kotlin.gradle.targets.js.testing.karma.*
import org.jetbrains.kotlin.gradle.tasks.*
import java.io.*
import java.nio.file.*

object RootKorlibsPlugin {
    @JvmStatic
    fun doInit(rootProject: Project) {
        rootProject.init()
        rootProject.afterEvaluate {
            rootProject.allprojectsThis {
                tasks.withType(Test::class.java) {
                    it.ignoreFailures = true
                }
            }
        }
    }

    fun Project.init() {
        plugins.apply("org.jetbrains.dokka")

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
        initPatchTests()
        initSamples()
        //subprojectsThis {
        //    tasks.withType(Copy::class.java) {
        //        println("$it : ${it.outputs.files.files}")
        //        if (it.name == "jsProcessResources") {
        //            println(it.outputs.files.files)
        //        }
        //    }
        //}
        //korgeCheckVersion() // Do not check on the development plugin
    }

    fun Project.initPatchTests() {
        subprojectsThis {
            if (this.name == "korge") {
                configureMingwX64TestWithMesa()
            }
        }
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
            project.version = forcedVersion?.removePrefix("refs/tags/")?.removePrefix("v")?.removePrefix("w")
                ?: project.version
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
            group = when {
                projectName == "korge-gradle-plugin" -> "com.soywiz.korlibs.korge.plugins"
                projectName == "korge-reload-agent" -> "com.soywiz.korlibs.korge.reloadagent"
                firstComponent == "korge" -> "com.soywiz.korlibs.korge2"
                else -> "com.soywiz.korlibs.$firstComponent"
            }
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
                it.versions.webpackDevServer.version = "4.0.0"
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

        rootProject.afterEvaluate {
            subprojectsThis {
                val linkDebugTestMingwX64 = project.tasks.findByName("linkDebugTestMingwX64")
                if (linkDebugTestMingwX64 != null && isWindows && inCI) {
                    linkDebugTestMingwX64.configureGCAndSystemInfo()
                }

                val mingwX64Test = project.tasks.findByName("mingwX64Test")
                if (mingwX64Test != null && isWindows && inCI) {
                    mingwX64Test.configureGCAndSystemInfo()
                }
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

                if (isSample && doEnableKotlinNative && isMacos) {
                    project.configureNativeIos(projectType = ProjectType.EXECUTABLE)
                }

                if (!isSample && rootProject.plugins.hasPlugin("org.jetbrains.dokka")) {
                    plugins.apply("org.jetbrains.dokka")

                    /*
                    tasks.dokkaHtml.configure {
                        offlineMode.set(true)
                    }

                    dokkaHtml {
                        // Used to prevent resolving package-lists online. When this option is set to true, only local files are resolved
                        offlineMode.set(true)
                    }

                    tasks {
                        val dokkaCopy = createThis<Task>("dokkaCopy") {
                            dependsOn("dokkaHtml")
                            doLast {
                                val ffrom = File(project.buildDir, "dokka/html")
                                val finto = File(project.rootProject.projectDir, "build/dokka-all/${project.name}")
                                copy {
                                    from(ffrom)
                                    into(finto)
                                }
                                File(finto, "index-redirect.html").writeText("<meta http-equiv=\"refresh\" content=\"0; url=${project.name}\">\n")
                            }
                        }
                    }

                     */
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
                            kotlinOptions.jvmTarget = ANDROID_JAVA_VERSION_STR
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
                    if (project.findProperty("enable.wasm") == "true") {
                        wasm {
                            //this.
                            //this.applyBinaryen()
                            //nodejs { commonWebpackConfig { experiments = mutableSetOf("topLevelAwait") } }
                            //browser { commonWebpackConfig { experiments = mutableSetOf("topLevelAwait") } }
                            browser {
                                commonWebpackConfig { experiments = mutableSetOf("topLevelAwait") }
                                //testTask {
                                //    it.useKarma {
                                //        //useChromeHeadless()
                                //        this.webpackConfig.configDirectory = File(rootProject.rootDir, "karma.config.d")
                                //    }
                                //}
                            }
                        }
                        val wasmBrowserTest = tasks.getByName("wasmBrowserTest") as KotlinJsTest
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
                            //println("onTestFrameworkSet: $it")
                            if (framework is KotlinKarma) {
                                File(rootProject.rootDir, "karma.config.d").takeIfExists()?.let {
                                    framework.useConfigDirectory(it)
                                }
                            }
                        }
                    }

                    val desktopAndMobileTargets = ArrayList<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().apply {
                        if (doEnableKotlinNative) addAll(nativeTargets(project))
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

                        val common = createPairSourceSet("common") { test ->
                            dependencies {
                                if (test) {
                                    implementation(kotlin("test-common"))
                                    implementation(kotlin("test-annotations-common"))
                                } else {
                                    implementation(kotlin("stdlib-common"))
                                }
                            }
                        }

                        val concurrent = createPairSourceSet("concurrent", common)
                        val jvmAndroid = createPairSourceSet("jvmAndroid", concurrent)

                        // Default source set for JVM-specific sources and dependencies:
                        // JVM-specific tests and their dependencies:
                        val jvm = createPairSourceSet("jvm", jvmAndroid) { test ->
                            dependencies {
                                if (test) {
                                    implementation(kotlin("test-junit"))
                                } else {
                                    implementation(kotlin("stdlib-jdk8"))
                                }
                            }
                        }

                        if (hasAndroid) {
                            val android = createPairSourceSet("android", jvmAndroid) { test ->
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

                        val js = createPairSourceSet("js", common) { test ->
                            dependencies {
                                if (test) {
                                    implementation(kotlin("test-js"))
                                } else {
                                    implementation(kotlin("stdlib-js"))
                                }
                            }
                        }

                        val wasm = createPairSourceSet("wasm", common) { test ->
                            dependencies {
                                if (test) {
                                    implementation(kotlin("test-wasm"))
                                } else {
                                    implementation(kotlin("stdlib-wasm"))
                                }
                            }
                        }

                        if (doEnableKotlinNative) {
                            val native by lazy { createPairSourceSet("native", concurrent) }
                            val posix by lazy { createPairSourceSet("posix", native) }
                            val darwin by lazy { createPairSourceSet("darwin", posix) }
                            val iosMacos by lazy { createPairSourceSet("iosMacos", darwin) }

                            val linux by lazy { createPairSourceSet("linux", posix) }
                            val macos by lazy { createPairSourceSet("macos", iosMacos) }
                            val mingw by lazy { createPairSourceSet("mingw", native) }

                            val nativeTargets = nativeTargets(project)

                            for (target in nativeTargets) {
                                val native = createPairSourceSet(target.name)
                                when {
                                    target.isWin -> native.dependsOn(mingw)
                                    target.isMacos -> native.dependsOn(macos)
                                    target.isLinux -> native.dependsOn(linux)
                                }
                            }

                            val darwinMobile by lazy { createPairSourceSet("darwinMobile", darwin) }
                            val iosTvos by lazy { createPairSourceSet("iosTvos", darwinMobile) }
                            val watchos by lazy { createPairSourceSet("watchos", darwinMobile) }
                            val tvos by lazy { createPairSourceSet("tvos", iosTvos) }
                            val ios by lazy { createPairSourceSet("ios", iosTvos, iosMacos) }

                            for (target in mobileTargets(project)) {
                                val native = createPairSourceSet(target.name)
                                when {
                                    target.isIos -> native.dependsOn(ios)
                                    target.isWatchos -> native.dependsOn(watchos)
                                    target.isTvos -> native.dependsOn(tvos)
                                }
                            }

                            /*
                            for (baseName in listOf(
                                "nativeInteropMain",
                                "posixInteropMain",
                                "darwinInteropMain",
                                "linuxInteropMain",
                            )) {
                                val nativeInteropMainFolder = file("src/$baseName/kotlin")
                                if (nativeInteropMainFolder.isDirectory) {
                                    val currentNativeTarget = currentPlatformNativeTarget(project)
                                    // @TODO: Copy instead of use the same source folder
                                    for (target in allNativeTargets(project)) {
                                        if (baseName.contains("posix", ignoreCase = true) && !target.isPosix) continue
                                        if (baseName.contains("darwin", ignoreCase = true) && !target.isApple) continue
                                        if (baseName.contains("linux", ignoreCase = true) && !target.isLinux) continue

                                        val sourceSet = this@sourceSets.maybeCreate("${target.name}Main")
                                        val folder = when {
                                            target == currentNativeTarget -> nativeInteropMainFolder
                                            else -> {
                                                file("build/${baseName}Copy${target.name}").also { outFolder ->
                                                    outFolder.mkdirs()
                                                    sync {
                                                        from(nativeInteropMainFolder)
                                                        into(outFolder)
                                                    }
                                                }
                                            }
                                        }
                                        sourceSet.kotlin.srcDir(folder)
                                    }
                                }
                            }
                            */

                            // Copy test resources
                            afterEvaluate {
                                for (targetV in (nativeTargets + listOf(iosX64(), iosSimulatorArm64()))) {
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
                                    if (target == "mingwX64") {
                                        afterEvaluate {
                                            afterEvaluate {
                                                tasks.findByName("mingwX64TestWine")?.let {
                                                    //println("***************++")
                                                    it?.dependsOn(taskName)
                                                }
                                            }
                                        }
                                    }
                                    if (target == "linuxX64") {
                                        afterEvaluate {
                                            afterEvaluate {
                                                tasks.findByName("linuxX64TestLima")?.let {
                                                    //println("***************++")
                                                    it?.dependsOn(taskName)
                                                }
                                            }
                                        }
                                    }
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
            if (project.findProperty("enable.wasm") == "true") {
                kotlin {
                    wasm {
                        binaries.executable()
                        browser {
                            this.distribution {
                            }
                            //testTask {
                            //    it.useKarma {
                            //        //useChromeHeadless()
                            //        this.webpackConfig.configDirectory = File(rootProject.rootDir, "karma.config.d")
                            //    }
                            //}
                        }
                    }
                }
                project.tasks.createThis<Task>("wasmCreateIndex") {
                    doFirst {
                        val compilation = kotlin.wasm().compilations["main"]!!
                        val npmDir = compilation.npmProject.dir
                        File(npmDir, "kotlin/index.html").writeText(
                            """
                                <html>
                                    <script type = 'module'>
                                        import { instantiate } from "./${npmDir.name}.uninstantiated.mjs"
                                        instantiate();
                                    </script>
                                </html>
                            """.trimIndent()
                        )
                    }
                }
                project.tasks.findByName("wasmBrowserDevelopmentRun")?.dependsOn("wasmCreateIndex")
                val task = project.tasks.createThis<Task>("runWasm") {
                    dependsOn("wasmRun")
                }
            }
            // @TODO: Patch, because runDebugReleaseExecutableMacosArm64 is not created!
            if (isMacos && isArm && doEnableKotlinNative) {
                project.afterEvaluate {
                    project.tasks {
                        for (kind in listOf("Debug", "Release")) {
                            val linkTaskName = "link${kind}ExecutableMacosArm64"
                            val runTaskName = "run${kind}ExecutableMacosArm64"
                            val tryLinkTask = project.tasks.findByName(linkTaskName)
                            val linkExecutableMacosArm64 = tryLinkTask as? org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink?
                                ?: error("$linkTaskName ($tryLinkTask) is not a KotlinNativeLink task in project $project")
                            if (project.tasks.findByName(runTaskName) == null) {
                                val runExecutableMacosArm64 = project.tasks.createThis<Exec>(runTaskName) {
                                    dependsOn(linkExecutableMacosArm64)
                                    group = "run"
                                    commandLine(linkExecutableMacosArm64.binary.outputFile)
                                }
                            }
                        }
                    }
                }
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

                if (doEnableKotlinNative) {
                    for (target in nativeTargets(project)) {
                        target.apply {
                            binaries {
                                executable {
                                    entryPoint("entrypoint.main")
                                }
                            }
                        }
                    }

                    val nativeDesktopFolder = File(project.buildDir, "platforms/nativeDesktop")
                    //val nativeDesktopEntryPointSourceSet = kotlin.sourceSets.create("nativeDesktopEntryPoint")
                    //nativeDesktopEntryPointSourceSet.kotlin.srcDir(nativeDesktopFolder)
                    sourceSets.getByName("nativeMain") { it.kotlin.srcDir(nativeDesktopFolder) }

                    val createEntryPointAdaptorNativeDesktop = tasks.createThis<Task>("createEntryPointAdaptorNativeDesktop") {
                        val mainEntrypointFile = File(nativeDesktopFolder, "entrypoint/main.kt")

                        outputs.file(mainEntrypointFile)

                        // @TODO: Determine the package of the main file
                        doLast {
                            mainEntrypointFile.also { it.parentFile.mkdirs() }.writeText("""
                                package entrypoint
        
                                import kotlinx.coroutines.*
                                import main
        
                                fun main(args: Array<String>) {
                                    runBlocking {
                                        main()
                                    }
                                }
                            """.trimIndent())
                        }
                    }

                    //for (target in nativeDesktopTargets) {
                    //target.compilations["main"].defaultSourceSet.dependsOn(nativeDesktopEntryPointSourceSet)
                    //    target.compilations["main"].defaultSourceSet.kotlin.srcDir(nativeDesktopFolder)
                    //}

                    for (target in nativeTargets(project)) {
                        for (binary in target.binaries) {
                            val compilation = binary.compilation
                            val copyResourcesTask = tasks.createThis<Copy>("copyResources${target.name.capitalize()}${binary.name.capitalize()}") {
                                //dependsOn(getKorgeProcessResourcesTaskName(target, compilation))
                                group = "resources"
                                val isDebug = binary.buildType == org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType.DEBUG
                                val isTest = binary.outputKind == org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind.TEST
                                val compilation = if (isTest) target.compilations.findByName("test")!! else target.compilations.findByName("main")!!
                                //target.compilations.first().allKotlinSourceSets
                                val sourceSet = compilation.defaultSourceSet
                                from(sourceSet.resources)
                                from(sourceSet.dependsOn.map { it.resources })
                                into(binary.outputDirectory)
                            }

                            //compilation.compileKotlinTask.dependsOn(copyResourcesTask)
                            binary.linkTask.dependsOn(copyResourcesTask)
                            binary.compilation.compileKotlinTask.dependsOn(createEntryPointAdaptorNativeDesktop)
                        }
                    }
                }
            }

            project.configureEsbuild()
            project.configureJavascriptRun()
        }
    }

    fun runNativeTaskNameWin(kind: String): String {
        return "run${kind}ExecutableMingwX64"
    }

    fun runNativeTaskName(kind: String): String {
        return when {
            isWindows -> runNativeTaskNameWin(kind)
            isMacos -> if (isArm) "run${kind}ExecutableMacosArm64" else "run${kind}ExecutableMacosX64"
            else -> "run${kind}ExecutableLinuxX64"
        }
    }

    fun Task.dependsOnNativeTask(kind: String) {
        dependsOn(runNativeTaskName(kind))
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

val Project.hasAndroidSdk by LazyExt { AndroidSdk.hasAndroidSdk(project) }
val Project.enabledSandboxResourceProcessor: Boolean get() = rootProject.findProperty("enabledSandboxResourceProcessor") == "true"

val Project.currentJavaVersion by LazyExt { currentJavaVersion() }
fun Project.hasBuildGradle() = listOf("build.gradle", "build.gradle.kts").any { File(projectDir, it).exists() }
val Project.isSample: Boolean get() = project.path.startsWith(":samples:") || project.path.startsWith(":korge-sandbox") || project.path.startsWith(":korge-editor") || project.path.startsWith(":korge-starter-kit")
fun Project.mustAutoconfigureKMM(): Boolean =
    project.name != "korge-gradle-plugin" &&
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

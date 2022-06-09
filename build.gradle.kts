import com.soywiz.korlibs.modules.*
import com.soywiz.korge.gradle.util.*
import com.soywiz.korge.gradle.targets.*
import com.soywiz.korge.gradle.targets.android.AndroidSdk
import com.soywiz.korge.gradle.targets.ios.configureNativeIos
import com.soywiz.korge.gradle.targets.jvm.JvmAddOpens
import org.gradle.kotlin.dsl.kotlin
import java.io.File
import java.nio.file.Files
import com.soywiz.korlibs.modules.*
import kotlin.io.path.relativeTo

buildscript {
    val kotlinVersion: String = libs.versions.kotlin.get()

    val androidBuildGradleVersion =
        if (System.getProperty("java.version").startsWith("1.8") || System.getProperty("java.version").startsWith("9")) {
            "4.2.0"
        } else {
            libs.versions.android.build.gradle.get()
        }

    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven { url = uri("https://plugins.gradle.org/m2/") }
        if (kotlinVersion.contains("eap") || kotlinVersion.contains("-")) {
            maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/temporary")
            maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
            maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven")
        }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    }
    dependencies {
        classpath(libs.gradle.publish.plugin)
        classpath("com.android.tools.build:gradle:$androidBuildGradleVersion")
    }
}

plugins {
	java
    kotlin("multiplatform")
    id("org.jetbrains.kotlinx.kover") version "0.5.0" apply false
    id("org.jetbrains.dokka") version "1.6.10" apply false
    signing
    `maven-publish`
}

//val headlessTests = true
val headlessTests = System.getenv("NON_HEADLESS_TESTS") != "true"
val useMimalloc = true
//val useMimalloc = false

val kotlinVersion: String = libs.versions.kotlin.get()
val realKotlinVersion = (System.getenv("FORCED_KOTLIN_VERSION") ?: kotlinVersion)
val nodeVersion: String = libs.versions.node.get()
val androidBuildGradleVersion: String = libs.versions.android.build.gradle.get()

//println(KotlinVersion.CURRENT)

val forcedVersion = System.getenv("FORCED_VERSION")

allprojects {
    project.version = forcedVersion?.removePrefix("refs/tags/")?.removePrefix("v")?.removePrefix("w")
        ?: project.version
}

allprojects {
	repositories {
        mavenLocal().content { excludeGroup("Kotlin/Native") }
		mavenCentral().content { excludeGroup("Kotlin/Native") }
        google().content { excludeGroup("Kotlin/Native") }
		maven { url = uri("https://plugins.gradle.org/m2/") }.content { excludeGroup("Kotlin/Native") }
        if (kotlinVersion.contains("eap") || kotlinVersion.contains("-")) {
            maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/temporary").content { excludeGroup("Kotlin/Native") }
            maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev").content { excludeGroup("Kotlin/Native") }
            maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven").content { excludeGroup("Kotlin/Native") }
        }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }.content { excludeGroup("Kotlin/Native") }
	}
}

val hasAndroidSdk by lazy { AndroidSdk.hasAndroidSdk(project) }

// Required by RC
kotlin {
    // Forced Java8 toolchain
    //jvmToolchain { (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of("8")) }
    jvm { }
}


//apply(from = "${rootProject.rootDir}/build.idea.gradle")

fun Project.hasBuildGradle() = listOf("build.gradle", "build.gradle.kts").any { File(projectDir, it).exists() }
val Project.isSample: Boolean get() = project.path.startsWith(":samples:") || project.path.startsWith(":korge-sandbox") || project.path.startsWith(":korge-editor") || project.path.startsWith(":korge-starter-kit")

allprojects {
    if (project.hasBuildGradle()) {
        apply(from = "${rootProject.rootDir}/build.idea.gradle")
    }
    val projectName = project.name
    val firstComponent = projectName.substringBefore('-')
    group = when {
        projectName == "korge-gradle-plugin" -> "com.soywiz.korlibs.korge.plugins"
        firstComponent == "korge" -> "com.soywiz.korlibs.korge2"
        else -> "com.soywiz.korlibs.$firstComponent"
    }
}

rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().nodeVersion = nodeVersion
}

subprojects {
    val doConfigure =
            project.name != "korge-gradle-plugin" &&
            project.hasBuildGradle()

    if (doConfigure) {
        val isSample = project.isSample
        val hasAndroid = doEnableKotlinAndroid && hasAndroidSdk
        //val hasAndroid = !isSample && true
        val mustPublish = !isSample

        // AppData\Local\Android\Sdk\tools\bin>sdkmanager --licenses
        apply(plugin = "kotlin-multiplatform")

        if (hasAndroid) {
            if (isSample) {
                apply(plugin = "com.android.application")
            } else {
                apply(plugin = "com.android.library")
            }
            apply(from = "${rootProject.rootDir}/build.android.gradle")
            if (isSample) {
                apply(from = "${rootProject.rootDir}/build.android.application.gradle")
            }
        }

        if (isSample && doEnableKotlinNative && com.soywiz.korge.gradle.targets.isMacos) {
            configureNativeIos()
        }

        if (!isSample && rootProject.plugins.hasPlugin("org.jetbrains.dokka")) {
            apply(plugin = "org.jetbrains.dokka")

            tasks {
                val dokkaCopy by creating(Task::class) {
                    dependsOn("dokkaHtml")
                    doLast {
                        val ffrom = File(project.buildDir, "dokka/html")
                        val finto = File(project.rootProject.projectDir, "build/dokka-all/${project.name}")
                        copy {
                            from(ffrom)
                            into(finto)
                        }
                        File(finto, "index.html").writeText("<meta http-equiv=\"refresh\" content=\"0; url=${project.name}\">\n")
                    }
                }
            }
        }

        if (mustPublish) {
            apply(plugin = "maven-publish")
        }

        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            kotlinOptions.suppressWarnings = true
        }

        afterEvaluate {
            val jvmTest = tasks.findByName("jvmTest")
            if (jvmTest is org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest) {
                val jvmTestFix = tasks.create("jvmTestFix", Test::class) {
                    group = "verification"
                    environment("UPDATE_TEST_REF", "true")
                    testClassesDirs = jvmTest.testClassesDirs
                    classpath = jvmTest.classpath
                    bootstrapClasspath = jvmTest.bootstrapClasspath
                    if (!JvmAddOpens.beforeJava9) jvmArgs(*JvmAddOpens.createAddOpensTypedArray())
                    if (headlessTests) systemProperty("java.awt.headless", "true")
                }
                if (!JvmAddOpens.beforeJava9) jvmTest.jvmArgs(*JvmAddOpens.createAddOpensTypedArray())
                if (headlessTests) jvmTest.systemProperty("java.awt.headless", "true")
            }
        }

        kotlin {
            //explicitApi()
            //explicitApiWarning()

            metadata {
                compilations.all {
                    kotlinOptions.suppressWarnings = true
                }
            }
            jvm {
                compilations.all {
                    kotlinOptions.jvmTarget = "1.8"
                    kotlinOptions.suppressWarnings = true
                    kotlinOptions.freeCompilerArgs = listOf("-Xno-param-assertions")
                    //kotlinOptions.

                    // @TODO:
                    // Tested on Kotlin 1.4.30:
                    // Class org.luaj.vm2.WeakTableTest.WeakKeyTableTest
                    // java.lang.AssertionError: expected:<null> but was:<mydata-111>
                    //kotlinOptions.useIR = true
                }
            }
            js(org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.IR) {
                browser {
                    compilations.all {
                        //kotlinOptions.sourceMap = true
                        kotlinOptions.suppressWarnings = true
                    }
                    testTask {
                        useKarma {
                            useChromeHeadless()
                        }
                    }
                }
                nodejs {
                    testTask {
                        useMocha()
                    }
                }
            }
            if (hasAndroid) {
                apply(from = "${rootProject.rootDir}/build.android.srcset.gradle")
            }

            val desktopAndMobileTargets = ArrayList<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().apply {
                if (doEnableKotlinNative) addAll(nativeTargets(project))
                if (doEnableKotlinMobile) addAll(mobileTargets(project))
            }.toList()

            for (target in desktopAndMobileTargets) {
                target.compilations.all {
                    // https://github.com/JetBrains/kotlin/blob/ec6c25ef7ee3e9d89bf9a03c01e4dd91789000f5/kotlin-native/konan/konan.properties#L875
                    kotlinOptions.freeCompilerArgs = ArrayList<String>().apply {
                        // Raspberry Pi doesn't support mimalloc at this time
                        if (useMimalloc && !target.name.contains("Arm32Hfp")) add("-Xallocator=mimalloc")
                        add("-Xoverride-konan-properties=clangFlags.mingw_x64=-cc1 -emit-obj -disable-llvm-passes -x ir -target-cpu x86-64")
                    }
                    kotlinOptions.suppressWarnings = true
                }
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
            sourceSets {

                data class PairSourceSet(val main: org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet, val test: org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet) {
                    fun get(test: Boolean) = if (test) this.test else this.main
                    fun dependsOn(other: PairSourceSet) {
                        main.dependsOn(other.main)
                        test.dependsOn(other.test)
                    }
                }

                //fun createPairSourceSet(name: String, vararg dependencies: PairSourceSet, block: org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.(test: Boolean) -> Unit = { }): PairSourceSet {
                fun createPairSourceSet(name: String, dependency: PairSourceSet? = null, block: org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.(test: Boolean) -> Unit = { }): PairSourceSet { val dependencies = listOfNotNull(dependency)
                    //println("${project.name}: CREATED SOURCE SET: \"${name}Main\"")
                    val main = maybeCreate("${name}Main").apply { block(false) }
                    val test = maybeCreate("${name}Test").apply { block(true) }
                    return PairSourceSet(main, test).also {
                        for (dependency in dependencies) {
                            it.dependsOn(dependency)
                        }
                    }
                }

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

                if (doEnableKotlinNative) {
                    val native by lazy { createPairSourceSet("native", concurrent) }
                    val posix by lazy { createPairSourceSet("posix", native) }
                    val darwin by lazy { createPairSourceSet("darwin", posix) }

                    val linux by lazy { createPairSourceSet("linux", posix) }
                    val macos by lazy { createPairSourceSet("macos", darwin) }
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
                    val ios by lazy { createPairSourceSet("ios", iosTvos) }

                    for (target in mobileTargets(project)) {
                        val native = createPairSourceSet(target.name)
                        when {
                            target.isIos -> native.dependsOn(ios)
                            target.isWatchos -> native.dependsOn(watchos)
                            target.isTvos -> native.dependsOn(tvos)
                        }
                    }

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

                    // Copy test resources
                    afterEvaluate {
                        for (targetV in nativeTargets) {
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
                        }
                    }
                }
            }
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

fun Project.nonSamples(block: Project.() -> Unit) {
    subprojects {
        if (!project.isSample && project.hasBuildGradle()) {
            block()
        }
    }
}

fun getKorgeProcessResourcesTaskName(target: org.jetbrains.kotlin.gradle.plugin.KotlinTarget, compilation: org.jetbrains.kotlin.gradle.plugin.KotlinCompilation<*>): String =
    "korgeProcessedResources${target.name.capitalize()}${compilation.name.capitalize()}"

allprojects {
    tasks.withType(Copy::class.java).all {
        //this.duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.WARN
        this.duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
        //println("Task $this")
    }
}

rootProject.configureMavenCentralRelease()

nonSamples {
    plugins.apply("maven-publish")

    val doConfigure = project.name != "korge-gradle-plugin" && project.hasBuildGradle()

    if (doConfigure) {
        configurePublishing()
        configureSigning()
    }

    /*
    val javadocJar = tasks.maybeCreate<Jar>("javadocJar").apply { archiveClassifier.set("javadoc") }
    val sourcesJar = tasks.maybeCreate<Jar>("sourceJar").apply { archiveClassifier.set("sources") }
    //val emptyJar = tasks.maybeCreate<Jar>("emptyJar").apply {}
    extensions.getByType(PublishingExtension::class.java).apply {
        afterEvaluate {
            //println(gkotlin.sourceSets.names)

            fun configure(publication: MavenPublication) {
                //println("Publication: $publication : ${publication.name} : ${publication.artifactId}")
                if (publication.name == "kotlinMultiplatform") {
                    //publication.artifact(sourcesJar) {}
                    //publication.artifact(emptyJar) {}
                }

                /*
                val sourcesJar = tasks.create<Jar>("sourcesJar${publication.name.capitalize()}") {
                    classifier = "sources"
                    baseName = publication.name
                    val pname = when (publication.name) {
                        "metadata" -> "common"
                        else -> publication.name
                    }
                    val names = listOf("${pname}Main", pname)
                    val sourceSet = names.mapNotNull { gkotlin.sourceSets.findByName(it) }.firstOrNull() as? KotlinSourceSet
                    sourceSet?.let { from(it.kotlin) }
                    //println("${publication.name} : ${sourceSet?.javaClass}")
                    /*
                    doFirst {
                        println(gkotlin.sourceSets)
                        println(gkotlin.sourceSets.names)
                        println(gkotlin.sourceSets.getByName("main"))
                        //from(sourceSets.main.allSource)
                    }
                    afterEvaluate {
                        println(gkotlin.sourceSets.names)
                    }
                     */
                }
                */

                //val mustIncludeDocs = publication.name != "kotlinMultiplatform"
                val mustIncludeDocs = true

                //if (publication.name == "")
                if (mustIncludeDocs) {
                    publication.artifact(javadocJar)
                }
                publication.pom.withXml {
                    asNode().apply {
                        appendNode("name", project.name)
                        appendNode("description", project.property("project.description"))
                        appendNode("url", project.property("project.scm.url"))
                        appendNode("licenses").apply {
                            appendNode("license").apply {
                                appendNode("name").setValue(project.property("project.license.name"))
                                appendNode("url").setValue(project.property("project.license.url"))
                            }
                        }
                        appendNode("scm").apply {
                            appendNode("url").setValue(project.property("project.scm.url"))
                        }

                        // Changes runtime -> compile in Android's AAR publications
                        if (publication.pom.packaging == "aar") {
                            val nodes = this.getAt(groovy.xml.QName("dependencies")).getAt("dependency").getAt("scope")
                            for (node in nodes) {
                                (node as groovy.util.Node).setValue("compile")
                            }
                        }
                    }
                }
            }

            if (project.tasks.findByName("publishKotlinMultiplatformPublicationToMavenLocal") != null) {
                publications.withType(MavenPublication::class.java) {
                    configure(this)
                }
            } else {
                publications.maybeCreate<MavenPublication>("maven").apply {
                    groupId = project.group.toString()
                    artifactId = project.name
                    version = project.version.toString()
                    from(components["java"])
                    configure(this)
                }
            }
        }
    }
    */
}

internal var _webServer: DecoratedHttpServer? = null

samples {
    // @TODO: Patch, because runDebugReleaseExecutableMacosArm64 is not created!
    if (isMacos && isArm && doEnableKotlinNative) {
        project.tasks {
            afterEvaluate {
                for (kind in listOf("Debug", "Release")) {
                    val linkTaskName = "link${kind}ExecutableMacosArm64"
                    val runTaskName = "run${kind}ExecutableMacosArm64"
                    val linkExecutableMacosArm64 = project.tasks.findByName(linkTaskName) as org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
                    if (project.tasks.findByName(runTaskName) == null) {
                        val runExecutableMacosArm64 = project.tasks.create(runTaskName, Exec::class) {
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
    project.tasks {
        val jvmMainClasses by getting
        val runJvm by creating(KorgeJavaExec::class) {
            group = "run"
            mainClass.set("MainKt")
        }

        // esbuild
        run {
            fun runServer(blocking: Boolean) {
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
                        openBrowser("http://$openAddress:${server.port}/index.html")
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

            val userGradleFolder = File(System.getProperty("user.home"), ".gradle")

            val esbuildVersion = "0.12.22"
            val wwwFolder = File(buildDir, "www")

            val esbuildFolder = File(if (userGradleFolder.isDirectory) userGradleFolder else rootProject.buildDir, "esbuild")
            val isWindows = org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS)
            val esbuildCmdUnix = File(esbuildFolder, "bin/esbuild")
            val esbuildCmdCheck = if (isWindows) File(esbuildFolder, "esbuild.cmd") else esbuildCmdUnix
            val esbuildCmd = if (isWindows) File(esbuildFolder, "node_modules/esbuild/esbuild.exe") else esbuildCmdUnix

            val npmInstallEsbuild = "npmInstallEsbuild"

            val env by lazy { org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin.apply(project.rootProject).requireConfigured() }
            val ENV_PATH by lazy {
                val NODE_PATH = File(env.nodeExecutable).parent
                val PATH_SEPARATOR = File.pathSeparator
                val OLD_PATH = System.getenv("PATH")
                "$NODE_PATH$PATH_SEPARATOR$OLD_PATH"
            }

            if (rootProject.tasks.findByName(npmInstallEsbuild) == null) {
                rootProject.tasks.create(npmInstallEsbuild, Exec::class) {
                    dependsOn("kotlinNodeJsSetup")
                    onlyIf { !esbuildCmdCheck.exists() && !esbuildCmd.exists() }

                    val esbuildVersion = esbuildVersion
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
            }

            afterEvaluate {
                val browserEsbuildResources by tasks.creating(Copy::class) {
                    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                    from(project.tasks.getByName("jsProcessResources").outputs.files)
                    afterEvaluate {
                        from(project.tasks.getByName("korgeProcessedResourcesJsMain").outputs.files)
                    }
                    //for (sourceSet in gkotlin.js().compilations.flatMap { it.kotlinSourceSets }) from(sourceSet.resources)
                    into(wwwFolder)
                }
                val compileDevelopmentExecutableKotlinJs = "compileDevelopmentExecutableKotlinJs"
                val runJs by creating(Exec::class) {
                    group = "run"
                    //dependsOn("jsBrowserDevelopmentRun")
                    dependsOn(browserEsbuildResources)
                    dependsOn("::$npmInstallEsbuild")
                    dependsOn(compileDevelopmentExecutableKotlinJs)
                    //task.dependsOn(browserPrepareEsbuild)

                    val jsPath = project.tasks.getByName(compileDevelopmentExecutableKotlinJs).outputs.files.first {
                        it.extension.toLowerCase() == "js"
                    }

                    val output = File(wwwFolder, "${project.name}.js")
                    inputs.file(jsPath)
                    outputs.file(output)
                    //task.environment("PATH", ENV_PATH)
                    commandLine(ArrayList<Any>().apply {
                        add(esbuildCmd)
                        //add("--watch",)
                        add("--bundle")
                        //add("--minify")
                        //add("--sourcemap=external")
                        add(jsPath)
                        add("--outfile=$output")
                        // @TODO: Close this command on CTRL+C
                        //if (run) add("--servedir=$wwwFolder")
                    })

                    doLast {
                        runServer(!project.gradle.startParameter.isContinuous)
                    }
                }
            }
        }

        val runJsWebpack by creating {
            group = "run"
            //dependsOn("jsBrowserDevelopmentRun")
            dependsOn("jsBrowserProductionRun")
        }
        fun Task.dependsOnNativeTask(kind: String) {
            when {
                isWindows -> dependsOn("run${kind}ExecutableMingwX64")
                isMacos -> if (isArm) dependsOn("run${kind}ExecutableMacosArm64") else dependsOn("run${kind}ExecutableMacosX64")
                else -> dependsOn("run${kind}ExecutableLinuxX64")
            }
        }
        val runNativeDebug by creating {
            group = "run"
            dependsOnNativeTask("Debug")
        }
        val runNativeRelease by creating {
            group = "run"
            dependsOnNativeTask("Release")
        }

        //val jsRun by creating { dependsOn("jsBrowserDevelopmentRun") } // Already available
        //val jvmRun by creating {
        //    group = "run"
        //    dependsOn(runJvm)
        //}
        //val run by getting(JavaExec::class)

        //val processResources by getting {
        //	dependsOn(processResourcesKorge)
        //}
    }

    kotlin {
        jvm {
        }
        js {
            browser {
                binaries.executable()
            }
        }

        tasks.getByName("jsProcessResources", Task::class).apply {
            //println(this.outputs.files.toList())
            doLast {
                val targetDir = this.outputs.files.first()
                val jsMainCompilation = kotlin.js().compilations["main"]!!

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
            sourceSets.getByName("nativeMain") { kotlin.srcDir(nativeDesktopFolder) }

            val createEntryPointAdaptorNativeDesktop = tasks.create("createEntryPointAdaptorNativeDesktop") {
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

            val nativeDesktopTargets = nativeTargets(project)
            val allNativeTargets = nativeDesktopTargets

            //for (target in nativeDesktopTargets) {
                //target.compilations["main"].defaultSourceSet.dependsOn(nativeDesktopEntryPointSourceSet)
            //    target.compilations["main"].defaultSourceSet.kotlin.srcDir(nativeDesktopFolder)
            //}

            for (target in allNativeTargets) {
                for (binary in target.binaries) {
                    val compilation = binary.compilation
                    val copyResourcesTask = tasks.create("copyResources${target.name.capitalize()}${binary.name.capitalize()}", Copy::class) {
                        dependsOn(getKorgeProcessResourcesTaskName(target, compilation))
                        group = "resources"
                        val isDebug = binary.buildType == org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType.DEBUG
                        val isTest = binary.outputKind == org.jetbrains.kotlin.gradle.plugin.mpp.NativeOutputKind.TEST
                        val compilation = if (isTest) target.compilations["test"] else target.compilations["main"]
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

    project.tasks {
        val runJvm by getting(KorgeJavaExec::class)
        val jvmMainClasses by getting(Task::class)

        //val prepareResourceProcessingClasses = create("prepareResourceProcessingClasses", Copy::class) {
        //    dependsOn(jvmMainClasses)
        //    afterEvaluate {
        //        from(runJvm.korgeClassPath.toList().map { if (it.extension == "jar") zipTree(it) else it })
        //    }
        //    into(File(project.buildDir, "korgeProcessedResources/classes"))
        //}

        for (target in kotlin.targets) {
            for (compilation in target.compilations) {
                val processedResourcesFolder = File(project.buildDir, "korgeProcessedResources/${target.name}/${compilation.name}")
                compilation.defaultSourceSet.resources.srcDir(processedResourcesFolder)
                val korgeProcessedResources = create(getKorgeProcessResourcesTaskName(target, compilation)) {
                    //dependsOn(prepareResourceProcessingClasses)
                    dependsOn(jvmMainClasses)

                    doLast {
                        processedResourcesFolder.mkdirs()
                        //URLClassLoader(prepareResourceProcessingClasses.outputs.files.toList().map { it.toURL() }.toTypedArray(), ClassLoader.getSystemClassLoader()).use { classLoader ->


                        /*
                        URLClassLoader(runJvm.korgeClassPath.toList().map { it.toURL() }.toTypedArray(), ClassLoader.getSystemClassLoader()).use { classLoader ->
                            val clazz = classLoader.loadClass("com.soywiz.korge.resources.ResourceProcessorRunner")
                            val folders = compilation.allKotlinSourceSets.flatMap { it.resources.srcDirs }.filter { it != processedResourcesFolder }.map { it.toString() }
                            //println(folders)
                            try {
                                clazz.methods.first { it.name == "run" }.invoke(null, classLoader, folders, processedResourcesFolder.toString(), compilation.name)
                            } catch (e: java.lang.reflect.InvocationTargetException) {
                                val re = (e.targetException ?: e)
                                re.printStackTrace()
                                System.err.println(re.toString())
                            }
                        }
                        System.gc()
                         */
                    }
                }
                //println(compilation.compileKotlinTask.name)
                //println(compilation.compileKotlinTask.name)
                //compilation.compileKotlinTask.finalizedBy(processResourcesKorge)
                //println(compilation.compileKotlinTask)
                //compilation.compileKotlinTask.dependsOn(processResourcesKorge)
                if (compilation.compileKotlinTask.name != "compileKotlinJvm") {
                    compilation.compileKotlinTask.dependsOn(korgeProcessedResources)
                } else {
                    compilation.compileKotlinTask.finalizedBy(korgeProcessedResources)
                    getByName("runJvm").dependsOn(korgeProcessedResources)

                }
                //println(compilation.output.allOutputs.toList())
                //println("$target - $compilation")

            }
        }
    }
}

val gitVersion = try {
    Runtime.getRuntime().exec("git describe --abbrev=8 --tags --dirty".split(" ").toTypedArray(), arrayOf(), rootDir).inputStream.reader()
        .readText().lines().first().trim()
} catch (e: Throwable) {
    e.printStackTrace()
    "unknown"
}

fun symlinktree(fromFolder: File, intoFolder: File) {
    try {
        if (!intoFolder.isDirectory && !Files.isSymbolicLink(intoFolder.toPath())) {
            runCatching { intoFolder.delete() }
            runCatching { intoFolder.deleteRecursively() }
            Files.createSymbolicLink(intoFolder.toPath(), intoFolder.parentFile.toPath().relativize(fromFolder.toPath()))
        }
    } catch (e: Throwable) {
        copy {
            val it = this
            it.from(fromFolder)
            it.into(intoFolder)
            it.duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }
}

//fileTree(new File(rootProject.projectDir, "buildSrc/src/main/kotlinShared"))
//copy {
symlinktree(
    fromFolder = File(rootProject.projectDir, "buildSrc/src/main/kotlin"),
    intoFolder = File(rootProject.projectDir, "korge-gradle-plugin/build/srcgen2")
)

symlinktree(
    fromFolder = File(rootProject.projectDir, "buildSrc/src/main/resources"),
    intoFolder = File(rootProject.projectDir, "korge-gradle-plugin/build/srcgen2res")
)

val buildVersionsFile = file("korge-gradle-plugin/build/srcgen/com/soywiz/korge/gradle/BuildVersions.kt")
val oldBuildVersionsText = buildVersionsFile.takeIf { it.exists() }?.readText()
val projectVersion = project.version
val newBuildVersionsText = """
package com.soywiz.korge.gradle

object BuildVersions {
    const val GIT = "$gitVersion"
    const val KOTLIN = "$realKotlinVersion"
    const val NODE_JS = "$nodeVersion"
    const val JNA = "${libs.versions.jna.get()}"
    const val COROUTINES = "${libs.versions.kotlinx.coroutines.get()}"
    const val ANDROID_BUILD = "$androidBuildGradleVersion"
    const val KOTLIN_SERIALIZATION = "${libs.versions.kotlinx.serialization.get()}"
    const val KRYPTO = "$projectVersion"
    const val KLOCK = "$projectVersion"
    const val KDS = "$projectVersion"
    const val KMEM = "$projectVersion"
    const val KORMA = "$projectVersion"
    const val KORIO = "$projectVersion"
    const val KORIM = "$projectVersion"
    const val KORAU = "$projectVersion"
    const val KORGW = "$projectVersion"
    const val KORGE = "$projectVersion"

    val ALL_PROPERTIES by lazy { listOf(::GIT, ::KRYPTO, ::KLOCK, ::KDS, ::KMEM, ::KORMA, ::KORIO, ::KORIM, ::KORAU, ::KORGW, ::KORGE, ::KOTLIN, ::JNA, ::COROUTINES, ::ANDROID_BUILD, ::KOTLIN_SERIALIZATION) }
    val ALL by lazy { ALL_PROPERTIES.associate { it.name to it.get() } }
}
""".trimIndent()

if (oldBuildVersionsText != newBuildVersionsText) {
    buildVersionsFile.also { it.parentFile.mkdirs() }.writeText(newBuildVersionsText)
}

if (
    org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_UNIX) &&
    (File("/.dockerenv").exists() || System.getenv("TRAVIS") != null || System.getenv("GITHUB_REPOSITORY") != null) &&
    (File("/usr/bin/apt-get").exists()) &&
    (!(File("/usr/include/GL/glut.h").exists()) || !(File("/usr/include/AL/al.h").exists()))
) {
    exec { commandLine("sudo", "apt-get", "update") }
    exec { commandLine("sudo", "apt-get", "-y", "install", "freeglut3-dev", "libopenal-dev") }
    // exec { commandLine("sudo", "apt-get", "-y", "install", "libgtk-3-dev") }
}

/*
allprojects {
    //println("GROUP: $group")
    tasks.whenTaskAdded {
        if ("DebugUnitTest" in name || "ReleaseUnitTest" in name) {
            enabled = false
            // MPP + Android unit testing is so broken we just disable it altogether,
            // (discussion here https://kotlinlang.slack.com/archives/C3PQML5NU/p1572168720226200)
        }
    }
    afterEvaluate {
        // Remove log pollution until Android support in KMP improves.
        project.extensions.findByType<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension>()?.let { kmpExt ->
            kmpExt.sourceSets.removeAll { it.name == "androidAndroidTestRelease" }
        }
    }
}
*/

val currentJavaVersion = com.soywiz.korlibs.currentJavaVersion()

subprojects {
    afterEvaluate {
        tasks {
            val publishKotlinMultiplatformPublicationToMavenLocal = "publishKotlinMultiplatformPublicationToMavenLocal"
            val publishKotlinMultiplatformPublicationToMavenRepository = "publishKotlinMultiplatformPublicationToMavenRepository"

            val publishJvmLocal by creating(Task::class) {
                if (findByName(publishKotlinMultiplatformPublicationToMavenLocal) != null) {
                    dependsOn("publishJvmPublicationToMavenLocal")
                    //dependsOn("publishMetadataPublicationToMavenLocal")
                    dependsOn(publishKotlinMultiplatformPublicationToMavenLocal)
                } else if (findByName("publishToMavenLocal") != null) {
                    dependsOn("publishToMavenLocal")
                }
            }

            val publishJsLocal by creating(Task::class) {
                if (findByName(publishKotlinMultiplatformPublicationToMavenLocal) != null) {
                    dependsOn("publishJsPublicationToMavenLocal")
                    //dependsOn("publishMetadataPublicationToMavenLocal")
                    dependsOn(publishKotlinMultiplatformPublicationToMavenLocal)
                }
            }

            val publishMacosX64Local by creating(Task::class) {
                if (findByName(publishKotlinMultiplatformPublicationToMavenLocal) != null) {
                    dependsOn("publishMacosX64PublicationToMavenLocal")
                    dependsOn(publishKotlinMultiplatformPublicationToMavenLocal)
                }
            }
            val publishMacosArm64Local by creating(Task::class) {
                if (findByName(publishKotlinMultiplatformPublicationToMavenLocal) != null) {
                    dependsOn("publishMacosArm64PublicationToMavenLocal")
                    dependsOn(publishKotlinMultiplatformPublicationToMavenLocal)
                }
            }
            val publishIosX64Local by creating(Task::class) {
                if (findByName(publishKotlinMultiplatformPublicationToMavenLocal) != null) {
                    dependsOn("publishIosX64PublicationToMavenLocal")
                    dependsOn(publishKotlinMultiplatformPublicationToMavenLocal)
                }
            }
            val publishIosArm64Local by creating(Task::class) {
                if (findByName(publishKotlinMultiplatformPublicationToMavenLocal) != null) {
                    dependsOn("publishIosArm64PublicationToMavenLocal")
                    dependsOn(publishKotlinMultiplatformPublicationToMavenLocal)
                }
            }
            val publishMobileLocal by creating(Task::class) {
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

            val publishMobileRepo by creating(Task::class) {
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
        tasks.withType(Test::class.java).all {
            testLogging {
                //setEvents(setOf("passed", "skipped", "failed", "standardOut", "standardError"))
                setEvents(setOf("skipped", "failed", "standardError"))
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            }
        }
    }
}

// https://youtrack.jetbrains.com/issue/KT-48273
afterEvaluate {
    rootProject.extensions.configure<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension> {
        versions.webpackDevServer.version = "4.0.0"
    }
}

if (isLinux) {
    project.logger.info("LD folders: ${LDLibraries.ldFolders}")
    for (lib in listOf("libGL.so.1", "libopenal.so.1")) {
        if (!LDLibraries.hasLibrary(lib)) {
            System.err.println("Can't find $lib. Please: sudo apt-get -y install freeglut3 libopenal1")
        }
    }
}

//println("currentJavaVersion=${com.soywiz.korlibs.currentJavaVersion()}")

afterEvaluate {
    subprojects {
        val mingwX64Test = project.tasks.findByName("mingwX64Test")
        if (mingwX64Test != null && com.soywiz.korge.gradle.targets.isWindows && com.soywiz.korge.gradle.targets.inCI) {
            mingwX64Test.doFirst {
                exec { commandLine("systeminfo") }
            }
            mingwX64Test.doLast {
                exec { commandLine("systeminfo") }
            }
        }
    }
}

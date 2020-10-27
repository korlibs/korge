import org.gradle.kotlin.dsl.kotlin
import org.jetbrains.kotlin.gradle.plugin.*
import java.io.File

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        google()
        maven { url = uri("https://plugins.gradle.org/m2/") }
        maven { url = uri("https://dl.bintray.com/kotlin/kotlin-dev") }
        maven { url = uri("https://dl.bintray.com/kotlin/kotlin-eap") }
    }
    dependencies {
        classpath("com.gradle.publish:plugin-publish-plugin:0.10.1")
        classpath("gradle.plugin.org.jetbrains.intellij.plugins:gradle-intellij-plugin:0.4.16")
        //classpath("com.android.tools.build:gradle:3.4.1")
        classpath("com.android.tools.build:gradle:4.0.1")
        //classpath("com.android.tools.build:gradle:4.1.0-rc03")
        //classpath("com.android.tools.build:gradle:4.2.0-alpha12")
    }
}

plugins {
    val kotlinVersion: String by project
	java
    kotlin("multiplatform") version kotlinVersion
    //id("com.gradle.plugin-publish") version "0.12.0" apply false
}

val kotlinVersion: String by project

//println(KotlinVersion.CURRENT)

allprojects {
	repositories {
        mavenLocal()
		mavenCentral()
		jcenter()
        google()
		maven { url = uri("https://plugins.gradle.org/m2/") }
        maven { url = uri("https://dl.bintray.com/kotlin/kotlin-eap") }
        maven { url = uri("https://dl.bintray.com/kotlin/kotlin-dev") }
	}
}

val enableKotlinNative: String by project
val doEnableKotlinNative get() = enableKotlinNative == "true"

val enableKotlinAndroid: String by project
val doEnableKotlinAndroid get() = enableKotlinAndroid == "true"

val enableKotlinMobile:String by project
val doEnableKotlinMobile get() = enableKotlinMobile == "true"

val KotlinTarget.isLinux get() = this.name == "linuxX64"
val KotlinTarget.isWin get() = this.name == "mingwX64"
val KotlinTarget.isMacos get() = this.name == "macosX64"
val KotlinTarget.isIosArm64 get() = this.name == "iosArm64"
val KotlinTarget.isIosX64 get() = this.name == "iosX64"
val KotlinTarget.isIos get() = isIosArm64 || isIosX64
val KotlinTarget.isWatchosX86 get() = this.name == "watchosX86"
val KotlinTarget.isWatchosArm32 get() = this.name == "watchosArm32"
val KotlinTarget.isWatchosArm64 get() = this.name == "watchosArm64"
val KotlinTarget.isWatchos get() = isWatchosX86 || isWatchosArm32 || isWatchosArm64
val KotlinTarget.isTvosX64 get() = this.name == "tvosX64"
val KotlinTarget.isTvosArm64 get() = this.name == "tvosArm64"
val KotlinTarget.isTvos get() = isTvosX64 || isTvosArm64
val KotlinTarget.isDesktop get() = isWin || isLinux || isMacos

val isWindows get() = org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS)
val isMacos get() = org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_MAC)

fun guessAndroidSdkPath(): String? {
    val userHome = System.getProperty("user.home")
    return listOfNotNull(
        System.getenv("ANDROID_HOME"),
        "$userHome/AppData/Local/Android/sdk",
        "$userHome/Library/Android/sdk",
        "$userHome/Android/Sdk"
    ).firstOrNull { File(it).exists() }
}

fun hasAndroidSdk(): Boolean {
    val env = System.getenv("ANDROID_SDK_ROOT")
    if (env != null) return true
    val localPropsFile = File(rootProject.rootDir, "local.properties")
    if (!localPropsFile.exists()) {
        val sdkPath = guessAndroidSdkPath() ?: return false
        localPropsFile.writeText("sdk.dir=${sdkPath.replace("\\", "/")}")
    }
    return true
}

val hasAndroidSdk by lazy { hasAndroidSdk() }

// Required by RC
kotlin {
    jvm { }
}

fun org.jetbrains.kotlin.gradle.dsl.KotlinTargetContainerWithPresetFunctions.nativeTargets(): List<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
    return when {
        isWindows -> listOf(mingwX64())
        isMacos -> listOf(macosX64())
        else -> listOf(linuxX64(), mingwX64(), macosX64())
    }
}

fun org.jetbrains.kotlin.gradle.dsl.KotlinTargetContainerWithPresetFunctions.mobileTargets(): List<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
    return listOf(iosArm64(), iosX64())
}

//apply(from = "${rootProject.rootDir}/build.idea.gradle")

fun Project.hasBuildGradle() = listOf("build.gradle", "build.gradle.kts").any { File(projectDir, it).exists() }

allprojects {
    if (project.hasBuildGradle()) {
        apply(from = "${rootProject.rootDir}/build.idea.gradle")
    }
}

subprojects {
    val projectName = project.name
    group = when {
        projectName == "korge-gradle-plugin" -> "com.soywiz.korlibs.korge.plugins"
        else -> "com.soywiz.korlibs.${projectName.substringBefore('-')}"
    }

    val doConfigure =
        project.name != "korge-intellij-plugin" &&
            project.name != "korge-gradle-plugin" &&
            project.hasBuildGradle()

    if (doConfigure) {
        val isSample = project.path.startsWith(":samples")
        val hasAndroid = !isSample && doEnableKotlinAndroid && hasAndroidSdk
        //val hasAndroid = !isSample && true
        val mustPublish = !isSample

        // AppData\Local\Android\Sdk\tools\bin>sdkmanager --licenses
        if (hasAndroid) {
            apply(from = "${rootProject.rootDir}/build.android.gradle")
        }

        apply(plugin = "kotlin-multiplatform")

        if (mustPublish) {
            apply(plugin = "maven-publish")
        }

        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            kotlinOptions.suppressWarnings = true
        }

        kotlin {
            metadata {
                compilations.all {
                    kotlinOptions.suppressWarnings = true
                }
            }
            jvm {
                compilations.all {
                    kotlinOptions.jvmTarget = "1.8"
                    kotlinOptions.suppressWarnings = true
                }
            }
            js(org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType.IR) {
                browser {
                    compilations.all {
                        kotlinOptions.sourceMap = true
                        kotlinOptions.suppressWarnings = true
                    }
                    testTask {
                        useKarma {
                            useChromeHeadless()
                        }
                    }
                }
            }
            if (hasAndroid) {
                apply(from = "${rootProject.rootDir}/build.android.srcset.gradle")
            }
            if (doEnableKotlinNative) {
                for (target in nativeTargets()) {
                    target.compilations.all {
                        //kotlinOptions.freeCompilerArgs = listOf("-Xallocator=mimalloc")
                        kotlinOptions.suppressWarnings = true
                    }
                }
            }

            if (doEnableKotlinMobile) {
                for (target in mobileTargets()) {
                    target.compilations.all {
                        //kotlinOptions.freeCompilerArgs = listOf("-Xallocator=mimalloc")
                        kotlinOptions.suppressWarnings = true
                    }
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

                fun createPairSourceSet(name: String, vararg dependencies: PairSourceSet, block: org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.(test: Boolean) -> Unit = { }): PairSourceSet {
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
                val nonNativeCommon = createPairSourceSet("nonNativeCommon", common)
                val nonJs = createPairSourceSet("nonJs", common)
                val nonJvm = createPairSourceSet("nonJvm", common)
                val jvmAndroid = createPairSourceSet("jvmAndroid", common)

                // Default source set for JVM-specific sources and dependencies:
                // JVM-specific tests and their dependencies:
                val jvm = createPairSourceSet("jvm", concurrent, nonNativeCommon, nonJs, jvmAndroid) { test ->
                    dependencies {
                        if (test) {
                            implementation(kotlin("test-junit"))
                        } else {
                            implementation(kotlin("stdlib-jdk8"))
                        }
                    }
                }

                if (hasAndroid) {
                    val android = createPairSourceSet("android", concurrent, nonNativeCommon, nonJs, jvmAndroid) { test ->
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

                val js = createPairSourceSet("js", common, nonNativeCommon, nonJvm) { test ->
                    dependencies {
                        if (test) {
                            implementation(kotlin("test-js"))
                        } else {
                            implementation(kotlin("stdlib-js"))
                        }
                    }
                }

                if (doEnableKotlinNative) {
                    val nativeCommon by lazy { createPairSourceSet("nativeCommon", concurrent) }
                    val nativeDesktop by lazy { createPairSourceSet("nativeDesktop", concurrent) }
                    val nativePosix by lazy { createPairSourceSet("nativePosix", nativeCommon) }
                    val nativePosixNonApple by lazy { createPairSourceSet("nativePosixNonApple", nativePosix) }
                    val nativePosixApple by lazy { createPairSourceSet("nativePosixApple", nativePosix) }
                    val iosWatchosTvosCommon by lazy { createPairSourceSet("iosWatchosTvosCommon", nativePosixApple) }
                    val iosWatchosCommon by lazy { createPairSourceSet("iosWatchosCommon", nativePosixApple) }
                    val iosTvosCommon by lazy { createPairSourceSet("iosTvosCommon", nativePosixApple) }
                    val macosIosTvosCommon by lazy { createPairSourceSet("macosIosTvosCommon", nativePosixApple) }
                    val macosIosWatchosCommon by lazy { createPairSourceSet("macosIosWatchosCommon", nativePosixApple) }
                    val iosCommon by lazy { createPairSourceSet("iosCommon", iosWatchosTvosCommon) }

                    for (target in nativeTargets()) {
                        val native = createPairSourceSet(target.name, common, nativeCommon, nonJvm, nonJs)
                        if (target.isDesktop) {
                            native.dependsOn(nativeDesktop)
                        }
                        if (target.isLinux || target.isMacos) {
                            native.dependsOn(nativePosix)
                        }
                        if (target.isLinux) {
                            native.dependsOn(nativePosixNonApple)
                        }
                        if (target.isMacos) {
                            native.dependsOn(nativePosixApple)
                            native.dependsOn(macosIosTvosCommon)
                            native.dependsOn(macosIosWatchosCommon)
                        }
                    }

                    if (doEnableKotlinMobile) {
                        for (target in mobileTargets()) {
                            val native = createPairSourceSet(target.name, common, nativeCommon, nonJvm, nonJs)
                            if (target.isIos) {
                                native.dependsOn(nativePosixApple)
                                native.dependsOn(iosCommon)
                                native.dependsOn(iosWatchosTvosCommon)
                                native.dependsOn(iosWatchosCommon)
                                native.dependsOn(iosTvosCommon)
                                native.dependsOn(macosIosTvosCommon)
                                native.dependsOn(macosIosWatchosCommon)
                            }
                            if (target.isWatchos) {
                                native.dependsOn(nativePosixApple)
                                native.dependsOn(iosWatchosCommon)
                                native.dependsOn(iosWatchosTvosCommon)
                                native.dependsOn(macosIosWatchosCommon)
                            }
                            if (target.isTvos) {
                                native.dependsOn(nativePosixApple)
                                native.dependsOn(iosWatchosTvosCommon)
                                native.dependsOn(iosTvosCommon)
                                native.dependsOn(macosIosTvosCommon)
                            }
                        }
                    }
                }
            }
        }
    }
}

open class KorgeJavaExec : JavaExec() {
    private val jvmCompilation by lazy { project.kotlin.targets.getByName("jvm").compilations as NamedDomainObjectSet<*> }
    private val mainJvmCompilation by lazy { jvmCompilation.getByName("main") as org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation }

    @get:InputFiles
    val korgeClassPath by lazy {
        mainJvmCompilation.runtimeDependencyFiles + mainJvmCompilation.compileDependencyFiles + mainJvmCompilation.output.allOutputs + mainJvmCompilation.output.classesDirs
    }

    init {
        systemProperties = (System.getProperties().toMutableMap() as MutableMap<String, Any>) - "java.awt.headless"
        val useZgc = (System.getenv("JVM_USE_ZGC") == "true") || (javaVersion.majorVersion.toIntOrNull() ?: 8) >= 14

        doFirst {
            if (useZgc) {
                println("Using ZGC")
            }
        }

        if (useZgc) {
            //jvmArgs("-XX:+UnlockExperimentalVMOptions", "-XX:+UseZGC")
        }
        project.afterEvaluate {
            //if (firstThread == true && OS.isMac) task.jvmArgs("-XstartOnFirstThread")
            classpath = korgeClassPath
        }
    }
}

fun Project.samples(block: Project.() -> Unit) {
    subprojects {
        if (project.path.startsWith(":samples:") && project.hasBuildGradle()) {
            block()
        }
    }
}

fun getKorgeProcessResourcesTaskName(target: org.jetbrains.kotlin.gradle.plugin.KotlinTarget, compilation: org.jetbrains.kotlin.gradle.plugin.KotlinCompilation<*>): String =
    "korgeProcessedResources${target.name.capitalize()}${compilation.name.capitalize()}"


samples {

    // @TODO: Move to KorGE plugin
    project.tasks {
        val jvmMainClasses by getting
        val runJvm by creating(KorgeJavaExec::class) {
            group = "run"
            main = "MainKt"
        }
        val runJs by creating {
            group = "run"
            dependsOn("jsBrowserDevelopmentRun")
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
                val jsFile = File(jsMainCompilation.kotlinOptions.outputFile ?: "dummy.js").name
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
            for (target in nativeTargets()) {
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
            sourceSets.getByName("nativeCommonMain") { kotlin.srcDir(nativeDesktopFolder) }

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

            val nativeDesktopTargets = nativeTargets()
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

val buildVersionsFile = file("korge-gradle-plugin/src/main/kotlin/com/soywiz/korge/gradle/BuildVersions.kt")
val oldBuildVersionsText = buildVersionsFile.readText()
val newBuildVersionsText = oldBuildVersionsText
    .replace(Regex("const val KORLIBS_VERSION = \"(.*?)\""), "const val KORLIBS_VERSION = \"${project.version}\"")
    .replace(Regex("const val KOTLIN = \"(.*?)\""), "const val KOTLIN = \"${kotlinVersion}\"")
if (oldBuildVersionsText != newBuildVersionsText) {
    buildVersionsFile.writeText(newBuildVersionsText)
}

/*
if (Os.isFamily(Os.FAMILY_UNIX) &&
    (File("/.dockerenv").exists() || System.getenv("TRAVIS") != null || System.getenv("GITHUB_REPOSITORY") != null) &&
    (File("/usr/bin/apt-get").exists())
) {

    exec { commandLine("sudo", "apt-get", "update")  }
    exec { commandLine("sudo", "apt-get", "-y", "install", "freeglut3-dev", "libopenal-dev", "ncurses5")  }
    //exec { commandLine("sudo", "apt-get", "-y", "install", "libgtk-3-dev") }
}
*/

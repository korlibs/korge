package com.soywiz.korlibs.root

import com.android.build.gradle.*
import com.android.build.gradle.internal.tasks.*
import com.soywiz.korge.gradle.*
import com.soywiz.korge.gradle.targets.*
import com.soywiz.korge.gradle.targets.android.*
import com.soywiz.korge.gradle.targets.ios.*
import com.soywiz.korge.gradle.targets.jvm.*
import com.soywiz.korge.gradle.targets.native.*
import com.soywiz.korge.gradle.util.*
import com.soywiz.korge.gradle.util.create
import com.soywiz.korlibs.*
import com.soywiz.korlibs.gkotlin
import com.soywiz.korlibs.kotlin
import com.soywiz.korlibs.modules.*
import com.soywiz.korlibs.modules.KorgeJavaExec
import com.soywiz.korlibs.tasks
import org.gradle.api.*
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.*
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.testing.*
import org.gradle.jvm.tasks.*
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.plugins.ide.idea.model.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.tasks.*
import java.io.*
import java.net.*
import java.nio.file.*

object RootKorlibsPlugin {
    @JvmStatic
    fun doInit(rootProject: Project) {
        rootProject.init()
    }

    fun Project.init() {
        configureBuildScriptClasspathTasks()
        initPlugins()
        initRootKotlinJvmTarget()
        initVersions()
        initAllRepositories()
        initIdeaExcludes()
        initGroupOverrides()
        initNodeJSFixes()
        configureMavenCentralRelease()
        initDuplicatesStrategy()
        initSymlinkTrees()
        initShowSystemInfoWhenLinkingInWindows()
        com.soywiz.korge.gradle.KorgeVersionsTask.registerShowKorgeVersions(project)
        initInstallAndCheckLinuxLibs()
        initCatalog()
        configureKover()
        initBuildVersions()
        initAndroidFixes()
        initPublishing()
        initKMM()
        initSamples()
        initShortcuts()
        initTests()
        initCrossTests()
        initPatchTests()
    }

    fun Project.initPatchTests() {
        subprojectsThis {
            if (this.name == "korge") {
                configureMingwX64TestWithMesa()
            }
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
        fun ArtifactRepository.config() {
            content { it.excludeGroup("Kotlin/Native") }
        }
        allprojectsThis {
            repositories.apply {
                mavenLocal().config()
                mavenCentral().config()
                google().config()
                maven { it.url = uri("https://plugins.gradle.org/m2/") }.config()
                maven { it.url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/temporary") }.config()
                maven { it.url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev") }.config()
                maven { it.url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven") }.config()
                maven { it.url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }.config()
            }
        }
    }

    fun Project.initIdeaExcludes() {
        allprojectsThis {
            if (project.hasBuildGradle()) {
                val plugin = this.plugins.apply(IdeaPlugin::class.java)
                val idea = this.extensions.getByType<IdeaModel>()

                idea.apply {
                    module {
                        it.excludeDirs = it.excludeDirs + listOf(
                            file(".gradle"), file("src2"), file("original"), file("original-tests"), file("old-rendering"),
                            file("gradle/wrapper"), file(".idea"), file("build"), file("@old"), file("_template"),
                            file("e2e-sample"), file("e2e-test"), file("experiments"),
                        )
                    }
                }
            }
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
            fromFolder = File(rootProject.projectDir, "buildSrc/src/main/kotlin8"),
            intoFolder = File(rootProject.projectDir, "korge-gradle-plugin/build/srcgen2_8")
        )

        project.symlinktree(
            fromFolder = File(rootProject.projectDir, "buildSrc/src/main/kotlin11"),
            intoFolder = File(rootProject.projectDir, "korge-gradle-plugin/build/srcgen2_11")
        )

        project.symlinktree(
            fromFolder = File(rootProject.projectDir, "buildSrc/src/main/resources"),
            intoFolder = File(rootProject.projectDir, "korge-gradle-plugin/build/srcgen2res")
        )
    }

    fun Project.initShowSystemInfoWhenLinkingInWindows() {
        rootProject.afterEvaluate {
            subprojectsThis {
                val linkDebugTestMingwX64 = project.tasks.findByName("linkDebugTestMingwX64")
                if (linkDebugTestMingwX64 != null && isWindows && inCI) {
                    linkDebugTestMingwX64.doFirst { execThis { commandLine("systeminfo") } }
                    linkDebugTestMingwX64.doLast { execThis { commandLine("systeminfo") } }
                }

                val mingwX64Test = project.tasks.findByName("mingwX64Test")
                if (mingwX64Test != null && isWindows && inCI) {
                    mingwX64Test.doFirst { execThis { commandLine("systeminfo") } }
                    mingwX64Test.doLast { execThis { commandLine("systeminfo") } }
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
            rootProject.execThis { commandLine("sudo", "apt-get", "-y", "install", "freeglut3-dev", "libopenal-dev") }
            // execThis { commandLine("sudo", "apt-get", "-y", "install", "libgtk-3-dev") }
        }
        if (isLinux) {
            project.logger.info("LD folders: ${LDLibraries.ldFolders}")
            for (lib in listOf("libGL.so.1", "libopenal.so.1")) {
                if (!LDLibraries.hasLibrary(lib)) {
                    System.err.println("Can't find $lib. Please: sudo apt-get -y install freeglut3 libopenal1")
                }
            }
        }
    }

    fun Project.initCatalog() {

        //try {
        //    println(URL("http://127.0.0.1:$httpPort/?startTime=0&endTime=1").readText())
        //} catch (e: Throwable) {
        //    e.printStackTrace()
        //}

        // @TODO: $catalog.json
        //afterEvaluate {
        //    val jsTestTestDevelopmentExecutableCompileSync = tasks.findByPath(":korim:jsTestTestDevelopmentExecutableCompileSync")
        //    if (jsTestTestDevelopmentExecutableCompileSync != null) {
        //        val copy = jsTestTestDevelopmentExecutableCompileSync as Copy
        //        //copy.from(File(""))
        //    }
        //}

        //println(tasks.findByPath(":korim:jsTestTestDevelopmentExecutableCompileSync")!!::class)

    }


    fun Project.initBuildVersions() {

        // Build versions
        val projectVersion = project.version
        fun createBuildVersions(git: Boolean): String =  """
            package com.soywiz.korge.gradle
            
            object BuildVersions {
                const val GIT = "${if (git) project.gitVersion else "main"}"
                const val KOTLIN = "${project.realKotlinVersion}"
                const val NODE_JS = "${project.nodeVersion}"
                const val JNA = "${project._libs["versions"]["jna"].dynamicInvoke("get").casted<String>()}"
                const val COROUTINES = "${project._libs["versions"]["kotlinx"]["coroutines"].dynamicInvoke("get").casted<String>()}"
                const val ANDROID_BUILD = "${project.androidBuildGradleVersion}"
                const val KOTLIN_SERIALIZATION = "${project._libs["versions"]["kotlinx"]["serialization"].dynamicInvoke("get").casted<String>()}"
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

        rootProject.file("buildSrc/src/main/kotlinGen/com/soywiz/korge/gradle/BuildVersions.kt").writeTextIfChanged(createBuildVersions(git = false))
        rootProject.file("korge-gradle-plugin/build/srcgen/com/soywiz/korge/gradle/BuildVersions.kt").writeTextIfChanged(createBuildVersions(git = true))
    }

    fun Project.initAndroidApplication() {
        //apply(plugin = "com.android.application")
        val androidApplicationId = "com.korge.samples.${project.name.replace("-", "_")}"
        val korgeGradlePluginResources = File(rootProject.projectDir, "buildSrc/src/main/resources")
        val android = extensions.getByName<TestedExtension>("android")
        android.apply {
            lintOptions {
                // @TODO: ../../build.gradle: All com.android.support libraries must use the exact same version specification (mixing versions can lead to runtime crashes). Found versions 28.0.0, 26.1.0. Examples include com.android.support:animated-vector-drawable:28.0.0 and com.android.support:customtabs:26.1.0
                it.disable("GradleCompatible")
            }
            // @TODO: Is this required?
            //kotlinOptions {
            //    jvmTarget = "1.8"
            //    freeCompilerArgs += "-Xmulti-platform"
            //}
            packagingOptions {
                it.exclude("META-INF/DEPENDENCIES")
                it.exclude("META-INF/LICENSE")
                it.exclude("META-INF/LICENSE.txt")
                it.exclude("META-INF/license.txt")
                it.exclude("META-INF/NOTICE")
                it.exclude("META-INF/NOTICE.txt")
                it.exclude("META-INF/notice.txt")
                it.exclude("META-INF/LGPL*")
                it.exclude("META-INF/AL2.0")
                it.exclude("META-INF/*.kotlin_module")
                it.exclude("**/*.kotlin_metadata")
                it.exclude("**/*.kotlin_builtins")
            }
            compileSdkVersion(28)
            defaultConfig {
                it.multiDexEnabled = true
                it.applicationId = androidApplicationId
                it.minSdk = 16
                it.targetSdk = 28
                it.versionCode = 1
                it.versionName = "1.0"
                it.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                it.manifestPlaceholders.clear()
            }
            signingConfigs {
                it.maybeCreate("release").apply {
                    //storeFile file(findProperty('RELEASE_STORE_FILE') ?: "korge.keystore")
                    storeFile(File(korgeGradlePluginResources, "korge.keystore"))
                    storePassword(findProperty("RELEASE_STORE_PASSWORD")?.toString() ?: "password")
                    keyAlias(findProperty("RELEASE_KEY_ALIAS")?.toString() ?: "korge")
                    keyPassword(findProperty("RELEASE_KEY_PASSWORD")?.toString() ?: "password")
                }
            }
            buildTypes {
                it.maybeCreate("debug").apply {
                    minifyEnabled(false)
                    setSigningConfig(signingConfigs.maybeCreate("release"))
                }
                it.maybeCreate("release").apply {
                    minifyEnabled(true)
                    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), File(rootProject.rootDir, "proguard-rules.pro"))
                    setSigningConfig(signingConfigs.maybeCreate("release"))
                }
            }
            sourceSets {
                it.maybeCreate("main").apply {
                    manifest.srcFile(File(project.buildDir, "AndroidManifest.xml"))
                    java.srcDirs("${project.buildDir}/androidsrc")
                    res.srcDirs("${project.buildDir}/androidres")
                    assets.srcDirs(
                        "${project.projectDir}/src/commonMain/resources",
                        "${project.projectDir}/src/androidMain/resources",
                        "${project.projectDir}/src/main/resources",
                        "${project.projectDir}/build/commonMain/korgeProcessedResources/metadata/main",
                    )
                    //java.srcDirs += ["C:\\Users\\soywi\\projects\\korlibs\\korge-hello-world\\src\\commonMain\\kotlin", "C:\\Users\\soywi\\projects\\korlibs\\korge-hello-world\\src\\androidMain\\kotlin", "C:\\Users\\soywi\\projects\\korlibs\\korge-hello-world\\src\\main\\java"]
                }
            }
        }

        val mainDir = project.buildDir

        val createAndroidManifest = tasks.createThis<Task>("createAndroidManifest") {
            doFirst {
                val generated = AndroidGenerated(
                    icons = KorgeIconProvider(File(korgeGradlePluginResources, "icons/korge.png"), File(korgeGradlePluginResources, "banners/korge.png")),
                    ifNotExists = true,
                    androidPackageName = androidApplicationId,
                    realEntryPoint = "main",
                    androidMsaa = 4,
                    androidAppName = project.name,
                )

                generated.writeResources(File(mainDir, "androidres"))
                generated.writeMainActivity(File(mainDir, "androidsrc"))
                generated.writeKeystore(mainDir)
                generated.writeAndroidManifest(mainDir)
            }
        }

        //tasks.getByName("installDebug").dependsOn("createAndroidManifest")

        tasks.createThis<Task>("onlyRunAndroid") {
            doFirst {
                val adb = "${AndroidSdk.guessAndroidSdkPath()}/platform-tools/adb"
                execThis {
                    commandLine(adb, "shell", "am", "start", "-n", "${androidApplicationId}/${androidApplicationId}.MainActivity")
                }

                var pid = ""
                for (n in 0 until 10) {
                    try {
                        pid = execOutput(adb, "shell", "pidof", androidApplicationId)
                        break
                    } catch (e: Throwable) {
                        Thread.sleep(500L)
                        if (n == 9) throw e
                    }
                }
                println(pid)
                execThis {
                    commandLine(adb, "logcat", "--pid=${pid.trim()}")
                }
            }
        }

        fun ordered(vararg dependencyPaths: String): List<Task> {
            val dependencies = dependencyPaths.map { tasks.getByPath(it) }
            for (n in 0 until dependencies.size - 1) {
                dependencies[n + 1].mustRunAfter(dependencies[n])
            }
            return dependencies
        }

        afterEvaluate {
            //InstallVariantTask id = installDebug
            (tasks.getByName("installRelease") as InstallVariantTask).apply {
                installOptions = listOf("-r")
            }
            //println(installDebug.class)

            tasks.createThis<Task>("runAndroidDebug") {
                dependsOn(ordered("createAndroidManifest", "installDebug"))
                finalizedBy("onlyRunAndroid")
            }

            tasks.createThis<Task>("runAndroidRelease") {
                dependsOn(ordered("createAndroidManifest", "installRelease"))
                finalizedBy("onlyRunAndroid")
            }

            tasks.findByName("generateDebugBuildConfig")
                ?.dependsOn(createAndroidManifest)
        }

    }

    fun Project.initPlugins() {
        plugins.apply("java")
        plugins.apply("kotlin-multiplatform")
        plugins.apply("signing")
        plugins.apply("maven-publish")
    }

    fun Project.initAndroidFixes() {
        /*
        allprojectsThis {
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
    }

    fun Project.initPublishing() {

        rootProject.afterEvaluate {
            rootProject.nonSamples {
                plugins.apply("maven-publish")

                val doConfigure = mustAutoconfigureKMM()

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
                            val sourcesJar = tasks.createThis<Jar>("sourcesJar${publication.name.capitalize()}") {
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
        }
    }

    fun Project.initKMM() {
        rootProject.subprojectsThis {
            val doConfigure = mustAutoconfigureKMM()

            if (doConfigure) {
                val isSample = project.isSample
                val hasAndroid = doEnableKotlinAndroid && hasAndroidSdk
                //val hasAndroid = !isSample && true
                val mustPublish = !isSample

                // AppData\Local\Android\Sdk\tools\bin>sdkmanager --licenses
                plugins.apply("kotlin-multiplatform")

                //initAndroidProject()
                if (hasAndroid) {
                    if (isSample) {
                        plugins.apply("com.android.application")
                    } else {
                        plugins.apply("com.android.library")
                    }

                    //apply(from = "${rootProject.rootDir}/build.android.gradle")

                    //apply(plugin = "kotlin-android")
                    //apply(plugin = "kotlin-android-extensions")
                    // apply plugin: 'kotlin-android'
                    // apply plugin: 'kotlin-android-extensions'
                    val android = extensions.getByName<TestedExtension>("android")
                    android.apply {
                        compileSdkVersion(project.findProperty("android.compile.sdk.version")?.toString()?.toIntOrNull() ?: 30)
                        buildToolsVersion(project.findProperty("android.buildtools.version")?.toString() ?: "30.0.2")

                        defaultConfig {
                            it.multiDexEnabled = true
                            it.minSdk = project.findProperty("android.min.sdk.version")?.toString()?.toIntOrNull() ?: 16 // Previously 18
                            it.targetSdk = project.findProperty("android.target.sdk.version")?.toString()?.toIntOrNull() ?: 30
                            it.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                            //testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner"
                        }
                    }

                    dependencies {
                        add("androidTestImplementation", "androidx.test:core:1.4.0")
                        add("androidTestImplementation", "androidx.test.ext:junit:1.1.2")
                        add("androidTestImplementation", "androidx.test.espresso:espresso-core:3.3.0")
                        //androidTestImplementation 'com.android.support.test:runner:1.0.2'
                    }

                    android.apply {
                        sourceSets {
                            it.maybeCreate("main").apply {
                                if (System.getenv("ANDROID_TESTS") == "true") {
                                    assets.srcDirs(
                                        "src/commonMain/resources",
                                        "src/commonTest/resources",
                                    )
                                } else {
                                    assets.srcDirs("src/commonMain/resources",)
                                }
                            }
                            it.maybeCreate("test").apply {
                                assets.srcDirs(
                                    "src/commonTest/resources",
                                )
                            }
                        }
                    }


                    if (isSample) {
                        initAndroidApplication()
                        //apply(from = "${rootProject.rootDir}/build.android.application.gradle")
                    }
                }

                if (isSample && doEnableKotlinNative && isMacos) {
                    project.configureNativeIos()
                }

                if (!isSample && rootProject.plugins.hasPlugin("org.jetbrains.dokka")) {
                    plugins.apply("org.jetbrains.dokka")

                    //tasks.dokkaHtml.configure {
                    //    offlineMode.set(true)
                    //}

                    //dokkaHtml {
                    //    // Used to prevent resolving package-lists online. When this option is set to true, only local files are resolved
                    //    offlineMode.set(true)
                    //}

                    tasks {
                        //val dokkaCopy = createThis<Task>("dokkaCopy") {
                        //    dependsOn("dokkaHtml")
                        //    doLast {
                        //        val ffrom = File(project.buildDir, "dokka/html")
                        //        val finto = File(project.rootProject.projectDir, "build/dokka-all/${project.name}")
                        //        copy {
                        //            from(ffrom)
                        //            into(finto)
                        //        }
                        //        File(finto, "index-redirect.html").writeText("<meta http-equiv=\"refresh\" content=\"0; url=${project.name}\">\n")
                        //    }
                        //}
                    }
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
                            compilations.allThis {
                                //kotlinOptions.sourceMap = true
                                kotlinOptions.suppressWarnings = true
                            }
                            testTask {
                                useKarma {
                                    useChromeHeadless()
                                    useConfigDirectory(File(rootDir, "karma.config.d"))
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
                        kotlin {
                            android {
                                publishAllLibraryVariants()
                                publishLibraryVariantsGroupedByFlavor = true
                                //this.attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.androidJvm)
                                compilations.allThis {
                                    kotlinOptions.jvmTarget = "1.8"
                                    kotlinOptions.suppressWarnings = true
                                    kotlinOptions.freeCompilerArgs = listOf("-Xno-param-assertions")
                                }
                            }
                        }

                    }

                    val desktopAndMobileTargets = ArrayList<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().apply {
                        if (doEnableKotlinNative) addAll(nativeTargets(project))
                        if (doEnableKotlinMobile) addAll(mobileTargets(project))
                    }.toList()

                    for (target in desktopAndMobileTargets) {
                        target.compilations.allThis {
                            // https://github.com/JetBrains/kotlin/blob/ec6c25ef7ee3e9d89bf9a03c01e4dd91789000f5/kotlin-native/konan/konan.properties#L875
                            kotlinOptions.freeCompilerArgs = ArrayList<String>().apply {
                                // Raspberry Pi doesn't support mimalloc at this time
                                if (useMimalloc && !target.name.contains("Arm32Hfp")) add("-Xallocator=mimalloc")
                                add("-Xoverride-konan-properties=clangFlags.mingw_x64=-cc1 -emit-obj -disable-llvm-passes -x ir -target-cpu x86-64")
                            }
                            kotlinOptions.freeCompilerArgs += listOf(
                                "-Xbinary=memoryModel=experimental",
                                // @TODO: https://youtrack.jetbrains.com/issue/KT-49234#focus=Comments-27-5293935.0-0
                                //"-Xdisable-phases=RemoveRedundantCallsToFileInitializersPhase",
                            )
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
        }
    }

    fun Project.initSamples() {
        rootProject.samples {
            // @TODO: Patch, because runDebugReleaseExecutableMacosArm64 is not created!
            if (isMacos && isArm && doEnableKotlinNative) {
                project.tasks {
                    afterEvaluate {
                        for (kind in listOf("Debug", "Release")) {
                            val linkTaskName = "link${kind}ExecutableMacosArm64"
                            val runTaskName = "run${kind}ExecutableMacosArm64"
                            val linkExecutableMacosArm64 = project.tasks.findByName(linkTaskName) as org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
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
            project.apply {
                project.tasks {
                    // https://www.baeldung.com/java-instrumentation
                    val runJvm = createThis<KorgeJavaExec>("runJvm") {
                        group = "run"
                        mainClass.set("MainKt")
                    }

                    val timeBeforeCompilationFile = File(project.buildDir, "timeBeforeCompilation")
                    val httpPort = 22011

                    val compileKotlinJvmAndNotifyBefore = createThis<Task>("compileKotlinJvmAndNotifyBefore") {
                        doFirst {
                            KorgeReloadNotifier.beforeBuild(timeBeforeCompilationFile)
                        }
                    }
                    afterEvaluate {
                        tasks.findByName("compileKotlinJvm")?.mustRunAfter("compileKotlinJvmAndNotifyBefore")
                    }
                    val compileKotlinJvmAndNotify = createThis<Task>("compileKotlinJvmAndNotify") {
                        dependsOn("compileKotlinJvmAndNotifyBefore", "compileKotlinJvm")
                        doFirst {
                            KorgeReloadNotifier.afterBuild(timeBeforeCompilationFile, httpPort)
                        }
                    }
                    for (enableRedefinition in listOf(false, true)) {
                        val taskName = when (enableRedefinition) {
                            false -> "runJvmAutoreload"
                            true -> "runJvmAutoreloadWithRedefinition"
                        }
                        createThis<KorgeJavaExec>(taskName) {
                            dependsOn(":korge-reload-agent:jar", "compileKotlinJvm")
                            group = "run"
                            mainClass.set("MainKt")
                            afterEvaluate {
                                val agentJarTask: Jar = project(":korge-reload-agent").tasks.findByName("jar") as Jar
                                val outputJar = agentJarTask.outputs.files.files.first()
                                //println("agentJarTask=$outputJar")
                                val compileKotlinJvm = tasks.findByName("compileKotlinJvm") as org.jetbrains.kotlin.gradle.tasks.KotlinCompile
                                val args = compileKotlinJvm.outputs.files.toList().joinToString(":::") { it.absolutePath }
                                //val gradlewCommand = if (isWindows) "gradlew.bat" else "gradlew"
                                //val continuousCommand = "${rootProject.rootDir}/$gradlewCommand --no-daemon --warn --project-dir=${rootProject.rootDir} --configuration-cache -t ${project.path}:compileKotlinJvmAndNotify"
                                val continuousCommand =
                                    "-classpath ${rootProject.rootDir}/gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain --no-daemon --warn --project-dir=${rootProject.rootDir} --configuration-cache -t ${project.path}:compileKotlinJvmAndNotify"
                                jvmArgs("-javaagent:$outputJar=$httpPort:::$continuousCommand:::$enableRedefinition:::$args")
                                environment("KORGE_AUTORELOAD", "true")
                            }
                        }
                    }

                    // esbuild
                    run {

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
                            rootProject.tasks.createThis<Exec>(npmInstallEsbuild) {
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
                                    commandLine(
                                        *npmCmd,
                                        "-g",
                                        "install",
                                        "esbuild@$esbuildVersion",
                                        "--prefix",
                                        esbuildFolder,
                                        "--scripts-prepend-node-path",
                                        "true"
                                    )
                                }
                            }
                        }

                        afterEvaluate {
                            val browserEsbuildResources = tasks.createThis<Copy>("browserEsbuildResources") {
                                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
                                from(project.tasks.getByName("jsProcessResources").outputs.files)
                                afterEvaluate {
                                    project.tasks.findByName("korgeProcessedResourcesJsMain")?.outputs?.files?.let {
                                        from(it)
                                    }
                                }
                                //for (sourceSet in gkotlin.js().compilations.flatMap { it.kotlinSourceSets }) from(sourceSet.resources)
                                into(wwwFolder)
                            }
                            val compileDevelopmentExecutableKotlinJs = "compileDevelopmentExecutableKotlinJs"
                            val runJs = createThis<Exec>("runJs") {
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

                    val runJsWebpack = createThis<Task>("runJsWebpack") {
                        group = "run"
                        //dependsOn("jsBrowserDevelopmentRun")
                        dependsOn("jsBrowserProductionRun")
                    }

                    val runNativeDebug = createThis<Task>("runNativeDebug") {
                        group = "run"
                        dependsOnNativeTask("Debug")
                    }
                    val runNativeRelease = createThis<Task>("runNativeRelease") {
                        group = "run"
                        dependsOnNativeTask("Release")
                    }
                    if (!isWindows) {
                        afterEvaluate {
                            for (type in CrossExecType.VALID_LIST) {
                                for (deb in listOf("Debug", "Release")) {
                                    val linkTask = project.tasks.findByName("link${deb}Executable${type.nameWithArchCapital}") as? KotlinNativeLink? ?: continue
                                    tasks.createThis<Exec>("runNative${deb}${type.interpCapital}") {
                                        group = "run"
                                        dependsOn(linkTask)
                                        commandLineCross(linkTask.binary.outputFile.absolutePath, type = type)
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

                    val nativeDesktopTargets = nativeTargets(project)
                    val allNativeTargets = nativeDesktopTargets

                    //for (target in nativeDesktopTargets) {
                    //target.compilations["main"].defaultSourceSet.dependsOn(nativeDesktopEntryPointSourceSet)
                    //    target.compilations["main"].defaultSourceSet.kotlin.srcDir(nativeDesktopFolder)
                    //}

                    for (target in allNativeTargets) {
                        for (binary in target.binaries) {
                            val compilation = binary.compilation
                            val copyResourcesTask = tasks.createThis<Copy>("copyResources${target.name.capitalize()}${binary.name.capitalize()}") {
                                dependsOn(getKorgeProcessResourcesTaskName(target, compilation))
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

            project.tasks {
                val runJvm = getByName("runJvm") as KorgeJavaExec
                //val prepareResourceProcessingClasses = create("prepareResourceProcessingClasses", Copy::class) {
                //    dependsOn(jvmMainClasses)
                //    afterEvaluate {
                //        from(runJvm.korgeClassPath.toList().map { if (it.extension == "jar") zipTree(it) else it })
                //    }
                //    into(File(project.buildDir, "korgeProcessedResources/classes"))
                //}

                for (target in project.gkotlin.targets) {
                    for (compilation in target.compilations) {
                        val processedResourcesFolder = File(project.buildDir, "korgeProcessedResources/${target.name}/${compilation.name}")
                        compilation.defaultSourceSet.resources.srcDir(processedResourcesFolder)
                        val korgeProcessedResources = createThis<Task>(getKorgeProcessResourcesTaskName(target, compilation)) {
                            //dependsOn(prepareResourceProcessingClasses)
                            dependsOn("jvmMainClasses")

                            if (project.enabledSandboxResourceProcessor) {
                                doLast {
                                    processedResourcesFolder.mkdirs()
                                    //URLClassLoader(prepareResourceProcessingClasses.outputs.files.toList().map { it.toURL() }.toTypedArray(), ClassLoader.getSystemClassLoader()).use { classLoader ->

                                    URLClassLoader(
                                        runJvm.korgeClassPath.toList().map { it.toURL() }.toTypedArray(),
                                        ClassLoader.getSystemClassLoader()
                                    ).use { classLoader ->
                                        val clazz = classLoader.loadClass("com.soywiz.korge.resources.ResourceProcessorRunner")
                                        val folders = compilation.allKotlinSourceSets.flatMap { it.resources.srcDirs }
                                            .filter { it != processedResourcesFolder }.map { it.toString() }
                                        //println(folders)
                                        try {
                                            clazz.methods.first { it.name == "run" }.invoke(
                                                null,
                                                classLoader,
                                                folders,
                                                processedResourcesFolder.toString(),
                                                compilation.name
                                            )
                                        } catch (e: java.lang.reflect.InvocationTargetException) {
                                            val re = (e.targetException ?: e)
                                            re.printStackTrace()
                                            System.err.println(re.toString())
                                        }
                                    }
                                    System.gc()
                                }
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

    //println("currentJavaVersion=${com.soywiz.korlibs.currentJavaVersion()}")
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
                //tasks.withType(Test::class.java).allThis {
                tasks.withType(AbstractTestTask::class.java).allThis {
                    testLogging {
                        //setEvents(setOf("passed", "skipped", "failed", "standardOut", "standardError"))
                        it.setEvents(setOf("skipped", "failed", "standardError"))
                        it.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                    }
                }

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
val useMimalloc: Boolean get() = true
//val useMimalloc = false

val Project._libs: Dyn get() = rootProject.extensions.getByName("libs").dyn
val Project.kotlinVersion: String get() = _libs["versions"]["kotlin"].dynamicInvoke("get").casted()
val Project.nodeVersion: String get() = _libs["versions"]["node"].dynamicInvoke("get").casted()
val Project.androidBuildGradleVersion: String get() = _libs["versions"]["android"]["build"]["gradle"]["java11"].dynamicInvoke("get").casted()
val Project.realKotlinVersion: String get() = (System.getenv("FORCED_KOTLIN_VERSION") ?: kotlinVersion)
val forcedVersion = System.getenv("FORCED_VERSION")

val Project.hasAndroidSdk by LazyExt { AndroidSdk.hasAndroidSdk(project) }
val Project.enabledSandboxResourceProcessor: Boolean by LazyExt { rootProject.findProperty("enabledSandboxResourceProcessor") == "true" }
val Project.gitVersion: String by LazyExt {
    try {
        Runtime.getRuntime().exec("git describe --abbrev=8 --tags --dirty".split(" ").toTypedArray(), arrayOf(), rootDir).inputStream.reader()
            .readText().lines().first().trim()
    } catch (e: Throwable) {
        e.printStackTrace()
        "unknown"
    }
}

val Project.currentJavaVersion by LazyExt { com.soywiz.korlibs.currentJavaVersion() }
fun Project.hasBuildGradle() = listOf("build.gradle", "build.gradle.kts").any { File(projectDir, it).exists() }
val Project.isSample: Boolean get() = project.path.startsWith(":samples:") || project.path.startsWith(":korge-sandbox") || project.path.startsWith(":korge-editor") || project.path.startsWith(":korge-starter-kit")
fun Project.mustAutoconfigureKMM(): Boolean =
    project.name != "korge-gradle-plugin" &&
        project.name != "korge-reload-agent" &&
        project.hasBuildGradle()

fun getKorgeProcessResourcesTaskName(target: org.jetbrains.kotlin.gradle.plugin.KotlinTarget, compilation: org.jetbrains.kotlin.gradle.plugin.KotlinCompilation<*>): String =
    "korgeProcessedResources${target.name.capitalize()}${compilation.name.capitalize()}"

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

fun Project.runServer(blocking: Boolean) {
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

package com.soywiz.korlibs

import com.soywiz.korge.gradle.targets.*
import com.soywiz.korge.gradle.targets.android.*
import com.soywiz.korge.gradle.util.*
import com.soywiz.korlibs.modules.*
import kotlinx.kover.api.*
import org.gradle.api.*
import org.gradle.api.Project
import org.gradle.api.file.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.testing.*
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.plugins.ide.idea.model.*
import java.io.*
import java.nio.file.*
import kotlin.reflect.*

//val headlessTests = true
//val headlessTests = System.getenv("NON_HEADLESS_TESTS") != "true"
val headlessTests: Boolean get() = System.getenv("CI") == "true" || System.getenv("HEADLESS_TESTS") == "true"
val useMimalloc: Boolean get() = true
//val useMimalloc = false

val Project._libs: Dyn get() = rootProject.extensions.getByName("libs").dyn
val Project.kotlinVersion: String get() = _libs["versions"]["kotlin"].dynamicInvoke("get").casted()
val Project.nodeVersion: String get() = _libs["versions"]["node"].dynamicInvoke("get").casted()
val Project.androidBuildGradleVersion: String get() = _libs["versions"]["android"]["build"]["gradle"].dynamicInvoke("get").casted()
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

fun RootKorlibsPlugin(project: Project) {
    val rootProject = project.rootProject

    // Required by RC
    project.kotlin {
        // Forced Java8 toolchain
        //jvmToolchain { (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of("8")) }
        jvm()
    }

    project.allprojects {
        project.version = forcedVersion?.removePrefix("refs/tags/")?.removePrefix("v")?.removePrefix("w")
            ?: project.version
    }

    project.allprojects {
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

    project.allprojects {
        if (project.hasBuildGradle()) {
            val plugin = this.apply<IdeaPlugin>()
            val idea = this.extensions.getByType<IdeaModel>()

            idea.apply {
                module {
                    excludeDirs = excludeDirs + listOf(
                        file(".gradle"), file("src2"), file("original"), file("original-tests"), file("old-rendering"),
                        file("gradle"), file(".idea"), file("build"), file("@old"), file("_template"),
                        file("e2e-sample"), file("e2e-test"), file("experiments"),
                    )
                }
            }
        }
    }

    project.allprojects {
        val projectName = project.name
        val firstComponent = projectName.substringBefore('-')
        group = when {
            projectName == "korge-gradle-plugin" -> "com.soywiz.korlibs.korge.plugins"
            projectName == "korge-reload-agent" -> "com.soywiz.korlibs.korge.reloadagent"
            firstComponent == "korge" -> "com.soywiz.korlibs.korge2"
            else -> "com.soywiz.korlibs.$firstComponent"
        }
    }

    project.rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
        project.rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().nodeVersion = project.nodeVersion
    }

    project.rootProject.configureMavenCentralRelease()

    project.allprojects {
        tasks.withType(Copy::class.java).all {
            //this.duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.WARN
            this.duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
            //println("Task $this")
        }
    }

    run {
        //fileTree(new File(rootProject.projectDir, "buildSrc/src/main/kotlinShared"))
        //copy {
        project.symlinktree(
            fromFolder = File(rootProject.projectDir, "buildSrc/src/main/kotlin"),
            intoFolder = File(rootProject.projectDir, "korge-gradle-plugin/build/srcgen2")
        )

        project.symlinktree(
            fromFolder = File(rootProject.projectDir, "buildSrc/src/main/resources"),
            intoFolder = File(rootProject.projectDir, "korge-gradle-plugin/build/srcgen2res")
        )
    }

    rootProject.afterEvaluate {
        subprojects {
            val linkDebugTestMingwX64 = project.tasks.findByName("linkDebugTestMingwX64")
            if (linkDebugTestMingwX64 != null && isWindows && inCI) {
                linkDebugTestMingwX64.doFirst { exec { commandLine("systeminfo") } }
                linkDebugTestMingwX64.doLast { exec { commandLine("systeminfo") } }
            }

            val mingwX64Test = project.tasks.findByName("mingwX64Test")
            if (mingwX64Test != null && isWindows && inCI) {
                mingwX64Test.doFirst { exec { commandLine("systeminfo") } }
                mingwX64Test.doLast { exec { commandLine("systeminfo") } }
            }
        }
    }

    com.soywiz.korge.gradle.KorgeVersionsTask.registerShowKorgeVersions(project)

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

    // Install required libraries in Linux with APT
    run {
        if (
            org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_UNIX) &&
            (File("/.dockerenv").exists() || System.getenv("TRAVIS") != null || System.getenv("GITHUB_REPOSITORY") != null) &&
            (File("/usr/bin/apt-get").exists()) &&
            (!(File("/usr/include/GL/glut.h").exists()) || !(File("/usr/include/AL/al.h").exists()))
        ) {
            rootProject.exec { commandLine("sudo", "apt-get", "update") }
            rootProject.exec { commandLine("sudo", "apt-get", "-y", "install", "freeglut3-dev", "libopenal-dev") }
            // exec { commandLine("sudo", "apt-get", "-y", "install", "libgtk-3-dev") }
        }
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
    }

    // Kover
    run {
        rootProject.allprojects {
            apply<kotlinx.kover.KoverPlugin>()
        }

        rootProject.koverMerged {
            enable()
        }


// https://repo.maven.apache.org/maven2/org/jetbrains/intellij/deps/intellij-coverage-agent/1.0.688/
//val koverVersion = "1.0.688"
        val koverVersion = rootProject._libs["versions"]["kover"]["agent"].dynamicInvoke("get").casted<String>()

        rootProject.allprojects {
            kover {
                engine.set(kotlinx.kover.api.IntellijEngine(koverVersion))
            }
            extensions.getByType(kotlinx.kover.api.KoverProjectConfig::class.java).apply {
                engine.set(kotlinx.kover.api.IntellijEngine(koverVersion))
            }
            tasks.withType<Test> {
                extensions.configure(kotlinx.kover.api.KoverTaskExtension::class) {
                    //generateXml = false
                    //generateHtml = true
                    //coverageEngine = kotlinx.kover.api.CoverageEngine.INTELLIJ
                    excludes.add(".*BuildConfig")
                }
            }
        }

    }
}

/**
 * Retrieves the [koverMerged][kotlinx.kover.api.KoverMergedConfig] extension.
 */
val org.gradle.api.Project.`koverMerged`: kotlinx.kover.api.KoverMergedConfig get() =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.getByName("koverMerged") as kotlinx.kover.api.KoverMergedConfig

/**
 * Configures the [koverMerged][kotlinx.kover.api.KoverMergedConfig] extension.
 */
fun org.gradle.api.Project.`koverMerged`(configure: Action<KoverMergedConfig>): Unit =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("koverMerged", configure)


/**
 * Retrieves the [kover][kotlinx.kover.api.KoverProjectConfig] extension.
 */
val org.gradle.api.Project.`kover`: kotlinx.kover.api.KoverProjectConfig get() =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.getByName("kover") as kotlinx.kover.api.KoverProjectConfig

/**
 * Configures the [kover][kotlinx.kover.api.KoverProjectConfig] extension.
 */
fun org.gradle.api.Project.`kover`(configure: Action<kotlinx.kover.api.KoverProjectConfig>): Unit =
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("kover", configure)






import org.gradle.kotlin.dsl.kotlin
import org.jetbrains.kotlin.gradle.plugin.*
import java.io.File

buildscript {
    val androidBuildGradleVersion: String by project
    val gradlePublishPluginVersion: String by project
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven { url = uri("https://plugins.gradle.org/m2/") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    }
    dependencies {
        classpath("com.gradle.publish:plugin-publish-plugin:$gradlePublishPluginVersion")
        classpath("com.android.tools.build:gradle:$androidBuildGradleVersion")
    }
}

plugins {
    val kotlinVersion: String by project
    val realKotlinVersion = (System.getenv("FORCED_KOTLIN_VERSION") ?: kotlinVersion)

	java
    kotlin("multiplatform") version realKotlinVersion
}

val headlessTests = false
val useMimalloc = true
//val useMimalloc = false

val kotlinVersion: String by project
val realKotlinVersion = (System.getenv("FORCED_KOTLIN_VERSION") ?: kotlinVersion)
val coroutinesVersion: String by project
val jnaVersion: String by project
val androidBuildGradleVersion: String by project
val kotlinSerializationVersion: String by project

//println(KotlinVersion.CURRENT)

val forcedVersion = System.getenv("FORCED_VERSION")

allprojects {
    project.version = forcedVersion?.removePrefix("refs/tags/v")?.removePrefix("v") ?: project.version
}

allprojects {
	repositories {
        mavenLocal().content {
            excludeGroup("Kotlin/Native")
        }
		mavenCentral().content {
            excludeGroup("Kotlin/Native")
        }
        google().content {
            excludeGroup("Kotlin/Native")
        }
		maven { url = uri("https://plugins.gradle.org/m2/") }.content {
            excludeGroup("Kotlin/Native")
        }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }.content {
            excludeGroup("Kotlin/Native")
        }
	}
}

val enableKotlinNative: String by project
val doEnableKotlinNative get() = enableKotlinNative == "true"

val enableKotlinAndroid: String by project
val doEnableKotlinAndroid get() = enableKotlinAndroid == "true"

val enableKotlinMobile:String by project
val doEnableKotlinMobile get() = enableKotlinMobile == "true"

val enableKotlinRaspberryPi: String by project
val doEnableKotlinRaspberryPi get() = enableKotlinRaspberryPi == "true"

val KotlinTarget.isLinuxX64 get() = this.name == "linuxX64"
val KotlinTarget.isLinuxArm32Hfp get() = this.name == "linuxArm32Hfp" && doEnableKotlinRaspberryPi
val KotlinTarget.isLinux get() = isLinuxX64 || isLinuxArm32Hfp
val KotlinTarget.isWin get() = this.name == "mingwX64"
val KotlinTarget.isMacosX64 get() = this.name == "macosX64"
val KotlinTarget.isMacosArm64 get() = this.name == "macosArm64"
val KotlinTarget.isMacos get() = isMacosX64 || isMacosArm64
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
val isArm get() = listOf("arm", "arm64", "aarch64").any { org.apache.tools.ant.taskdefs.condition.Os.isArch(it) }

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
        isWindows -> listOfNotNull(mingwX64())
        //isMacos -> listOf(macosX64(), macosArm64())
        isMacos -> listOfNotNull(macosX64())
        else -> listOfNotNull(
            linuxX64(),
            mingwX64(),
            macosX64(),
            if (doEnableKotlinRaspberryPi) linuxArm32Hfp() else null
        )
    }
}

fun org.jetbrains.kotlin.gradle.dsl.KotlinTargetContainerWithPresetFunctions.mobileTargets(): List<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
    return listOf(iosArm64(), iosX64())
}

//apply(from = "${rootProject.rootDir}/build.idea.gradle")

fun Project.hasBuildGradle() = listOf("build.gradle", "build.gradle.kts").any { File(projectDir, it).exists() }
val Project.isSample: Boolean get() = project.path.startsWith(":samples:") || project.path.startsWith(":korge-editor")

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

subprojects {
    val doConfigure =
            project.name != "korge-gradle-plugin" &&
            project.hasBuildGradle()

    if (doConfigure) {
        val isSample = project.path.startsWith(":samples")
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

        // @TODO: When Kotlin/Native is enabled:
        // @TODO: Cannot change dependencies of dependency configuration ':kbignum:iosArm64MainImplementationDependenciesMetadata' after task dependencies have been resolved
        if (!doEnableKotlinNative && !doEnableKotlinMobile) {
            if (!isSample) {
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
                if (headlessTests) {
                    jvmTest.systemProperty("java.awt.headless", "true")
                }
            }
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
            }
            if (hasAndroid) {
                apply(from = "${rootProject.rootDir}/build.android.srcset.gradle")
            }

            val desktopAndMobileTargets = ArrayList<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget>().apply {
                if (doEnableKotlinNative) addAll(nativeTargets())
                if (doEnableKotlinMobile) addAll(mobileTargets())
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

                fun createPairSourceSet(name: String, vararg dependencies: PairSourceSet, block: org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.(test: Boolean) -> Unit = { }): PairSourceSet {
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
        project.afterEvaluate {
            //if (firstThread == true && OS.isMac) task.jvmArgs("-XstartOnFirstThread")
            classpath = korgeClassPath
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

nonSamples {
    plugins.apply("maven-publish")

    val javadocJar = tasks.maybeCreate<Jar>("javadocJar").apply { archiveClassifier.set("javadoc") }
    val sourcesJar = tasks.maybeCreate<Jar>("sourceJar").apply { archiveClassifier.set("sources") }
    //val emptyJar = tasks.maybeCreate<Jar>("emptyJar").apply {}

    extensions.getByType(PublishingExtension::class.java).apply {
        repositories {
            //maven {
            //    credentials {
            //        username = BINTRAY_USER
            //        password = BINTRAY_KEY
            //    }
            //    url = uri("https://api.bintray.com/maven/${project.property("project.bintray.org")}/${project.property("project.bintray.repository")}/${project.property("project.bintray.package")}/")
            //}
        }
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

                        // Workaround for kotlin-native cinterops without gradle metadata
                        //if (korlibs.cinterops.isNotEmpty()) {
                        //    val dependenciesList = (this.get("dependencies") as NodeList)
                        //    if (dependenciesList.isNotEmpty()) {
                        //        (dependenciesList.first() as Node).apply {
                        //            for (cinterop in korlibs.cinterops.filter { it.targets.contains(publication.name) }) {
                        //                appendNode("dependency").apply {
                        //                    appendNode("groupId").setValue("${project.group}")
                        //                    appendNode("artifactId").setValue("${project.name}-${publication.name.toLowerCase()}")
                        //                    appendNode("version").setValue("${project.version}")
                        //                    appendNode("type").setValue("klib")
                        //                    appendNode("classifier").setValue("cinterop-${cinterop.name}")
                        //                    appendNode("scope").setValue("compile")
                        //                    appendNode("exclusions").apply {
                        //                        appendNode("exclusion").apply {
                        //                            appendNode("artifactId").setValue("*")
                        //                            appendNode("groupId").setValue("*")
                        //                        }
                        //                    }
                        //                }
                        //            }
                        //        }
                        //    }
                        //}

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
}

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

val gitVersion = try {
    Runtime.getRuntime().exec("git describe --abbrev=8 --tags --dirty".split(" ").toTypedArray(), arrayOf(), rootDir).inputStream.reader()
        .readText().lines().first().trim()
} catch (e: Throwable) {
    e.printStackTrace()
    "unknown"
}


val buildVersionsFile = file("korge-gradle-plugin/build/srcgen/com/soywiz/korge/gradle/BuildVersions.kt")
val oldBuildVersionsText = buildVersionsFile.takeIf { it.exists() }?.readText()
val projectVersion = project.version
val newBuildVersionsText = """
package com.soywiz.korge.gradle

object BuildVersions {
    const val GIT = "$gitVersion"
    const val KOTLIN = "$realKotlinVersion"
    const val JNA = "$jnaVersion"
    const val COROUTINES = "$coroutinesVersion"
    const val ANDROID_BUILD = "$androidBuildGradleVersion"
    const val KOTLIN_SERIALIZATION = "$kotlinSerializationVersion"
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

allprojects {
    //println("GROUP: $group")
}

subprojects {
    afterEvaluate {
        tasks {
            val publishJvmLocal by creating(Task::class) {
                if (findByName("publishKotlinMultiplatformPublicationToMavenLocal") != null) {
                    dependsOn("publishJvmPublicationToMavenLocal")
                    //dependsOn("publishMetadataPublicationToMavenLocal")
                    dependsOn("publishKotlinMultiplatformPublicationToMavenLocal")
                } else if (findByName("publishToMavenLocal") != null) {
                    dependsOn("publishToMavenLocal")
                }
            }

            val publishJsLocal by creating(Task::class) {
                if (findByName("publishKotlinMultiplatformPublicationToMavenLocal") != null) {
                    dependsOn("publishJsPublicationToMavenLocal")
                    //dependsOn("publishMetadataPublicationToMavenLocal")
                    dependsOn("publishKotlinMultiplatformPublicationToMavenLocal")
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

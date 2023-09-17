import com.android.build.gradle.*
import org.jetbrains.kotlin.gradle.plugin.*

//buildscript {
//    repositories {
//        mavenLocal()
//        mavenCentral()
//        google()
//        gradlePluginPortal()
//    }
//    dependencies {
//        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
//    }
//}
val isSandbox = project.name == "korge-sandbox"
val enableAndroid = true
val enableNative = true

plugins {
    id("publishing")
    id("maven-publish")
    id("signing")
    kotlin("multiplatform") version libs.versions.kotlin
    id("com.android.library") version libs.versions.android.build.gradle
    //id("com.android.application") apply false
    idea
}

var File.text: String get() = this.readText(); set(value) { this.also { it.parentFile.mkdirs() }.writeText(value) }
val isJava8or9 = System.getProperty("java.version").startsWith("1.8") || System.getProperty("java.version").startsWith("9")
if (isJava8or9) throw Exception("At least Java 11 is required")

var projectVersion = System.getenv("FORCED_VERSION")
    ?.replaceFirst("^refs/tags/", "")
    ?.replaceFirst("^v", "")
    ?.replaceFirst("^w", "")
    ?.replaceFirst("^z", "")
    ?: "999.0.0.999"
    //?: props.getProperty("version")

if (projectVersion.contains("-only-gradle-plugin-")) {
    val parts = projectVersion.split("-only-gradle-plugin-")
    projectVersion = parts.last()
}

if (System.getenv("FORCED_VERSION") != null) {
    println(":: FORCED_VERSION=${System.getenv("FORCED_VERSION")}")
    println(":: projectVersion=$projectVersion")
}


allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap") }
        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/temporary") }
        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev") }
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven") }
        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
    }

    //group = "com.soywiz.korge"
    group = "com.soywiz.korlibs.korge2"
    version = projectVersion
}

kotlin {
    jvm()
    val android = extensions.getByName<TestedExtension>("android")
    android.compileSdkVersion(31)
    androidTarget {
        //android.compileSdkVersion = "android-31"
    }
}

subprojects {
    if (project.name == "korge-gradle-plugin") return@subprojects
    if (project.name == "korge-reload-agent") return@subprojects

    //id("publishing")
    //id("maven-publish")
    //id("signing")

    apply(plugin = "org.jetbrains.kotlin.multiplatform")
    //apply(plugin = "kotlin")
    apply(plugin = "maven-publish")
    //apply<KotlinMultiplatformPlugin>()
    //apply(ApplicationPlugin::class)

    //java {
    //    targetCompatibility = JavaVersion.VERSION_1_8
    //    sourceCompatibility = JavaVersion.VERSION_1_8
    //}

    fun KotlinTarget.configureTarget() {
        compilations.configureEach {
            val options = compilerOptions.options
            options.suppressWarnings.set(true)
            options.freeCompilerArgs.apply {
                add("-Xskip-prerelease-check")
                if (project.findProperty("enableMFVC") == "true") add("-Xvalue-classes")
                if (target.name == "android" || target.name == "jvm") add("-Xno-param-assertions")
                add("-opt-in=kotlinx.cinterop.ExperimentalForeignApi")
            }
        }
    }

    kotlin {
        applyDefaultHierarchyTemplate()
        metadata().configureTarget()
        jvm {
            compilations.all {
                kotlinOptions.jvmTarget = "1.8"
                kotlinOptions.suppressWarnings = true
                //kotlinOptions.freeCompilerArgs = listOf("-Xuse-k2")
                kotlinOptions.freeCompilerArgs = listOf()
            }
            //withJava()
            testRuns["test"].executionTask.configure {
                useJUnit()
                testLogging { exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL }
            }
            configureTarget()
        }
        js(IR) {
            binaries.executable()
            //useCommonJs()
            //nodejs()
            useEsModules()
            browser {
            }
            compilations.all {
                kotlinOptions.suppressWarnings = true
                //kotlinOptions.freeCompilerArgs = listOf()
            }
            configureTarget()
        }

        if (enableAndroid) {
            if (isSandbox) {
                apply(plugin = "com.android.application")
            } else {
                apply(plugin = "com.android.library")
            }
            val android = extensions.getByName<TestedExtension>("android")
            android.compileSdkVersion(31)
            androidTarget {
                //android.compileSdkVersion = "android-31"
                configureTarget()
            }
        }

        iosX64().configureTarget()
        iosArm64().configureTarget()
        iosSimulatorArm64().configureTarget()
        tvosX64().configureTarget()
        tvosArm64().configureTarget()
        tvosSimulatorArm64().configureTarget()

        @Suppress("OPT_IN_USAGE")
        wasmJs {
            this.useEsModules()
            browser {
            }
            compilations.all {
                kotlinOptions.suppressWarnings = true
                //    kotlinOptions.freeCompilerArgs = listOf()
            }
            configureTarget()
        }
        sourceSets {
            val commonMain by getting {
                //kotlin.setSrcDirs(listOf("src/common"))
                //resources.setSrcDirs(listOf("srcresources"))
            }
            val commonTest by getting {
                //kotlin.setSrcDirs(listOf("test/common"))
                //resources.setSrcDirs(listOf("testresources"))

                dependencies {
                    api(kotlin("test"))
                }
            }
            val jvmMain by getting {
                //kotlin.setSrcDirs(listOf("src/jvm"))
                //resources.setSrcDirs(listOf<String>())

                dependencies {
                    //api(libs.jna.core)
                    //api(libs.jna.platform)
                }
            }
            val jvmTest by getting {
                //kotlin.setSrcDirs(listOf("test/jvm"))
                //resources.setSrcDirs(listOf<String>())
                dependencies {
                    api(kotlin("test"))
                }
            }
            val jsMain by getting {
                //kotlin.setSrcDirs(listOf("src/js"))
                //resources.setSrcDirs(listOf<String>())
                dependencies {
                }
            }
            val jsTest by getting {
                //kotlin.setSrcDirs(listOf("test/js"))
                //resources.setSrcDirs(listOf<String>())
            }
            val wasmJsMain by getting {
                //kotlin.setSrcDirs(listOf("src/wasm"))
                //resources.setSrcDirs(listOf<String>())
                dependencies {
                }
            }
            val wasmJsTest by getting {
                //kotlin.setSrcDirs(listOf("test/wasm"))
                //resources.setSrcDirs(listOf<String>())
            }
            val jvmAndroidMain by creating {
            }
            val jvmAndroidTest by creating {
                //dependsOn(androidTest.get())
            }
            val concurrentMain by creating {
            }
            val concurrentTest by creating {
            }

            concurrentMain.dependsOn(commonMain)
            concurrentTest.dependsOn(commonTest)

            jvmMain.dependsOn(jvmAndroidMain)
            jvmMain.dependsOn(concurrentMain)

            jvmTest.dependsOn(jvmAndroidTest)
            jvmTest.dependsOn(concurrentTest)

            if (enableNative) {
                val appleMain by getting { }
                val appleTest by getting { }
                appleMain.dependsOn(concurrentMain)
                appleTest.dependsOn(concurrentTest)
            }

            if (enableAndroid) {
                val androidMain by getting { }
                androidMain.dependsOn(jvmAndroidMain)
                androidMain.dependsOn(concurrentMain)
            }
        }

        tasks {
            val jvmTestProcessResources by getting(Copy::class) {
                this.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            }
        }
    }

    //if (path == ":korge") {
    //    //println("config=${configurations.toList()}")
    //    dependencies {
    //        add("commonMainImplementation", project(":korge-core"))
    //        //add("commonTestImplementation", project(path = ":korge-core", configuration = "commonTestImplementation"))
    //        //testImplementation(project(path = ":another-project", configuration = "testArtifacts"))
    //    }
    //}
}

idea {
    module {
        excludeDirs = excludeDirs + listOf(
            ".idea", ".gradle", "gradle/wrapper",
            "docs", "archive", "_template", "e2e",
            "kotlin-js-store",
        ).map { file(it) }
    }
}

/*
dependencies {
    implementation(libs.kover)
    implementation(libs.dokka)
    implementation(libs.proguard.gradle)
    implementation(libs.closure.compiler)
    implementation(libs.gson)
    implementation(libs.gradle.publish.plugin)
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.android.build.gradle)
    testImplementation(libs.junit)
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
    gradlePluginPortal()
    maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap") }
    maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/temporary") }
    maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev") }
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven") }
    maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
}

tasks.withType(KotlinCompile::class).all {
    kotlinOptions.suppressWarnings = true
}

tasks.withType(KotlinCompile::class).configureEach {
    kotlinOptions {
        jvmTarget = "11"
    }
}

java {
    val jversion = JavaVersion.VERSION_11
    sourceCompatibility = jversion
    targetCompatibility = jversion
    sourceSets.getByName("main").java {
        srcDir("src/main/kotlinGen")
    }
}


// Build versions

var gitVersion = "unset"
try {
    gitVersion = file("../.git/ORIG_HEAD").text.trim()
} catch (e: Throwable) {
}

if (System.getenv("FORCED_VERSION") != null) {
    try {
        gitVersion = Runtime.getRuntime().exec(
            "git describe --abbrev=8 --tags --dirty",
            arrayOf(),
            rootDir
        ).inputStream.readBytes().toString().trim()
    } catch (e: Throwable) {
        System.err.println(e.message)
    }
}

val props = Properties()
props.load(StringReader(file("gradle.properties").text))

var projectVersion = System.getenv("FORCED_VERSION")
    ?.replaceFirst("^refs/tags/", "")
    ?.replaceFirst("^v", "")
    ?.replaceFirst("^w", "")
    ?.replaceFirst("^z", "")
    ?: props.getProperty("version")

if (projectVersion.contains("-only-gradle-plugin-")) {
    val parts = projectVersion.split("-only-gradle-plugin-")
    projectVersion = parts.last()
}

if (System.getenv("FORCED_VERSION") != null) {
    println(":: FORCED_VERSION=${System.getenv("FORCED_VERSION")}")
    println(":: projectVersion=$projectVersion")
}

val realKotlinVersion = System.getenv("FORCED_KOTLIN_VERSION")
    ?: libs.versions.kotlin.get()

val buildVersionsString = """
package korlibs.korge.gradle

object BuildVersions {
    const val GIT = "---"
    const val KOTLIN = "${realKotlinVersion}"
    const val NODE_JS = "${libs.versions.node.get()}"
    const val JNA = "${libs.versions.jna.get()}"
    const val COROUTINES = "${libs.versions.kotlinx.coroutines.get()}"
    const val ANDROID_BUILD = "${libs.versions.android.build.gradle.get()}"
    const val KOTLIN_SERIALIZATION = "${libs.versions.kotlinx.serialization.get()}"
    const val KORGE_TEST = "$projectVersion"
    const val KORGE_CORE = "$projectVersion"
    const val KORGE_FOUNDATION = "$projectVersion"
    const val KORLIBS = "$projectVersion"
    const val KRYPTO = "$projectVersion" // Deprecated
    const val KLOCK = "$projectVersion" // Deprecated
    const val KDS = "$projectVersion" // Deprecated
    const val KMEM = "$projectVersion" // Deprecated
    const val KORMA = "$projectVersion" // Deprecated
    const val KORIO = "$projectVersion" // Deprecated
    const val KORIM = "$projectVersion" // Deprecated
    const val KORAU = "$projectVersion" // Deprecated
    const val KORGW = "$projectVersion" // Deprecated
    const val KORTE = "$projectVersion" // Deprecated
    const val KORGE = "$projectVersion"

    val ALL_PROPERTIES by lazy { listOf(
        ::GIT, ::KRYPTO, ::KLOCK, ::KDS, ::KMEM, 
        ::KORMA, ::KORIO, ::KORIM, ::KORAU, ::KORGW, ::KORGE,
         ::KOTLIN, ::JNA, ::COROUTINES, ::ANDROID_BUILD, ::KOTLIN_SERIALIZATION,
         ::KORGE_TEST, ::KORGE_CORE, ::KORGE_FOUNDATION,
    ) }
    val ALL by lazy { ALL_PROPERTIES.associate { it.name to it.get() } }
}
""".trim()

val buildVersionsStringForBuildSrc = buildVersionsString
val buildVersionsStringForPlugin = buildVersionsString.replace(
    "const val GIT = \"---\"",
    "const val GIT = \"${gitVersion}\""
)

val buildsVersionFilePlugin =
    file("korge-gradle-plugin/build/srcgen/korlibs/korge/gradle/BuildVersions.kt")

if (!buildsVersionFilePlugin.exists() || buildsVersionFilePlugin.text != buildVersionsStringForPlugin) {
    buildsVersionFilePlugin.parentFile.mkdirs()
    buildsVersionFilePlugin.text = buildVersionsStringForPlugin
}
*/

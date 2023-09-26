
import org.jetbrains.kotlin.gradle.tasks.*
import java.io.StringReader
import java.util.*

var File.text
    get() = this.readText();
    set(value) {
        this.also { it.parentFile.mkdirs() }.writeText(value)
    }

plugins {
    id("publishing")
    id("maven-publish")
    id("signing")
    id("org.jetbrains.kotlin.jvm") version libs.versions.kotlin
}

val isJava8or9 =
    System.getProperty("java.version").startsWith("1.8") || System.getProperty("java.version")
        .startsWith("9")

if (isJava8or9) {
    throw Exception("At least Java 11 is required")
}

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

//Eval.xy(this, this, file("../gradle/repositories.settings.gradle").text)
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
props.load(StringReader(file("../gradle.properties").text))

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
    const val KORGE = "$projectVersion"

    val ALL_PROPERTIES by lazy { listOf(
        ::GIT, ::KOTLIN, ::NODE_JS, ::JNA, ::COROUTINES, 
        ::ANDROID_BUILD, ::KOTLIN_SERIALIZATION, ::KORGE
    ) }
    val ALL by lazy { ALL_PROPERTIES.associate { it.name to it.get() } }
}
""".trim()

val buildVersionsStringForBuildSrc = buildVersionsString
val buildVersionsStringForPlugin = buildVersionsString.replace(
    "const val GIT = \"---\"",
    "const val GIT = \"${gitVersion}\""
)

val buildsVersionBuildSrcFile =
    file("../buildSrc/src/main/kotlinGen/korlibs/korge/gradle/BuildVersions.kt")
val buildsVersionFilePlugin =
    file("../korge-gradle-plugin/build/srcgen/korlibs/korge/gradle/BuildVersions.kt")

if (!buildsVersionBuildSrcFile.exists() || buildsVersionBuildSrcFile.text != buildVersionsStringForBuildSrc) {
    buildsVersionBuildSrcFile.parentFile.mkdirs()
    buildsVersionBuildSrcFile.text = buildVersionsStringForBuildSrc
}
if (!buildsVersionFilePlugin.exists() || buildsVersionFilePlugin.text != buildVersionsStringForPlugin) {
    buildsVersionFilePlugin.parentFile.mkdirs()
    buildsVersionFilePlugin.text = buildVersionsStringForPlugin
}

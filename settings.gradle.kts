pluginManagement {
//    Eval.xy(this, it, file('./gradle/repositories.settings.gradle').text)
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
}

val enableMetalPlayground: String by settings

rootProject.name = "${rootDir.name}-root"

fun isPropertyTrue(name: String): Boolean {
    return System.getenv(name) == "true" || System.getProperty(name) == "true"
}

val inCI = isPropertyTrue("CI")
val disabledExtraKorgeLibs = isPropertyTrue("DISABLED_EXTRA_KORGE_LIBS")

include(":korcoutines")
include(":klock")
include(":klogger")
include(":korinject")
include(":kmem")
include(":kds")
include(":korma")
include(":krypto")
include(":korte")
include(":korio")
include(":korim")
include(":korau")
include(":korgw")
include(":korge")
include(":ktruth")
include(":korge-gradle-plugin")
include(":korge-reload-agent")
include(":korge-sandbox")
if (enableMetalPlayground != "false" && !inCI) {
    include(":osx-metal-playground")
}

//if (!inCI || System.getenv("ENABLE_BENCHMARKS") == "true") {
if (System.getenv("ENABLE_BENCHMARKS") == "true") {
    include(":korge-benchmarks")
}

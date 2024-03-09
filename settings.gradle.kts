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

include(":korlibs-concurrent")
include(":korlibs-logger")
include(":korlibs-platform")
include(":korlibs-datastructure")
include(":korlibs-memory")
include(":korlibs-util")
include(":korlibs-ffi")
include(":korlibs-wasm")
include(":korlibs-math-core")
include(":korlibs-math")
include(":korlibs-inject")
include(":korlibs-time")
include(":korlibs-bignumber")
include(":korlibs-annotations")
include(":korlibs-crypto")
include(":korlibs-number")
include(":korlibs-serialization-yaml")
include(":korlibs-serialization-toml")
include(":korlibs-serialization-csv")
include(":korlibs-template")
include(":korlibs-io")
include(":korge-foundation")
include(":korge-core")
include(":korge")
include(":korge-gradle-plugin")
include(":korge-gradle-plugin-common")
include(":korge-gradle-plugin-settings")
include(":korge-reload-agent")
if (System.getenv("DISABLE_SANDBOX") != "true") {
    include(":korge-sandbox")
}
if (!inCI || System.getenv("ENABLE_BENCHMARKS") == "true") {
    include(":korge-benchmarks")
}

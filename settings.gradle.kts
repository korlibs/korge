pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
    }
    includeBuild("korge-gradle-plugin")
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven {
            name = "Central Portal Snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
            content {
                // Only consume org.korge.korlibs snapshots
                includeGroup("org.korge.korlibs")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(
    ":korge",
    ":korge-core",
    ":korge-ipc",
    ":korge-reload-agent",
    // Korge sandbox: shared game code (library) + one application module per entry point
    ":korge-sandbox:shared",
    ":korge-sandbox:jvmApp",
    ":korge-sandbox:androidApp",
    ":korge-sandbox:webApp",
)

val enableMetalPlayground: String by settings

rootProject.name = "korge-root"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

fun isPropertyTrue(name: String): Boolean {
    return System.getenv(name) == "true" || System.getProperty(name) == "true"
}

// The iOS entry point links a native framework, which can only be built on macOS.
// Gate it so the build stays green on Linux/Windows.
if (System.getProperty("os.name").startsWith("Mac")) {
    include(":korge-sandbox:iosApp")
}

val inCI = isPropertyTrue("CI")
val disabledExtraKorgeLibs = isPropertyTrue("DISABLED_EXTRA_KORGE_LIBS")

//if (System.getenv("DISABLE_SANDBOX") != "true") {
//    include(
//        ":korge-sandbox:shared",
//        ":korge-sandbox:androidApp",
//    )
//}

if (!inCI || System.getenv("ENABLE_BENCHMARKS") == "true") {
    include(":korge-benchmarks")
}

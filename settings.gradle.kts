pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
    }
    includeBuild("korge-gradle-plugins")
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
    ":korge-sandbox:shared",
    ":korge-sandbox:androidApp",
)

val enableMetalPlayground: String by settings

rootProject.name = "korge-root"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

fun isPropertyTrue(name: String): Boolean {
    return System.getenv(name) == "true" || System.getProperty(name) == "true"
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

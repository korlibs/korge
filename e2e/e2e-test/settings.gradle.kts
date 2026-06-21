enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        mavenLocal()
        maven { url = uri("https://central.sonatype.com/repository/maven-snapshots") }
        mavenCentral()
        google()
        gradlePluginPortal()
    }
    //Eval.xy(this, it, file('../../gradle/repositories.settings.gradle').text)
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(
    ":shared",
    ":androidApp",
    // Uncomment once korge plugin is migrated
//    ":desktopApp",
//    ":webApp",
)

pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "kotlin-multiplatform") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${requested.version}")
            }
        }
    }

    repositories {
        mavenLocal()
        mavenCentral()
        maven { url = uri("https://plugins.gradle.org/m2/") }
        maven { url = uri("https://dl.bintray.com/kotlin/kotlin-eap") }
    }
}

enableFeaturePreview("GRADLE_METADATA")

rootProject.name = java.util.Properties().apply { load(File(rootProject.projectDir, "gradle.properties").readText().reader()) }.getProperty("project.name")

include(":korge")
include(":korge-admob")
include(":korge-box2d")
include(":korge-dragonbones")
//include(":korge-spriter")
include(":korge-swf")
//include(":korge-ui")

//includeBuild("plugins")

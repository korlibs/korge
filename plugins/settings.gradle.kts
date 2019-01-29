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

rootProject.name = "korge-plugins"

include(":korge-build")
include(":korge-gradle-plugin")

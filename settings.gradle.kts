rootProject.name = "korlibs-root"

pluginManagement {
	repositories {
		mavenCentral()
		gradlePluginPortal()
		maven ("https://dl.bintray.com/kotlin/kotlin-eap")
	}
}

enableFeaturePreview("GRADLE_METADATA")

/*
for (file in rootDir.listFiles()) {
	if (file.name == "build" || file.name == "buildSrc" || file.name.startsWith(".")) continue
	if (
			setOf("build.gradle", "build.gradle.kts").map { File(file, it) }.any { it.exists() } &&
			setOf("settings.gradle", "settings.gradle.kts").map { File(file, it) }.all { !it.exists() }
	) {
		include(":${file.name}")
	}
}
*/
include(":kbignum")
include(":klock")
include(":klogger")
include(":korinject")
include(":kmem")
include(":kds")
include(":korma")
include(":korma-shape")
include(":luak")
include(":krypto")
include(":korte")
include(":korio")
include(":korim")

rootProject.name = "korlibs-next"

pluginManagement {
	repositories {
		mavenCentral()
		gradlePluginPortal()
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
	}
}

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
include(":luak")
include(":ktjs")
include(":krypto")
include(":korte")
include(":korio")
include(":korim")
include(":korau")
include(":korgw")
include(":korvi")
include(":kbox2d")
include(":korge")
include(":korge-editor")
include(":korge-dragonbones")
include(":korge-spine")
include(":korge-swf")
include(":korge-gradle-plugin")

/*
for (sample in (File(rootProject.projectDir, "samples").takeIf { it.isDirectory }?.listFiles() ?: arrayOf())) {
    if (File(sample, "build.gradle.kts").exists() || File(sample, "build.gradle").exists()) {
        include(":samples:${sample.name}")
    }
}
*/

//val skipKorgeSamples = System.getenv("SKIP_KORGE_SAMPLES") == "true"
val skipKorgeSamples = true

if (!skipKorgeSamples) {
    fileTree(File(rootProject.projectDir, "samples")) {
        include("**" + "/build.gradle.kts")
        include("**" + "/build.gradle")
        exclude("**" + "/build/**")
    }.forEach {
        val sample = moduleName(it.parentFile)
        include(":$sample")
        //project(":$sample").projectDir = File(relativePath(it.parent))
    }
}

fun moduleName(f: File): String {
    return if (f.parentFile == rootDir) {
        f.name
    } else {
        val p = moduleName(f.parentFile)
        "${p}:${f.name}"
    }
}

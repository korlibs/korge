rootProject.name = "korlibs-next"

pluginManagement {
	repositories {
		mavenCentral()
		gradlePluginPortal()

        //val gradleProperties = java.util.Properties().also { it.load(File(rootDir, "gradle.properties").readText().reader()) }
        //val kotlinVersion = gradleProperties["kotlinVersion"].toString()
        val kotlinVersion: String by settings

        if (kotlinVersion.contains("-M") || kotlinVersion.contains("-RC") || kotlinVersion.contains("eap") || kotlinVersion.contains("-release")) {
            maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/temporary")
            maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven")
        }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
	}
}

val inCI = System.getProperty("CI") == "true"

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
include(":krypto")
include(":korte")
include(":korio")
include(":korim")
include(":korau")
include(":korgw")
include(":korvi")
include(":kbox2d")
include(":korge")
include(":korge-dragonbones")
include(":korge-spine")
include(":korge-swf")
include(":korge-box2d")
include(":korge-gradle-plugin")
//include(":tensork")

if (!inCI) {
    include(":korge-sandbox")
}

/*
for (sample in (File(rootProject.projectDir, "samples").takeIf { it.isDirectory }?.listFiles() ?: arrayOf())) {
    if (File(sample, "build.gradle.kts").exists() || File(sample, "build.gradle").exists()) {
        include(":samples:${sample.name}")
    }
}
*/

//val skipKorgeSamples = System.getenv("SKIP_KORGE_SAMPLES") == "true"
val skipKorgeSamples = true

if (!skipKorgeSamples && !inCI) {
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

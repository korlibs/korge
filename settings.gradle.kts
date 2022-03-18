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

fun isPropertyTrue(name: String): Boolean =
    System.getenv(name) == "true" || System.getProperty(name) == "true"

val inCI = isPropertyTrue("CI")
val includeKorlibsSamples = isPropertyTrue("INCLUDE_KORLIBS_SAMPLES")
val disabledExtraKorgeLibs = isPropertyTrue("DISABLED_EXTRA_KORGE_LIBS")

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
//include(":kbignum")
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
include(":korvi")
include(":korge")

if (!inCI) {
    include(":korge-sandbox")
}

if (!disabledExtraKorgeLibs) {
    include(":luak")
    include(":kbox2d")
    include(":korge-dragonbones")
    include(":korge-spine")
    include(":korge-swf")
    include(":korge-box2d")
    include(":korge-gradle-plugin")
    include(":korge-fleks")

    //include(":samples:parallax-scrolling-aseprite")
    //include(":samples:tiled-background")
    //include(":samples:fleks-ecs")

    // This is required because having tons of gradle modules is super slow
    val skipKorgeSamples = !includeKorlibsSamples

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
}

fun moduleName(f: File): String {
    return if (f.parentFile == rootDir) {
        f.name
    } else {
        val p = moduleName(f.parentFile)
        "${p}:${f.name}"
    }
}

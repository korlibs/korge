description = "I/O utilities for Kotlin"

val coroutinesVersion: String by project

val enableKotlinNative: String by project
val doEnableKotlinNative get() = enableKotlinNative == "true"

val isWindows get() = org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_WINDOWS)
val isMacos get() = org.apache.tools.ant.taskdefs.condition.Os.isFamily(org.apache.tools.ant.taskdefs.condition.Os.FAMILY_MAC)

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
            }
        }
    }
}
dependencies {
	add("commonMainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    add("commonMainApi", project(":klock"))
	add("commonMainApi", project(":kds"))
	add("commonMainApi", project(":kmem"))
    add("commonMainApi", project(":krypto"))
    add("commonMainApi", project(":klogger"))

    afterEvaluate {
        if (configurations.findByName("androidMainApi") != null) {
            add("androidMainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
        }
    }
}

if (doEnableKotlinNative) {
    kotlin {
        if (isWindows) {
            mingwX64().compilations["main"].cinterops {
                maybeCreate("win32_ssl_socket")
            }
        }
    }
}
/*
import com.soywiz.korlibs.korlibs

apply<com.soywiz.korlibs.KorlibsPlugin>()

val klockVersion: String by project
val kdsVersion: String by project
val kmemVersion: String by project
val coroutinesVersion: String by project

dependencies {


    add("commonMainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    if (korlibs.hasAndroid) {
        add("androidMainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
    }
	add("commonMainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core-common:$coroutinesVersion")
	//add("jsMainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$coroutinesVersion")
	add("jvmMainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

	add("iosArm64MainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core-iosarm64:$coroutinesVersion")
	add("iosX64MainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core-iosx64:$coroutinesVersion")

    if (korlibs.watchosEnabled) {
        add("watchosArm32MainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core-watchosarm32:$coroutinesVersion")
        add("watchosArm64MainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core-watchosarm64:$coroutinesVersion")
        add("watchosX86MainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core-watchosx86:$coroutinesVersion")
    }

    if (korlibs.tvosEnabled) {
        add("tvosArm64MainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core-tvosarm64:$coroutinesVersion")
        add("tvosX64MainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core-tvosx64:$coroutinesVersion")
    }

    if (korlibs.linuxEnabled) {
        add("linuxX64MainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core-linuxx64:$coroutinesVersion")
    }
	add("mingwX64MainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core-mingwx64:$coroutinesVersion")
	add("macosX64MainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core-macosx64:$coroutinesVersion")
}
*/

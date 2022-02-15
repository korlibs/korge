description = "I/O utilities for Kotlin"

val coroutinesVersion: String by project

val enableKotlinNative: String by project
val kryptoVersion: String by project
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

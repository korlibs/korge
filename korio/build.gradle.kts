import com.soywiz.korlibs.modules.*

description = "I/O utilities for Kotlin"

val coroutinesVersion: String by project

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
        for (target in nativeTargets(project)) {
            if (target.isWin) {
                target.compilations["main"].cinterops {
                    maybeCreate("win32ssl")
                }
            }
        }
    }
}

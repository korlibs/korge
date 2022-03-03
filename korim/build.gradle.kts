import com.soywiz.korlibs.modules.*

description = "Korim: Kotlin cORoutines IMaging utilities for JVM, JS, Native and Common"

if (doEnableKotlinNative) {
    kotlin {
        for (target in nativeTargets(project)) {
            if (target.isLinux) {
                target.compilations["main"].cinterops {
                    maybeCreate("stb_image")
                }
            }
        }
    }
}

dependencies {
	add("commonMainApi", project(":korio"))
	add("commonMainApi", project(":korma"))
    //add("commonTestApi", project(":korma-shape"))
}

import com.soywiz.korlibs.modules.*

description = "Portable Audio library for Kotlin"

val jnaVersion: String by project

if (doEnableKotlinNative) {
    kotlin {
        for (target in allNativeTargets(project)) {
            target.compilations["main"].cinterops {
                maybeCreate("minimp3")
                maybeCreate("stb_vorbis")
            }
        }

    }
}

dependencies {
    add("commonMainApi", project(":korio"))
    add("jvmMainApi", "net.java.dev.jna:jna:$jnaVersion")
    add("jvmMainApi", "net.java.dev.jna:jna-platform:$jnaVersion")
}

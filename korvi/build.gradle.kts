import com.soywiz.korlibs.*

plugins {
    kotlin("multiplatform").apply(false)
}

apply<KorlibsPlugin>()

korlibs {
    exposeVersion()
}

//kotlin {
//    linuxX64().apply {
//        compilations["main"].cinterops {
//            val ffmpeg_min by creating {
//                //includeDirs.headerFilterOnly(rootProject.rootDir["ffmpeg/include"])
//                //includeDirs.(rootProject.rootDir["ffmpeg/include"])
//            }
//        }
//    }
//}

val korimVersion: String by project
val korauVersion: String by project
val jcodecVersion: String by project

dependencies {
    add("commonMainApi", "com.soywiz.korlibs.korim:korim:$korimVersion")
    add("commonMainApi", "com.soywiz.korlibs.korau:korau:$korauVersion")
    add("jvmMainApi", "org.jcodec:jcodec:$jcodecVersion")
}

/*
apply plugin: com.soywiz.korlibs.KorlibsPlugin
//apply plugin: com.soywiz.korlibs.KorlibsPluginNoNativeNoAndroid

korlibs {
    exposeVersion()
    def nativeTargets = [
        "mingwX64", "macosX64",
        *(korlibs.linuxEnabled ? ["linuxX64"] : []),
        "iosX64", "iosArm32", "iosArm64",
        "tvosX64", "tvosArm64",
        "watchosX86", "watchosArm32", "watchosArm64"
    ]
    //dependencyCInterops("mac_OpenAL", ["linuxX64"])
    dependencyCInterops("min_ffmpeg", ["linuxX64"])
}

dependencies {
    commonMainApi("com.soywiz.korlibs.korim:korim:$korimVersion")
    commonMainApi("com.soywiz.korlibs.korau:korau:$korauVersion")
    jvmMainApi("org.jcodec:jcodec:0.2.5")
    //jvmMainApi(project(":korvi-jcodec"))
}
*/

import com.soywiz.korlibs.modules.*

description = "Portable UI with accelerated graphics support for Kotlin"

val jnaVersion: String by project

if (doEnableKotlinNative) {
    kotlin {
        for (target in nativeTargets(project)) {
            if (target.name == "linuxX64" || target.name == "linuxArm32Hfp") {
                target.compilations["main"].cinterops {
                    maybeCreate("X11Embed")
                }
                //if (target.name == "linuxX64") maybeCreate("X11")
            }
        }
    }
}

dependencies {
    add("commonMainApi", project(":korim"))
    add("jvmMainApi", "net.java.dev.jna:jna:$jnaVersion")
    add("jvmMainApi", "net.java.dev.jna:jna-platform:$jnaVersion")
    //if (hasAndroid) add("androidMainApi", "androidx.appcompat:appcompat:1.3.0")
}

afterEvaluate {
    kotlin {
        for (targetName in listOf("linuxX64", "linuxArm32Hfp", "macosX64", "macosArm64")) {
            //println("targetName=$targetName")
            val target = targets.findByName(targetName) ?: continue
            //println("target=$target")
            val folder = project.file("src/${targetName}Main/kotlin")
            //println(" - $folder")
            target.compilations["main"].defaultSourceSet.kotlin.srcDir(folder)
        }
    }
}

/*
import org.apache.tools.ant.taskdefs.condition.Os

apply plugin: com.soywiz.korlibs.KorlibsPlugin

korlibs {
    exposeVersion()
    //dependencyCInteropsExternal("com.soywiz.korlibs.korim:korim:$korimVersion", "stb_image", ["linuxX64", "iosX64", "iosArm64"])
    //dependencyCInterops("GL", ["linuxX64"])

    dependencies {
        if (hasAndroid) {
            add("androidMainApi", "com.android.support:appcompat-v7:28.0.0")
        }
    }
}

kotlin {
    if (korlibs.linuxEnabled) {
        linuxX64 {
            compilations.main {
                cinterops {
                    GL {
                        //print(it)
                        //def linuxFolder = new File(rootDir, "nlib/linuxX64")
                        //compilerOpts("-I${new File(linuxFolder, "include").absolutePath}")
                        //linkerOpts("-L${new File(linuxFolder, "lib/x86_64-linux-gnu").absolutePath}")
                    }
                    //gtk3 {
                    //    ["/opt/local/include", "/usr/include", "/usr/local/include"].forEach {
                    //        includeDirs(
                    //            "$it/atk-1.0",
                    //            "$it/gdk-pixbuf-2.0",
                    //            "$it/cairo",
                    //            "$it/harfbuzz",
                    //            "$it/pango-1.0",
                    //            "$it/gtk-3.0",
                    //            "$it/glib-2.0"
                    //        )
                    //    }
//
                    //    includeDirs(
                    //        "/opt/local/lib/glib-2.0/include",
                    //        "/usr/lib/x86_64-linux-gnu/glib-2.0/include",
                    //        "/usr/local/lib/glib-2.0/include"
                    //    )
                    //}
                }
            }
        }
    }
}

kotlin.sourceSets {
    jvmMain {
        resources.srcDir("libs")
    }
}

task runSample(type: JavaExec) {
    classpath = kotlin.targets.jvm.compilations.test.runtimeDependencyFiles
    main = 'com.soywiz.korgw.TestGameWindow'
}

task runSampleFirstThread(type: JavaExec) {
    classpath = kotlin.targets.jvm.compilations.test.runtimeDependencyFiles
    if (Os.isFamily(Os.FAMILY_MAC)) {
        jvmArgs "-XstartOnFirstThread"
    }
    main = 'com.soywiz.korgw.TestGameWindow'
}

task runSampleMainThread {
    dependsOn runSampleFirstThread
}
*/

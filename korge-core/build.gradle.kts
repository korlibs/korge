description = "Korge Core Libraries"

korlibs.NativeTools.configureAllCInterop(project, "minimp3")
korlibs.NativeTools.configureAllCInterop(project, "stb_vorbis")
korlibs.NativeTools.configureAllCInterop(project, "stb_image")
korlibs.NativeTools.configureCInteropWin32(project, "win32ssl")

dependencies {
    add("androidMainApi", "androidx.javascriptengine:javascriptengine:1.0.0-alpha05")
    add("commonMainApi", project(":korge-foundation"))
    //add("commonTestApi", project(":korge-test"))
    add("commonTestApi", libs.kotlinx.coroutines.test)
    add("jvmMainApi", libs.asm.core)
    add("jvmMainApi", libs.asm.util)
}

korlibs.NativeTools.configureAndroidDependency(project, libs.kotlinx.coroutines.android)

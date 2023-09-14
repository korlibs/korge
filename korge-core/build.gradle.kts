description = "Korge Core Libraries"

korlibs.NativeTools.configureAllCInterop(project, "minimp3")
korlibs.NativeTools.configureAllCInterop(project, "stb_vorbis")
korlibs.NativeTools.configureAllCInterop(project, "stb_image")

dependencies {
    add("commonMainApi", project(":korge-foundation"))
    add("commonTestApi", project(":korge-test"))
    add("commonTestApi", libs.kotlinx.coroutines.test)
}

korlibs.NativeTools.configureAndroidDependency(project, libs.kotlinx.coroutines.android)
korlibs.NativeTools.configureCInteropWin32(project, "win32ssl")

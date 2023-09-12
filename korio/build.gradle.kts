description = "I/O utilities for Kotlin"

dependencies {
    add("commonMainApi", project(":korge-foundation"))
}

korlibs.NativeTools.configureAndroidDependency(project, libs.kotlinx.coroutines.android)
korlibs.NativeTools.configureCInteropWin32(project, "win32ssl")

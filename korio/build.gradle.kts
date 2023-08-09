description = "I/O utilities for Kotlin"

dependencies {
    add("commonMainApi", libs.bundles.kotlinx.coroutines)
    add("commonMainApi", project(":klock"))
	add("commonMainApi", project(":kds"))
	add("commonMainApi", project(":kmem"))
    add("commonMainApi", project(":krypto"))
    add("commonMainApi", project(":klogger"))
}

korlibs.NativeTools.configureAndroidDependency(project, libs.kotlinx.coroutines.android)
korlibs.NativeTools.configureCInteropWin32(project, "win32ssl")

description = "Portable Audio library for Kotlin"

korlibs.NativeTools.configureAllCInterop(project, "minimp3")
korlibs.NativeTools.configureAllCInterop(project, "stb_vorbis")

dependencies {
    add("commonMainApi", project(":korio"))
    add("commonMainApi", project(":korma"))
}
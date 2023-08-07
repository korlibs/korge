description = "Portable UI with accelerated graphics support for Kotlin"

korlibs.NativeTools.configureCInteropLinux(project, "X11Embed")

dependencies {
    add("commonMainApi", project(":korim"))
    add("commonTestApi", project(":ktruth"))
}
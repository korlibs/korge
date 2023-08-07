description = "Korim: Kotlin cORoutines IMaging utilities for JVM, JS, Native and Common"

//korlibs.NativeTools.configureCInteropLinux(project, "stb_image")
korlibs.NativeTools.configureAllCInterop(project, "stb_image")

dependencies {
	add("commonMainApi", project(":korio"))
	add("commonMainApi", project(":korma"))
	add("commonTestApi", project(":ktruth"))
    //add("commonTestApi", project(":korma-shape"))
}
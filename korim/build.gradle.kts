val enableKotlinNative: String by project
val doEnableKotlinNative get() = enableKotlinNative == "true"
if (doEnableKotlinNative) {
	kotlin {

		for (target in listOf(linuxX64())) {
			target.compilations["main"].cinterops { maybeCreate("stb_image") }
		}
	}
}

dependencies {
	add("commonMainApi", project(":korio"))
	add("commonMainApi", project(":korma"))
}
dependencies {
	add("commonMainApi", project(":korio"))
	add("commonMainApi", project(":korma"))
	//dependencyCInterops("stb_image", (if (korlibs.linuxEnabled) listOf("linuxX64") else listOf()) + listOf("iosX64", "iosArm32", "iosArm64"))
}
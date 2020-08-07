dependencies {
	add("commonMainApi", project(":korgw"))
    add("commonMainApi", project(":korui"))
	add("commonMainApi", project(":korau"))
	add("commonMainApi", project(":klogger"))
	add("commonMainApi", project(":korinject"))
    commonMainApi(project(":korte"))
	add("jvmMainApi", project(":krypto"))
    //add("jvmMainApi", project(":korte"))
}

/*
import com.soywiz.korlibs.*

apply<KorlibsPlugin>()
//apply(plugin = "org.jetbrains.dokka")

korlibs {
	exposeVersion()

	//dependencyCInteropsExternal("com.soywiz.korlibs.korau:korau:$korauVersion", "minimp3", ["mingwX64", "macosX64", "linuxX64", "iosX64", "iosArm32", "iosArm64"])
	//dependencyCInteropsExternal("com.soywiz.korlibs.korau:korau:$korauVersion", "stb_vorbis", ["mingwX64", "macosX64", "linuxX64", "iosX64", "iosArm32", "iosArm64"])
	//dependencyCInteropsExternal("com.soywiz.korlibs.korau:korau:$korauVersion", "win32_winmm", ["mingwX64"])
	//dependencyCInteropsExternal("com.soywiz.korlibs.korau:korau:$korauVersion", "linux_OpenAL", ["linuxX64"])
	//dependencyCInteropsExternal("com.soywiz.korlibs.korau:korau:$korauVersion", "mac_OpenAL", ["macosX64"])
	//dependencyCInteropsExternal("com.soywiz.korlibs.korau:kgl:$kglVersion", "GL", ["linuxX64"])
}

*/

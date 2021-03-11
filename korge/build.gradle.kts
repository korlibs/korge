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

val klockVersion: String by project
val korauVersion: String by project
val korimVersion: String by project
val kormaVersion: String by project
val korteVersion: String by project
val korgwVersion: String by project
val kryptoVersion: String by project
val korinjectVersion: String by project
val kloggerVersion: String by project
val kdsVersion: String by project
val kmemVersion: String by project

dependencies {
    add("commonMainApi", "com.soywiz.korlibs.klock:klock:$klockVersion")
	add("commonMainApi", "com.soywiz.korlibs.korau:korau:$korauVersion")
    add("commonMainApi", "com.soywiz.korlibs.korma:korma:$kormaVersion")
    add("commonMainApi", "com.soywiz.korlibs.korim:korim:$korimVersion")
	add("commonMainApi", "com.soywiz.korlibs.korgw:korgw:$korgwVersion")
	add("commonMainApi", "com.soywiz.korlibs.krypto:krypto:$kryptoVersion")
	add("commonMainApi", "com.soywiz.korlibs.korinject:korinject:$korinjectVersion")
	add("commonMainApi", "com.soywiz.korlibs.klogger:klogger:$kloggerVersion")
	add("commonMainApi", "com.soywiz.korlibs.kds:kds:$kdsVersion")
    add("commonMainApi", "com.soywiz.korlibs.kmem:kmem:$kmemVersion")
    add("commonMainApi", "com.soywiz.korlibs.korte:korte:$korteVersion")
    //add("commonTestApi", "it.krzeminski.vis-assert:vis-assert:0.4.0-beta")
}

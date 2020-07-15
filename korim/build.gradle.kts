import com.soywiz.korlibs.*

apply<KorlibsPlugin>()

korlibs {
    exposeVersion()
    dependencyCInterops("stb_image", (if (korlibs.linuxEnabled) listOf("linuxX64") else listOf()) + listOf("iosX64", "iosArm32", "iosArm64"))
}

val korioVersion: String by project
val kormaVersion: String by project

dependencies {
    add("commonMainApi", "com.soywiz.korlibs.korio:korio:$korioVersion")
    add("commonMainApi", "com.soywiz.korlibs.korma:korma:$kormaVersion")
}

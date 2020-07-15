import com.soywiz.korlibs.korlibs

apply<com.soywiz.korlibs.KorlibsPlugin>()

val klockVersion: String by project
val kdsVersion: String by project
val kmemVersion: String by project
val coroutinesVersion: String by project

dependencies {
	add("commonMainApi", "com.soywiz.korlibs.klock:klock:$klockVersion")
    add("commonMainApi", "com.soywiz.korlibs.kds:kds:$kdsVersion")
    add("commonMainApi", "com.soywiz.korlibs.kmem:kmem:$kmemVersion")

    add("commonMainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    if (korlibs.hasAndroid) {
        add("androidMainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutinesVersion")
    }
	add("commonMainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core-common:$coroutinesVersion")
	add("jsMainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$coroutinesVersion")
	add("jvmMainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    add("iosArm32MainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core-iosarm32:$coroutinesVersion")
	add("iosArm64MainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core-iosarm64:$coroutinesVersion")
	add("iosX64MainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core-iosx64:$coroutinesVersion")

    if (korlibs.watchosEnabled) {
        add("watchosArm32MainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core-watchosarm32:$coroutinesVersion")
        add("watchosArm64MainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core-watchosarm64:$coroutinesVersion")
        add("watchosX86MainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core-watchosx86:$coroutinesVersion")
    }

    if (korlibs.tvosEnabled) {
        add("tvosArm64MainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core-tvosarm64:$coroutinesVersion")
        add("tvosX64MainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core-tvosx64:$coroutinesVersion")
    }

    if (korlibs.linuxEnabled) {
        add("linuxX64MainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core-linuxx64:$coroutinesVersion")
    }
	add("mingwX64MainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core-mingwx64:$coroutinesVersion")
	add("macosX64MainApi", "org.jetbrains.kotlinx:kotlinx-coroutines-core-macosx64:$coroutinesVersion")
}

buildscript {
    val dokka_version: String by project

    repositories {
        mavenLocal()
        mavenCentral()
        jcenter()
        google()
        maven { url = uri("https://dl.bintray.com/korlibs/korlibs/") }
        maven { url = uri("https://plugins.gradle.org/m2/") }
        //maven { url = uri("https://dl.bintray.com/kotlin/kotlin-dev") }
    }

    dependencies {
        classpath("com.soywiz.korlibs:easy-kotlin-mpp-gradle-plugin:0.8.3") // Kotlin 1.3.71: https://github.com/korlibs/easy-kotlin-mpp-gradle-plugin
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:${dokka_version}")
    }
}

val buildExtraGradleFile = File(rootDir, "build.extra.gradle.kts")
if (buildExtraGradleFile.exists()) {
    apply(from = buildExtraGradleFile)
}

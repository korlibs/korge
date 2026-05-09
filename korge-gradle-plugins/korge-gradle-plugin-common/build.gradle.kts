plugins {
    id("java")
    id("java-gradle-plugin")
    alias(libs.plugins.kotlin.dsl)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.vanniktech.mavenPublish)
}

description = "Multiplatform Game Engine written in Kotlin"
group = "org.korge.gradle"

dependencies {
    implementation(libs.jgit)
    testImplementation(libs.bundles.kotlin.test)
}

java {
    setSourceCompatibility(libs.versions.javaSourceCompatibility.get())
    setTargetCompatibility(libs.versions.javaTargetCompatibility.get())
}

mavenPublishing {
    publishToMavenCentral()

    coordinates(group.toString(), "korge-gradle-plugin-common", version.toString())

    pom {
        name.set("korge-gradle-plugin-common")
        description.set("Multiplatform Game Engine written in Kotlin – Gradle Plugin Common")
        url.set("https://github.com/korlibs/korge")
        licenses {
            license {
                name.set("MIT")
                url.set("https://raw.githubusercontent.com/korlibs/korge/main/LICENSE")
            }
        }
        developers {
            developer {
                id.set("korge")
                name.set("KorGE Team")
                email.set("info@korge.org")
            }
        }
        scm {
            url.set("https://github.com/korlibs/korge")
        }
    }
}

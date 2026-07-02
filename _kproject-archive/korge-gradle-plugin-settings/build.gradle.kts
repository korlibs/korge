plugins {
    id("java")
    id("java-gradle-plugin")
    `kotlin-dsl`
    alias(libs.plugins.vanniktech.mavenPublish)
}

description = "Multiplatform Game Engine written in Kotlin"
group = "org.korge.gradleplugins"
version = libs.versions.korge.get()

java {
    setSourceCompatibility(libs.versions.javaSourceCompatibility.get())
    setTargetCompatibility(libs.versions.javaTargetCompatibility.get())
}

dependencies {
    testImplementation(libs.bundles.kotlin.test)
}

gradlePlugin {
    website.set("https://github.com/korlibs/kproject")
    vcsUrl.set("https://github.com/korlibs/kproject")
    //tags = ["kproject", "git"]

    plugins {
        register("korge-settings") {
            id = "org.korge.engine.settings"
            displayName = "KProject Settings Gradle Plugin"
            description = "Allows to use sourcecode & git-based dependencies"
            version = libs.versions.korge.get()
            // language=jvm-class-name
            implementationClass = "org.korge.kproject.KProjectSettingsPlugin"
        }
    }
}

// GradlePlugin type is auto-detected by vanniktech because java-gradle-plugin is applied
mavenPublishing {
    publishToMavenCentral()

    pom {
        name.set("korge-gradle-plugin-settings")
        description.set("Multiplatform Game Engine written in Kotlin – Settings Gradle Plugin")
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

plugins {
    id("java")
    id("java-gradle-plugin")
    `kotlin-dsl`
    alias(libs.plugins.vanniktech.mavenPublish)
    alias(libs.plugins.buildconfig)
}

description = "Multiplatform Game Engine written in Kotlin"
group = "org.korge.gradle"

gradlePlugin {
    website.set("https://korge.org/")
    vcsUrl.set("https://github.com/korlibs/korge")
    //tags = ["korge", "game", "engine", "game engine", "multiplatform", "kotlin"]

    plugins {
        val korge by registering {
            id = "org.korge.engine"
            displayName = "Korge Game Engine"
            description = "Multiplatform Game Engine for Kotlin"
            implementationClass = "korlibs.korge.gradle.KorgeGradlePlugin"
        }

        val `korge-library` by registering {
            id = "org.korge.engine.library"
            displayName = "Korge Library"
            description = "Multiplatform Game Engine for Kotlin"
            implementationClass = "korlibs.korge.gradle.KorgeLibraryGradlePlugin"
        }

        val kproject by registering {
            id = "org.korge.kproject"
            displayName = "KProject Gradle Plugin"
            description = "Allows to use sourcecode & git-based dependencies"
            implementationClass = "org.korge.kproject.KProjectPlugin"
        }

        val kprojectRoot by registering {
            id = "org.korge.kproject.root"
            displayName = "KProject Root Gradle Plugin"
            description = "Allows to use sourcecode & git-based dependencies"
            implementationClass = "org.korge.kproject.KProjectRootPlugin"
        }
    }
}

dependencies {
    implementation(project(":korge-gradle-plugin-common"))

    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.gradle.plugin.api)
    implementation(libs.kotlin.gradle.dsl)
    implementation(libs.kotlin.serialization)

    implementation(libs.proguard.gradle)
    implementation(libs.gson)
//    implementation(libs.gradle.publish.plugin)
    implementation(libs.vanniktech.maven.publish)

    implementation(libs.kotlinx.kover.gradle.plugin)
    implementation(libs.dokka)

    implementation(libs.android.build.gradle)

    implementation(gradleApi())
    implementation(localGroovy())

    testImplementation(libs.bundles.kotlin.test)
}

buildConfig {
    packageName("org.korge.gradle")
    className("BuildVersions")

    useKotlinOutput()
    buildConfigField("KORGE", libs.versions.korge)
    buildConfigField("KOTLIN", libs.versions.kotlin.asProvider())
    buildConfigField("KOTLIN_SERIALIZATION", libs.versions.kotlinx.serialization)
    buildConfigField("JNA", libs.versions.jna)
    buildConfigField("ANDROID_BUILD", libs.versions.android.gradle.plugin)
    buildConfigField("COROUTINES", libs.versions.kotlinx.coroutines)
}

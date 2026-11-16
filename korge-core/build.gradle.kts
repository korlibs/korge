import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.kotlinx.kover)
    alias(libs.plugins.dokka)
    alias(libs.plugins.vanniktech.mavenPublish)
}

description = "Multiplatform Game Engine written in Kotlin - Core module"
group = "org.korge.engine"

kotlin {
    applyDefaultHierarchyTemplate()
    // TODO Consider enabling ABI validation
//    @OptIn(ExperimentalAbiValidation::class)
//    abiValidation {
//        enabled.set(true)
//    }

    jvm()

    android {
        namespace = "org.korge.engine.core"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()

        androidResources.enable = true
        withHostTest {}
        withDeviceTest {}
    }
    js {
        browser {
            compilerOptions {
                target.set("es2015")
            }
        }
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            compilerOptions {
                target.set("es2015")
            }
        }
    }

    iosArm64()
    iosSimulatorArm64()
    iosX64()
    macosArm64()
    // TODO Add support for these targets as well
//    linuxX64()
//    linuxArm64()
//    mingwX64()
    // TODO Add android native targets as well

    sourceSets {
        commonMain.dependencies {
            api(libs.bundles.korlibs.all)
            api(libs.kotlinx.atomicfu)
            api(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            api(libs.kotlin.reflect)
            implementation(libs.jackson.databind)
            implementation(libs.jackson.module.kotlin)
        }

        val jvmAndAndroidMain by creating {
            dependsOn(commonMain.get())
        }

        jvmMain {
            dependsOn(jvmAndAndroidMain)
        }

        androidMain {
            dependsOn(jvmAndAndroidMain)
        }
    }
}

// Ensure the vanniktech maven-publish extension creates the Maven Central publish tasks
mavenPublishing {
    configure(KotlinMultiplatform(javadocJar = JavadocJar.Empty()))
    publishToMavenCentral()
    // Only sign when a key is available (e.g. not for publishToMavenLocal on CI)
    val hasSigningKey = project.providers.gradleProperty("signingInMemoryKey").isPresent ||
        System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey") != null
    if (hasSigningKey) {
        signAllPublications()
    }

    coordinates(group.toString(), "korge-core", version.toString())

    pom {
        name.set("korge-core")
        description.set("Multiplatform Game Engine written in Kotlin - Core module")
        inceptionYear.set("2020")
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
            connection.set("scm:git:https://github.com/korlibs/korge.git")
            developerConnection.set("scm:git:git@github.com:korlibs/korge.git")
            url.set("https://github.com/korlibs/korge")
        }
    }
}

package com.soywiz.korlibs.targets

import com.soywiz.korlibs.*
import org.gradle.api.*

fun Project.configureTargetAndroid() {
    if (korlibs.hasAndroid) {
        plugins.apply("com.android.library")
        extensions.getByType(com.android.build.gradle.LibraryExtension::class.java).apply {
            compileSdkVersion(28)
            defaultConfig {
                it.minSdkVersion(18)
                it.targetSdkVersion(28)
            }
        }

        gkotlin.apply {
            android {
                publishLibraryVariants("release", "debug")
            }
        }

        dependencies {
            add("androidMainImplementation", "org.jetbrains.kotlin:kotlin-stdlib")
            add("androidTestImplementation", "org.jetbrains.kotlin:kotlin-test")
            add("androidTestImplementation", "org.jetbrains.kotlin:kotlin-test-junit")
        }
    }
}

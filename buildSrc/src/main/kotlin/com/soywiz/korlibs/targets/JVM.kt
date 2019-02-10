package com.soywiz.korlibs.targets

import com.soywiz.korlibs.*
import org.gradle.api.*
import org.gradle.api.tasks.testing.*

fun Project.configureTargetJVM() {
    gkotlin.apply {
        jvm()
    }

    dependencies.apply {
        add("jvmMainImplementation", "org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        add("jvmTestImplementation", "org.jetbrains.kotlin:kotlin-test")
        add("jvmTestImplementation", "org.jetbrains.kotlin:kotlin-test-junit")
    }

    // Headless testing on JVM (so we can use GWT)
    tasks {
        (getByName("jvmTest") as Test).apply {
            jvmArgs = (jvmArgs ?: arrayListOf()) + arrayListOf("-Djava.awt.headless=true")
        }
    }
}

package com.soywiz.korge.gradle

import org.gradle.api.Project
import java.net.URI

fun Project.configureRepositories() {
	repositories.apply {
		mavenLocal().content {
			excludeGroup("Kotlin/Native")
		}
        mavenCentral().content {
            excludeGroup("Kotlin/Native")
        }
        google().content {
            excludeGroup("Kotlin/Native")
        }
        if (kotlinVersionIsDev) {
            maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/temporary") }
            maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven") }
        }
        maven { url = uri("https://androidx.dev/storage/compose-compiler/repository/") }

        //println("kotlinVersion=$kotlinVersion")
	}
}


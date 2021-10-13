package com.soywiz.korge.gradle

import org.gradle.api.Project
import java.net.URI

fun Project.configureRepositories() {
	repositories.apply {
		mavenLocal().content {
			it.excludeGroup("Kotlin/Native")
		}
        mavenCentral().content {
            it.excludeGroup("Kotlin/Native")
        }
        google().content {
            it.excludeGroup("Kotlin/Native")
        }
        if (kotlinVersionIsDev) {
            maven { it.url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/temporary") }
            maven { it.url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-coroutines/maven") }
        }
        //println("kotlinVersion=$kotlinVersion")
	}
}


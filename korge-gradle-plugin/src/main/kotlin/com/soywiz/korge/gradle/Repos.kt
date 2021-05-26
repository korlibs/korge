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
	}
}


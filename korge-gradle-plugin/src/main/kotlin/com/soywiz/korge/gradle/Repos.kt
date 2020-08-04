package com.soywiz.korge.gradle

import org.gradle.api.Project
import java.net.URI

fun Project.configureRepositories() {
	repositories.apply {
		mavenLocal().content {
			//it.excludeGroup("Kotlin/Native")
		}
		maven {
			it.url = URI("https://dl.bintray.com/korlibs/korlibs")
			it.content {
				it.excludeGroup("Kotlin/Native")
			}
		}
        mavenCentral().content {
            it.excludeGroup("Kotlin/Native")
        }
		jcenter().content {
			it.excludeGroup("Kotlin/Native")
		}
        google().content {
        }
        maven {
            it.url = URI("https://dl.bintray.com/kotlin/kotlin-eap")
            it.content {
                it.excludeGroup("Kotlin/Native")
            }
        }
        maven {
            it.url = URI("https://dl.bintray.com/kotlin/kotlin-dev")
            it.content {
                it.excludeGroup("Kotlin/Native")
            }
        }
	}
}


package com.soywiz.korlibs.modules

import org.gradle.api.*

fun Project.configureKorlibsRepos() {
    allprojects {
        repositories.apply {
            mavenLocal().apply {
                content {
                    excludeGroup("Kotlin/Native")
                }
            }
            mavenCentral() {
                content {
                    excludeGroup("Kotlin/Native")
                }
            }
            google().apply {
                content {
                    excludeGroup("Kotlin/Native")
                }
            }
        }
    }
}

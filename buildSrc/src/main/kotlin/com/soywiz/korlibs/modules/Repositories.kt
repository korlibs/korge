package com.soywiz.korlibs.modules

import org.gradle.api.*

fun Project.configureKorlibsRepos() {
    allprojects {
        repositories.apply {
            mavenLocal().apply {
                content {
                    it.excludeGroup("Kotlin/Native")
                }
            }
            maven {
                it.url = uri("https://dl.bintray.com/soywiz/soywiz")
                it.content {
                    it.includeGroup("com.soywiz")
                    it.excludeGroup("Kotlin/Native")
                }
            }
            jcenter() {
                it.content {
                    it.excludeGroup("Kotlin/Native")
                }
            }
            google().apply {
                content {
                    it.excludeGroup("Kotlin/Native")
                }
            }
        }
    }
}

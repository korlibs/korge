package com.soywiz.korlibs.root

import com.soywiz.korge.gradle.util.*
import org.gradle.api.*
import org.gradle.api.tasks.testing.*
import org.gradle.kotlin.dsl.*

fun Project.configureKover() {
    rootProject.allprojects {
        apply<kotlinx.kover.KoverPlugin>()
    }

    rootProject.koverMerged {
        enable()
    }

    // https://repo.maven.apache.org/maven2/org/jetbrains/intellij/deps/intellij-coverage-agent/1.0.688/
    //val koverVersion = "1.0.688"
    val koverVersion = rootProject._libs["versions"]["kover"]["agent"].dynamicInvoke("get").casted<String>()

    rootProject.allprojects {
        kover {
            engine.set(kotlinx.kover.api.IntellijEngine(koverVersion))
        }
        extensions.getByType(kotlinx.kover.api.KoverProjectConfig::class.java).apply {
            engine.set(kotlinx.kover.api.IntellijEngine(koverVersion))
        }
        tasks.withType<Test> {
            extensions.configure(kotlinx.kover.api.KoverTaskExtension::class) {
                //generateXml = false
                //generateHtml = true
                //coverageEngine = kotlinx.kover.api.CoverageEngine.INTELLIJ
                excludes.add(".*BuildConfig")
            }
        }
    }
}

package com.soywiz.korlibs.modules

import com.soywiz.korlibs.*
import org.gradle.api.*

fun Project.configureTests() {
    tasks.withType(org.gradle.api.tasks.testing.AbstractTestTask::class.java).allThis {
        testLogging {
            //setEvents(setOf("passed", "skipped", "failed", "standardOut", "standardError"))
            it.setEvents(setOf("skipped", "failed", "standardError"))
            it.showStandardStreams = true
            it.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}

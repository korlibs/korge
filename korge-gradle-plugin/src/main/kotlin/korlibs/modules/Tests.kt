package korlibs.modules

import korlibs.*
import org.gradle.api.*
import org.gradle.api.tasks.testing.logging.*

fun Project.configureTests() {
    tasks.withType(org.gradle.api.tasks.testing.AbstractTestTask::class.java).allThis {
        testLogging {
            //setEvents(setOf("passed", "skipped", "failed", "standardOut", "standardError"))
            it.events = mutableSetOf(
                //TestLogEvent.STARTED, TestLogEvent.PASSED,
                TestLogEvent.SKIPPED,
                TestLogEvent.FAILED,
                TestLogEvent.STANDARD_OUT, TestLogEvent.STANDARD_ERROR
            )
            it.exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            it.showStandardStreams = true
            it.showStackTraces = true
        }
    }
}

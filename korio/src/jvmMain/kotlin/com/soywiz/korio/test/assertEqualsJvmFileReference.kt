package com.soywiz.korio.test

import com.soywiz.korio.dynamic.*
import java.io.*

/**
 * Checks that a string matches a file in `src/jvmTest/resources`.
 * When environment variable UPDATE_TEST_REF=true, instead of checking,
 * this functions updates the file with the expected content.
 *
 * This is similar to jest's snapshot testing: <https://jestjs.io/docs/snapshot-testing>
 *
 * ---
 *
 * To simplify its usage, it is recommended to define a `jvmTestFix` gradle task as follows:
 *
 * Use ./gradlew jvmTestFix to update these files
 *
 * jvmTestFix gradle task should add environment("UPDATE_TEST_REF", "true")
 *
 * ```kotlin
 * val jvmTestFix = tasks.create("jvmTestFix", Test::class) {
 *     group = "verification"
 *     environment("UPDATE_TEST_REF", "true")
 *     testClassesDirs = jvmTest.testClassesDirs
 *     classpath = jvmTest.classpath
 *     bootstrapClasspath = jvmTest.bootstrapClasspath
 *     systemProperty("java.awt.headless", "true")
 * }
 * ```
 */
fun assertEqualsJvmFileReference(path: String, content: String) {
    val file = File("src/jvmTest/resources/$path").absoluteFile
    if (System.getenv("UPDATE_TEST_REF") == "true") {
        file.parentFile.mkdirs()
        file.writeText(content)
    }

    val message: String? = null
    val expected: String = file.takeIf { it.exists() }?.readText() ?: ""
    val actual: String = content

    val expectedLines = expected.lines()
    val actualLines = actual.lines()

    val asserter = Dyn.global["kotlin.test.AssertionsKt"]
        .dynamicInvokeOrThrow("getAsserter")

    val MAX_LINES = 300

    if ((actualLines.size < MAX_LINES && expectedLines.size < MAX_LINES) || expected == actual) {
        asserter.dynamicInvokeOrThrow("assertEquals", message, expected, actual)
    } else {
        for (n in 0 until kotlin.math.max(actualLines.size, expectedLines.size)) {
            val expectedLine = "LINE[$n]: ${expectedLines[n]}"
            val actualLine = "LINE[$n]: ${actualLines[n]}"
            asserter.dynamicInvokeOrThrow("assertEquals", message, expectedLine, actualLine)
        }
    }
}

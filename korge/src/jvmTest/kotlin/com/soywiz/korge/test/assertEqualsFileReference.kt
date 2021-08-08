package com.soywiz.korge.test

import java.io.*
import kotlin.test.*

// Use ./gradlew jvmTestFix to update these files
fun assertEqualsFileReference(path: String, content: String) {
    val file = File("src/jvmTest/resources/$path").absoluteFile
    if (System.getenv("UPDATE_TEST_REF") == "true") {
        file.parentFile.mkdirs()
        file.writeText(content)
    }
    //if (!file.exists()) assertTrue(false, "File '$file' doesn't exists")
    assertEquals(file.takeIf { it.exists() }?.readText(), content)
}

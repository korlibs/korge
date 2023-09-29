package com.soywiz.kproject.model

import java.io.*
import kotlin.test.*

class FileRefTest {
    val repo = GitRepository("https://github.com/korlibs/kproject.git")

    @Test
    fun testGitFileRef() {
        assertEquals(
            """
                rootProject.name = "kproject"
            """.trimIndent(),
            GitFileRef(repo, "v0.0.5", "/settings.gradle").readText().trim()
        )
        assertEquals(
            """
                rootProject.name = "kproject"

                include(":kproject-common")
                include(":kproject-project")
                include(":kproject-settings")
            """.trimIndent(),
            GitFileRef(repo, "v0.1.3", "/settings.gradle").readText().trim()
        )
        assertEquals(
            """
                [versions]
                kotlin = "1.8.21"
                gson = "2.8.6"

                [libraries]
                gson = { module = "com.google.code.gson:gson", version.ref = "gson" }
            """.trimIndent(),
            GitFileRef(repo, "v0.1.3", "/kproject-settings/build.gradle").parent().get("../gradle/libs.versions.toml").readText().trim()
        )
    }

    @Test
    fun test() {
        val mem = MemoryFileRef()
        assertEquals(
            mem["/demo2/kproject.yml"],
            mem["/demo3.kproject.yml"].parent()["demo2/kproject.yml"]
        )
        assertEquals(
            "/demo",
            mem["/demo3.kproject.yml"][".."]["../demo"].path.fullPath
        )
    }

    @Test
    fun testLocalFileTest() {
        assertEquals(LocalFileRef(File("aa/demo.txt")), LocalFileRef(File("aa/hello.txt")).parent()["demo.txt"])
        assertEquals(LocalFileRef(File("/aa/demo.txt")), LocalFileRef(File("/aa/hello.txt")).parent()["demo.txt"])
    }
}

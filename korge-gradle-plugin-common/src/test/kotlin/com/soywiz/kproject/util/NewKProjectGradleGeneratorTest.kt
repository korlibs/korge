package com.soywiz.kproject.util

import com.soywiz.kproject.model.*
import com.soywiz.kproject.version.*
import kotlin.test.*

class NewKProjectGradleGeneratorTest {
    @Test
    fun test() {
        val files = MemoryFiles().root

        files["/deps.kproject.yml"] = """
            plugins:
            - serialization
            dependencies:
            - ./mymodule
        """.trimIndent()

        files["/mymodule/kproject.yml"] = """
            src: "https://github.com/korlibs/kproject.git/samples/demo1/content#08f93c2e49b65d1d8258e4e1408580772558b038"
            dependencies:
            - "https://github.com/korlibs/kproject.git/samples/demo2#95696dd942ebc8db4ee9d9f4835ce12d853ff16f"
            testDependencies:
            - maven::jvm::org.mockito:mockito-core:5.3.1
            - io.mockk:mockk-android:1.13.5::jvm
        """.trimIndent()

        NewKProjectGradleGenerator(files)
            .generate("/deps.kproject.yml")

        val out = arrayListOf<String>()
        for ((fileName, _) in files.files.map) {
            val content = files[fileName].readText()
            out += "## $fileName"
            out += content
            out += ""
        }

        assertEquals(
            NewKProjectGradleGenerator::class.java.getResource("/NewKProjectGradleGeneratorTest_test.txt")?.readText()?.trim(),
            out.joinToString("\n").trim()
                .replace(KProjectVersion.VERSION, "0.0.1-SNAPSHOT")
                .trim()
        )
    }
}

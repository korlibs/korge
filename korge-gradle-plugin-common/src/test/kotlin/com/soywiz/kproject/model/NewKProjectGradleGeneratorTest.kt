package com.soywiz.kproject.model

import kotlin.test.*

class NewKProjectGradleGeneratorTest {
    @Test
    fun testEdgeCases() {
        val mem = MemoryFiles(linkedMapOf(
            "deps.kproject.yml" to byteArrayOf()
        ))

        val generator = NewKProjectGradleGenerator(mem.root)
        generator.generate("deps.kproject.yml")
    }
}

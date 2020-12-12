package com.soywiz.korio.file.std

import com.soywiz.korio.async.*
import kotlin.test.*

class DynamicRootVfsTest {
    @Test
    fun test() = suspendTest {
        val memoryVfs = MemoryVfsMix("path0/demo" to "world")
        var base = "/path0"
        val root = DynamicRootVfs(memoryVfs.vfs) { base }
        assertEquals("/path0/demo", root["demo"].absolutePath)
        assertEquals("world", root["demo"].readString())
        base = "/path1"
        assertEquals("/path1/test", root["test"].absolutePath)
    }
}

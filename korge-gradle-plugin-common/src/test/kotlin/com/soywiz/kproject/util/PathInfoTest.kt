package com.soywiz.kproject.util

import kotlin.test.*

class PathInfoTest {
    @Test
    fun testFullPath() {
        assertEquals("/", PathInfo("/").fullPath)
        assertEquals("/", PathInfo("//").fullPath)
        assertEquals("/hello", PathInfo("/hello").fullPath)
        assertEquals("/hello/world", PathInfo("/hello/world").fullPath)
        assertEquals("/world", PathInfo("/hello/../world").fullPath)
        assertEquals("/world", PathInfo("/////hello///..///world/").fullPath)
        assertEquals("/", PathInfo("/../../../../").fullPath)
        assertEquals("", PathInfo("").fullPath)
        assertEquals("", PathInfo("../../../../").fullPath)
    }

    @Test
    fun testFullPathExt() {
        assertEquals("/a", PathInfo("/../../a/../../a").fullPath)
    }

    @Test
    fun testAccess() {
        assertEquals("/a", PathInfo("/").access("a").fullPath)
        assertEquals("/a", PathInfo("/").access("../a/../a").fullPath)
        assertEquals("/hello/kproject", PathInfo("/hello/world").access("../kproject").fullPath)
    }
}

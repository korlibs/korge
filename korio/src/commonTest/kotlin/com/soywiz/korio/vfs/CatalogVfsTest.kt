package com.soywiz.korio.vfs

import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import kotlinx.coroutines.flow.*
import kotlin.test.*

class CatalogVfsTest {
    @Test
    fun test() = suspendTest {
        val vfs = MemoryVfsMix(
            "/\$catalog.json" to """[
                {"name": "demo", "size": 96, "modifiedTime": 0, "createTime": 0, "isDirectory": true},
                {"name": "korge.png", "size": 14015, "modifiedTime": 1, "createTime": 1, "isDirectory": false},
                {"name": "test.txt", "size": 11, "modifiedTime": 2, "createTime": 2, "isDirectory": false},
            ]""",
            "/demo/\$catalog.json" to """[
                {"name": "test.txt", "size": 12, "modifiedTime": 2, "createTime": 2, "isDirectory": false},
            ]""",
        ).withCatalog()

        assertEquals(
            "/demo,/demo/test.txt,/korge.png,/test.txt",
            vfs.listRecursive().toList().joinToString(",") { it.fullPathNormalized }
        )

        assertEquals(0L, vfs["/"].size())
        assertEquals(96L, vfs["/demo"].size())
        assertEquals(14015L, vfs["/korge.png"].size())
        assertEquals(11L, vfs["/test.txt"].size())
        assertEquals(12L, vfs["/demo/test.txt"].size())
    }
}

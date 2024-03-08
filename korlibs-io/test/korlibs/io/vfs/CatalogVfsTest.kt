package korlibs.io.vfs

import korlibs.io.async.suspendTest
import korlibs.io.file.*
import korlibs.io.file.std.MemoryVfsMix
import korlibs.io.file.std.withCatalog
import kotlinx.coroutines.flow.toList
import kotlin.test.Test
import kotlin.test.assertEquals

class CatalogVfsTest {
    @Test
    fun testOldCatalog() = suspendTest {
        testCatalog(MemoryVfsMix(
            "/\$catalog.json" to """[
                {"name": "demo", "size": 96, "modifiedTime": 0, "createTime": 0, "isDirectory": true},
                {"name": "korge.png", "size": 14015, "modifiedTime": 1, "createTime": 1, "isDirectory": false},
                {"name": "test.txt", "size": 11, "modifiedTime": 2, "createTime": 2, "isDirectory": false},
            ]""",
            "/demo/\$catalog.json" to """[
                {"name": "test.txt", "size": 12, "modifiedTime": 2, "createTime": 2, "isDirectory": false},
            ]""",
        ).withCatalog())
    }

    @Test
    fun testNewCompactCatalog() = suspendTest {
        testCatalog(MemoryVfsMix(
            "/\$catalog.json" to """{
                "demo/": [96, 0],
                "korge.png": [14015, 1],
                "test.txt": [11, 2],
            }""",
            "/demo/\$catalog.json" to """{
                "test.txt": [12, 2],
            }""",
        ).withCatalog())
    }


    private fun testCatalog(vfs: VfsFile) = suspendTest {
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

    // When catalog not found, it should fallback to a normal file stat check
    @Test
    fun testFallbackStat() = suspendTest {
        val vfs = MemoryVfsMix(
            "/hello.txt" to "world",
        ).withCatalog()

        val stat = vfs["hello.txt"].stat()
        assertEquals("5/true", "${stat.size}/${stat.exists}")
    }
}

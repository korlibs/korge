package korlibs.io.compression.zip

import korlibs.io.async.*
import korlibs.io.file.*
import korlibs.io.file.std.*
import kotlin.test.*

class ZipBuilderTest {
    suspend fun VfsFile.dumpZip() = openAsZip().listRecursiveSimple().joinToString(", ") { it.path }

    @Test
    fun testZipBuilder() = suspendTest {
        val root = MemoryVfsMix(
            "a/b/c/hello.txt" to "world"
        )

        assertEquals(
            "/a, /a/b, /a/b/c, /a/b/c/hello.txt",
            root["a"].createZipFromTreeTo(SingleFileMemoryVfs(""), useFolderAsRoot = false).dumpZip()
        )
        assertEquals(
            "/b, /b/c, /b/c/hello.txt",
            root["a"].createZipFromTreeTo(SingleFileMemoryVfs(""), useFolderAsRoot = true).dumpZip()
        )
    }
}

package korlibs.io.file.std

import korlibs.io.async.*
import korlibs.io.file.*
import korlibs.platform.*
import kotlin.test.*

class CaseSensitiveTest {
    private val cond: () -> Boolean = { !Platform.isJsOrWasm }

    @Test fun testResourcesVfs() = suspendTest(cond = cond, preferSyncIo = true) { _testResourcesVfs() }
    @Test fun testResourcesVfsAsync() = suspendTest(cond = cond, preferSyncIo = false) { _testResourcesVfs() }
    @Test fun testLocalVfs() = suspendTest(cond = cond, preferSyncIo = true) { _testLocalVfs() }
    @Test fun testLocalVfsAsync() = suspendTest(cond = cond, preferSyncIo = false) { _testLocalVfs() }
    @Test fun testLocalVfsFolder() = suspendTest(cond = cond, preferSyncIo = true) { _testLocalVfsFolder() }
    @Test fun testLocalVfsFolderAsync() = suspendTest(cond = cond, preferSyncIo = false) { _testLocalVfsFolder() }

    private suspend fun _testResourcesVfs() {
        assertEquals(false, resourcesVfs["file-not-exists.file.bin"].exists())
        assertEquals(false, resourcesVfs["resource.TXT"].exists())
        assertEquals(true, resourcesVfs["resource.txt"].exists())
        assertEquals(5, resourcesVfs["resource.txt"].readBytes().size)
        assertFails { resourcesVfs["resource.Txt"].readBytes().size }
    }

    private suspend fun _testLocalVfs() {
        val vfs = localVfs(StandardPaths.temp)
        val file = vfs["korio-resource.Txt"]
        file.writeString("HELLO")
        try {
            assertEquals(false, vfs["korio-resource.txt"].exists())
            assertEquals(false, vfs["korio-resource.TXT"].exists())
            assertEquals(true, vfs["korio-resource.Txt"].exists())

            assertEquals(false, vfs["korio-resource.txt"].isFile())
            assertEquals(true, vfs["korio-resource.Txt"].isFile())
            assertEquals(false, vfs["korio-resource.txt"].isDirectory())
            assertEquals(false, vfs["korio-resource.Txt"].isDirectory())

            assertFails { vfs["korio-resource.txt"].readBytes() }
            assertFails { vfs["korio-resource.TXT"].readBytes() }
            assertEquals(5, vfs["korio-resource.Txt"].readBytes().size)
        } finally {
            file.delete()
        }
    }

    private suspend fun _testLocalVfsFolder() {
        val vfs = localVfs(StandardPaths.temp)
        val dir = vfs["korio-resource-temp-Folder"]
        dir.mkdirs()
        dir["demo.txt"].writeString("HELLO")
        try {
            assertEquals(false, vfs["korio-resource-temp-folder"].exists())
            assertEquals(true, vfs["korio-resource-temp-Folder"].exists())

            assertEquals(false, vfs["korio-resource-temp-folder"].isFile())
            assertEquals(false, vfs["korio-resource-temp-Folder"].isFile())
            assertEquals(false, vfs["korio-resource-temp-folder"].isDirectory())
            assertEquals(true, vfs["korio-resource-temp-Folder"].isDirectory())

            assertEquals(listOf("demo.txt"), dir.listSimple().filter { it.baseName == "demo.txt" }.map { it.baseName })
            assertFails { vfs["korio-resource-temp-folder"].listSimple() }
        } finally {
            dir.deleteRecursively(includeSelf = true)
        }
    }
}

package korlibs.io.file.std

import korlibs.io.async.*
import korlibs.io.file.*
import korlibs.io.stream.*
import kotlin.test.*

class LocalVfsJvmAndroidTest {
    @Test
    fun testAppend() = suspendTest {
        _testAppendVfs(rootLocalVfs.vfs, "default")
    }

    @Test
    fun testAppendAsynchronousFileChannelVfs() = suspendTest {
        _testAppendVfs(AsynchronousFileChannelVfs(), "async")
    }

    @Test
    fun testAppendBaseLocalVfsJvm() = suspendTest {
        _testAppendVfs(BaseLocalVfsJvm(), "sync")
    }

    suspend fun _testAppendVfs(vfs: Vfs, name: String) {
        val file = vfs[StandardPaths.temp]["file.append.$name.txt"]
        file.delete()
        try {
            file.openUse(VfsOpenMode.APPEND) {
                writeString("hello")
            }
            file.openUse(VfsOpenMode.APPEND) {
                writeString(" world")
            }
            assertEquals("hello world", file.readString())
        } finally {
            file.delete()
        }
    }
}

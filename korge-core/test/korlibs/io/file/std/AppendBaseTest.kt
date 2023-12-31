package korlibs.io.file.std

import korlibs.io.file.*
import korlibs.io.stream.*
import kotlin.test.*

class AppendBaseTest {
    companion object {
        suspend fun _testAppendVfs(file: VfsFile) {
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
}

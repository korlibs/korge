package korlibs.io.compression.zip

import korlibs.io.async.*
import korlibs.io.file.std.*
import kotlin.test.*

class ZipFileTest {
    @Test
    fun test() = suspendTest {
        resourcesVfs["krita1.kra"].open().useIt { stream ->
            val zip = ZipFile(stream)
            //println(zip.files)
            val vfs = stream.openAsZip()
            assertEquals(65859L, vfs["mergedimage.png"].size())
        }
    }
}

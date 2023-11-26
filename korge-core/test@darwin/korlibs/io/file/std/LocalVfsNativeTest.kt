package korlibs.io.file.std

import korlibs.io.lang.*
import kotlin.test.*

class LocalVfsNativeTest {

    @Test
    fun testUserHomeVfsValue() {
        val expectedResult = when {
            Environment["HOMEDRIVE"] != null && Environment["HOMEPATH"] != null -> "${Environment["HOMEDRIVE"]}${Environment["HOMEPATH"]}"
            else -> Environment["HOMEPATH"] ?: Environment["HOME"] ?: Environment["TEMP"] ?: Environment["TMP"] ?: "/tmp"
        }
        assertEquals(
            expectedResult.replace("\\", "/").trimEnd('/'),
            userHomeVfs.absolutePath.replace("\\", "/").trimEnd('/')
        )
    }

}

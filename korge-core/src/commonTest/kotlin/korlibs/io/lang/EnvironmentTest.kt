package korlibs.io.lang

import korlibs.memory.*
import korlibs.io.util.*
import korlibs.platform.*
import kotlin.test.*

class EnvironmentTest {
	//@Test
	//fun testCaseInsensitive() {
    //    if (OS.isJsBrowserOrWorker) return
    //    val path1 = Environment["pAth"]
    //    val path2 = Environment["PATH"]
    //    if (OS.isWindows) {
    //        assertEquals(path1, path2)
    //        assertNotNull(path2)
    //    } else {
    //        assertNull(path1)
    //        assertNotNull(path2)
    //    }
	//}

    @Test
    fun testGetAllWorks() {
        val envs = Environment.getAll().map { it.key } // Test that at least it doesn't crash
        if (!Platform.isJsBrowserOrWorker) {
            assertTrue { envs.size >= 4 }
        }
    }

    @Test
    fun testExpand() {
        // Windows
        Environment(
            "HOMEDrive" to "C:",
            "Homepath" to "\\Users\\soywiz",
        ).also { env ->
            assertEquals("C:\\Users\\soywiz/.game", env.expand("~/.game"))
        }

        // Linux
        Environment(
            "hOme" to "/home/soywiz",
        ).also { env ->
            assertEquals("/home/soywiz/.game", env.expand("~/.game"))
        }
    }
}

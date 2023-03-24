package korlibs.io.file.registry

import korlibs.io.async.*
import korlibs.io.file.*
import korlibs.io.stream.*
import korlibs.io.util.*
import korlibs.crypto.encoding.*
import kotlin.test.*

class WindowsRegistryMingwTest {
    @Test
    fun testRegistry() = suspendTest({ WindowsRegistry.isSupported }) {
        //assertEquals(WindowsRegistryBase.KEY_MAP.keys.toList().sorted(), WindowsRegistryVfs.root.listNames().sorted())
        //println(WindowsRegistry.listSubKeys("HKEY_LOCAL_MACHINE"))
        //println(WindowsRegistry.listValues("HKEY_CURRENT_USER/Software/7-Zip"))
    }

}
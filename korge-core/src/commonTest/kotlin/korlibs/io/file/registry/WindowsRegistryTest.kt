package korlibs.io.file.registry

import korlibs.encoding.*
import korlibs.io.async.*
import korlibs.io.file.*
import korlibs.io.stream.*
import korlibs.platform.*
import kotlin.test.*

class WindowsRegistryTest {
    // @TODO: Move to korlibs repo
    @Test
    fun testRegistry() = suspendTest({ WindowsRegistry.isSupported && !Platform.isJsDenoJs }) {
        assertEquals(WindowsRegistry.KEY_MAP.keys.toList().sorted(), WindowsRegistryVfs.root.listNames().sorted())

        //println(WindowsRegistryVfs.HKEY_LOCAL_MACHINE["Software"].listNames())

        assertTrue("software") { WindowsRegistryVfs.HKEY_CURRENT_USER.listNames().map { it.lowercase() }.contains("software") }
        assertTrue("microsoft") { WindowsRegistryVfs.HKEY_LOCAL_MACHINE["Software"].listNames().map { it.lowercase() }.contains("microsoft") }
        assertTrue("currentversion") { WindowsRegistryVfs.HKEY_LOCAL_MACHINE["Software/Microsoft/Windows"].listNames().map { it.lowercase() }.contains("currentversion") }

        val korge = WindowsRegistryVfs.HKEY_CURRENT_USER["Software/KorGETest"]
        suspend fun cleanup() {
            korge["MyKey"].delete()
            korge.delete()
        }

        cleanup()
        korge.mkdir()
        korge["MyKey"].mkdir()
        korge["MyStringValue"].writeString("hello")
        korge["MyBinaryValue"].writeBytes(byteArrayOf(1, 2, 3))
        korge["MyIntValue"].put("1024".openAsync(), Vfs.FileKind.INT)
        korge["MyLongValue"].put("1024".openAsync(), Vfs.FileKind.LONG)

        assertEquals(
            listOf("MyBinaryValue", "MyIntValue", "MyKey", "MyLongValue", "MyStringValue"),
            korge.listSimple().map { it.baseName }.sorted()
        )

        assertEquals("hello", korge["MyStringValue"].readString())
        assertEquals("010203", korge["MyBinaryValue"].readBytes().hex)
        assertEquals("1024", korge["MyIntValue"].readString())
        assertEquals("1024", korge["MyLongValue"].readString())

        cleanup()
    }
}

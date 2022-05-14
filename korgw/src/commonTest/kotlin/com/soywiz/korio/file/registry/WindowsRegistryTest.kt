package com.soywiz.korio.file.registry

import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.file.Vfs
import com.soywiz.korio.file.baseName
import com.soywiz.korio.stream.openAsync
import com.soywiz.krypto.encoding.hex
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WindowsRegistryTest {
    @Test
    @Ignore
    fun testRegistry() = suspendTest({ WindowsRegistry.isSupported }) {
        assertEquals(WindowsRegistryBase.KEY_MAP.keys.toList().sorted(), WindowsRegistryVfs.root.listNames().sorted())

        assertTrue { WindowsRegistryVfs["HKEY_CURRENT_USER"].listNames().map { it.lowercase() }.contains("software") }
        assertTrue { WindowsRegistryVfs["HKEY_LOCAL_MACHINE/Software"].listNames().map { it.lowercase() }.contains("windows") }
        assertTrue { WindowsRegistryVfs["HKEY_LOCAL_MACHINE/Software/Windows"].listNames().map { it.lowercase() }.contains("currentversion") }

        val korge = WindowsRegistryVfs["HKEY_CURRENT_USER/Software/KorGETest"]
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

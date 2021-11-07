package com.soywiz.korio.file.registry

import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*
import com.soywiz.krypto.encoding.*
import kotlin.test.*

class WindowsRegistryMingwTest {
    @Test
    fun testRegistry() = suspendTest({ WindowsRegistry.isSupported }) {
        //assertEquals(WindowsRegistryBase.KEY_MAP.keys.toList().sorted(), WindowsRegistryVfs.root.listNames().sorted())
        //println(WindowsRegistry.listSubKeys("HKEY_LOCAL_MACHINE"))
        //println(WindowsRegistry.listValues("HKEY_CURRENT_USER/Software/7-Zip"))
    }

}

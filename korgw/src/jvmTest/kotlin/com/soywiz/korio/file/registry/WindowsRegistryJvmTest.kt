package com.soywiz.korio.file.registry

import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*
import com.soywiz.krypto.encoding.*
import kotlin.test.*

class WindowsRegistryJvmTest {
    @Test
    fun testRegistry() = suspendTest({ WindowsRegistry.isSupported }) {
    }
}

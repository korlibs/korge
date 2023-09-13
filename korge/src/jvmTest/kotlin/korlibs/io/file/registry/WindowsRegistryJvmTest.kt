package korlibs.io.file.registry

import korlibs.io.async.*
import korlibs.io.file.*
import korlibs.io.stream.*
import korlibs.io.util.*
import korlibs.crypto.encoding.*
import kotlin.test.*

class WindowsRegistryJvmTest {
    @Test
    fun testRegistry() = suspendTest({ WindowsRegistry.isSupported }) {
    }
}

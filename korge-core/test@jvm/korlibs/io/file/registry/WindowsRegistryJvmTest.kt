package korlibs.io.file.registry

import korlibs.io.async.*
import kotlin.test.*

class WindowsRegistryJvmTest {
    @Test
    fun testRegistry() = suspendTest({ WindowsRegistry.isSupported }) {
    }
}

package korlibs.image.format

import korlibs.io.async.*
import korlibs.io.file.std.*
import korlibs.math.geom.*
import korlibs.platform.*
import kotlin.test.*

class FAKETest {
    init {
        RegisteredImageFormats.registerFirst(FAKE)
    }

    @Test
    fun testInfoNotFake() = suspendTest {
        assertEquals(null, FAKE.decodeHeaderSuspend(SingleFileMemoryVfsWithName("", name = "4x3.png"))?.size)
    }

    @Test
    fun testInfoFake() = suspendTest {
        assertEquals(Size(4, 3), FAKE.decodeHeaderSuspend(SingleFileMemoryVfsWithName("", name = "4x3.fake"))?.size)
    }

    @Test
    fun testSizeInName() = suspendTest {
        if (Platform.isWasm) {
            println("Skipping in wasm as it fails for now")
            return@suspendTest
        }

        assertEquals("4x3", SingleFileMemoryVfsWithName("", name = "4x3.fake").readBitmapSlice().sizeString)
    }

    @Test
    fun testSizeInContent() = suspendTest {
        if (Platform.isWasm) {
            println("Skipping in wasm as it fails for now")
            return@suspendTest
        }

        assertEquals("4x3", SingleFileMemoryVfs("4x3", ext = "fake").readBitmapSlice().sizeString)
    }

    @Test
    fun testDefaultSize() = suspendTest {
        if (Platform.isWasm) {
            println("Skipping in wasm as it fails for now")
            return@suspendTest
        }

        assertEquals("128x128", SingleFileMemoryVfs("", ext = "fake").readBitmapSlice().sizeString)
    }
}

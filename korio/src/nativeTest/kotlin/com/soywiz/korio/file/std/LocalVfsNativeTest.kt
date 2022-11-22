package com.soywiz.korio.file.std

import com.soywiz.korio.file.*
import com.soywiz.korio.lang.*
import kotlin.test.*

class LocalVfsNativeTest {

    @Test
    fun testUserHomeVfsValue() {
        val homePath = Environment["HOME"]
        val expectedResult = if (homePath != null) homePath else "/tmp"
        assertEquals(
            expectedResult.replace("/", ""),
            userHomeVfs.absolutePath.replace("/", "")
        )
    }

}

package com.soywiz.korio.vfs

import com.soywiz.klock.DateTime
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.Closeable
import com.soywiz.korio.util.OS
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.toList
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.seconds

class ResourcesVfsTest {
    @Test
    fun name() = suspendTest({ OS.isJvm }) {
        println("[A]")
        val listing = resourcesVfs["tresfolder"].list()
        println("[B]")

        for (v in resourcesVfs["tresfolder"].list().filter { it.extensionLC == "txt" }.toList()) {
            println(v)
        }

        assertEquals(
            "[a.txt, b.txt]",
            resourcesVfs["tresfolder"].list().filter { it.extensionLC == "txt" }.toList().map { it.baseName }.sorted()
                .toString()
        )
    }

    @Test
    fun watch() = suspendTest({ OS.isJvm }) {
        var log = String()
        println("watcher start")

        val closeable = resourcesVfs["tresfolder"].watch {
            log = it.toString()
            println(log)
        }

        resourcesVfs["tresfolder/a.txt"].touch(DateTime.now())
        kotlinx.coroutines.delay(100)
        closeable.close()

        println("watcher end")
        assertEquals("MODIFIED(JailVfs(ResourcesVfs[])[/tresfolder/a.txt])", log)
    }
}

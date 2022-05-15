package com.soywiz.korio.vfs

import com.soywiz.korio.async.suspendTest
import com.soywiz.korio.async.suspendTestNoBrowser
import com.soywiz.korio.file.baseName
import com.soywiz.korio.file.std.MemoryVfsMix
import com.soywiz.korio.file.std.MergedVfs
import com.soywiz.korio.file.std.Mountable
import com.soywiz.korio.file.std.MountableVfs
import com.soywiz.korio.file.std.MountableVfsSync
import com.soywiz.korio.file.std.mount
import com.soywiz.korio.file.std.openAsIso
import com.soywiz.korio.file.std.openAsZip
import com.soywiz.korio.file.std.resourcesVfs
import com.soywiz.korio.lang.FileNotFoundException
import com.soywiz.korio.util.expectException
import kotlin.test.Test
import kotlin.test.assertEquals

class MountableVfsTest {
	@Test
	fun testMountable() = suspendTestNoBrowser {
		val root = MountableVfs(closeMounts = true) {
			mount("/zip/demo2", resourcesVfs["hello.zip"].openAsZip())
			mount("/zip", resourcesVfs["hello.zip"].openAsZip())
			mount("/zip/demo", resourcesVfs["hello.zip"].openAsZip())
			mount("/iso", resourcesVfs["isotest.iso"].openAsIso())
		}
		try {
			assertEquals("HELLO WORLD!", root["/zip/hello/world.txt"].readString())
			assertEquals("HELLO WORLD!", root["/zip/demo/hello/world.txt"].readString())
			assertEquals("HELLO WORLD!", root["/zip/demo2/hello/world.txt"].readString())
			assertEquals("WORLD!", root["iso"]["hello/world.txt"].readString())

			(root.vfs as Mountable).unmount("/zip")

			expectException<FileNotFoundException> {
				root["/zip/hello/world.txt"].readString()
			}
		} finally {
			root.vfs.close()
		}
	}

    @Test
    fun testMountable2() = suspendTest {
        val root = MountableVfsSync {
            mount("/", MemoryVfsMix("f1" to "v1"))
            mount("/sdcard", MergedVfs(
                MemoryVfsMix("f2" to "v2"),
                MemoryVfsMix("f3" to "v3"),
                MemoryVfsMix("f4" to "v4")
            ))
        }
        assertEquals("v1", root["f1"].readString())
        assertEquals("v2", root["/sdcard/f2"].readString())
        assertEquals("v3", root["/sdcard/f3"].readString())
        assertEquals("v4", root["/sdcard/f4"].readString())

        assertEquals(listOf("f1"), root["/"].listSimple().map { it.baseName })
        assertEquals(listOf("f2", "f3", "f4"), root["/sdcard"].listSimple().map { it.baseName })
    }
}

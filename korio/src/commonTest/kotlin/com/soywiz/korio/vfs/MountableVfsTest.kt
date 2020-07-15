package com.soywiz.korio.vfs

import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import kotlin.test.*

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
}
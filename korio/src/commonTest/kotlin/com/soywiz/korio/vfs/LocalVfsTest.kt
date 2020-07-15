package com.soywiz.korio.vfs

import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.stream.*
import com.soywiz.korio.util.*
import kotlin.test.*

class LocalVfsTest {
	val temp by lazy { tempVfs }

	@Test
	fun name() = suspendTestNoBrowser {
		val content = "HELLO WORLD!"
		temp["korio.temp"].writeString(content)
		temp["korio.temp2"].writeFile(temp["korio.temp"])
		temp["korio.temp3"].writeFile(temp["korio.temp"])
		temp["korio.temp3"].writeStream(temp["korio.temp"].open().slice(0 until 3, closeParent = true))
		assertEquals(content, temp["korio.temp2"].readString())
		assertEquals("HEL", temp["korio.temp3"].readString())
		assertEquals(true, temp["korio.temp"].delete(), "deleting korio.temp")
		assertEquals(true, temp["korio.temp2"].delete(), "deleting korio.temp2")
		assertEquals(true, temp["korio.temp3"].delete(), "deleting korio.temp3")
		assertEquals(false, temp["korio.temp3"].delete(), "deleting korio.temp3")
		assertEquals(
			tempVfs["korio.temp3"].absolutePath.replace('\\', '/'),
			temp["korio.temp3"].absolutePath
		)
	}

	@Test
	fun execTest() = suspendTestNoBrowser {
		when {
			OS.isJsBrowserOrWorker -> Unit // Skip
			OS.isNative -> Unit // Skip
			else -> assertEquals("1", temp.execToString(listOf("echo", "1")).trim())
		}
	}

	@Test
	fun ensureParent() = suspendTestNoBrowser {
		temp["korio.temp.folder/test.txt"].ensureParents().writeString("HELLO")
		temp["korio.temp.folder/test.txt"].delete()
		temp["korio.temp.folder"].delete()
	}

	private val local by lazy { localCurrentDirVfs }
	private val existing1 by lazy { local["__existing"] }
	private val unexisting1 by lazy { local["__unexisting"] }

	@Test
	fun openModeRead() = suspendTestNoBrowser {
		when {
			OS.isJsBrowserOrWorker -> Unit // Ignore
			else -> {
				existing1.writeString("hello")
				val readBytes = existing1.openUse(VfsOpenMode.READ) { readAll() }
				assertEquals("hello", readBytes.toString(UTF8))
				expectException<FileNotFoundException> { unexisting1.open(VfsOpenMode.READ).close() }
				//unexisting1.open(VfsOpenMode.READ).close()
			}
		}
	}

}
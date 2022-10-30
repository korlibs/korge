package com.soywiz.korio.vfs

import com.soywiz.kmem.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.localCurrentDirVfs
import com.soywiz.korio.file.std.tempVfs
import com.soywiz.korio.lang.FileNotFoundException
import com.soywiz.korio.lang.UTF8
import com.soywiz.korio.lang.toString
import com.soywiz.korio.stream.readAll
import com.soywiz.korio.stream.slice
import com.soywiz.korio.util.OS
import com.soywiz.korio.util.expectException
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
	fun testExec() = suspendTestNoBrowser {
        if (OS.isAndroid) return@suspendTestNoBrowser
        if (OS.isIos) return@suspendTestNoBrowser
        //val str = ">hello< '1^&) \" $ \\ \n \r \t \$test (|&,; 2" // @TODO: Couldn't get line breaks working on windows
        //val str = ">hello< '1^&) \" $ \\ \$test %test% (|&,; 2" // @TODO: Fails on windows/nodejs
        val str = "hello world"
        //val str = "1"
		when {
			OS.isJsBrowserOrWorker -> Unit // Skip
			else -> assertEquals(str, temp.execToString(listOf("echo", str)).trim())
		}
	}

    @Test
    fun testExecNonExistant() = suspendTestNoBrowser {
        if (OS.isAndroid) return@suspendTestNoBrowser
        if (OS.isIos) return@suspendTestNoBrowser
        val message = assertFailsWith<FileNotFoundException> {
            localCurrentDirVfs["directory-that-does-not-exist"].execToString("echo", "1")
        }
        assertTrue { message.message!!.contains("is not a directory, to execute 'echo'") }
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
        if (OS.isAndroid) return@suspendTestNoBrowser
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

    @Test
    fun testUnixPermissions() = suspendTest({ (Platform.isJvm && Platform.isUnix) || Platform.isMac || Platform.isLinux || (Platform.isJsNodeJs) }) {
        val chmod = "0713".toInt(8)
        val file = tempVfs["korio-temp123.bin"]
        file.delete()
        try {
            file.writeString("123")
            println("testUnixPermissions[before]:attribute=${file.getAttribute<Vfs.UnixPermissionsAttribute>()}")
            file.setAttributes(Vfs.UnixPermissionsAttribute(chmod))
            println("testUnixPermissions[after]:attribute=${file.getAttribute<Vfs.UnixPermissionsAttribute>()}")
            assertEquals(chmod, file.getAttribute<Vfs.UnixPermissionsAttribute>()!!.rbits)
        } finally {
            file.delete()
        }
    }
}

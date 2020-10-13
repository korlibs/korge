package com.soywiz.korio.vfs

import com.soywiz.klock.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.serialization.xml.*
import com.soywiz.korio.stream.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlin.test.*

@Suppress("EXPERIMENTAL_FEATURE_WARNING")
class ZipVfsTest {
	@Test
	fun testZipUncompressed1() = suspendTestNoBrowser {
		resourcesVfs["hello.zip"].openAsZip { helloZip ->
			assertEquals(
				"[VfsStat(file=/hello, exists=true, isDirectory=true, size=0, device=-1, inode=0, mode=511, owner=nobody, group=nobody, createTime=Mon, 26 Dec 2016 00:00:10 UTC, modifiedTime=Thu, 01 Jan 1970 00:00:00 UTC, lastAccessTime=Thu, 01 Jan 1970 00:00:00 UTC, extraInfo=null, id=null)]",
				helloZip.list().toList().map { it.stat().toString(showFile = false) }.toString()
			)
		}
	}

	@Test
	fun testZipUncompressed2() = suspendTestNoBrowser {
		resourcesVfs["hello.zip"].openAsZip { helloZip ->
			assertEquals(
				"[VfsStat(file=/hello/world.txt, exists=true, isDirectory=false, size=12, device=-1, inode=1, mode=511, owner=nobody, group=nobody, createTime=Mon, 26 Dec 2016 00:00:10 UTC, modifiedTime=Thu, 01 Jan 1970 00:00:00 UTC, lastAccessTime=Thu, 01 Jan 1970 00:00:00 UTC, extraInfo=null, id=null)]",
				helloZip["hello"].list().toList().map { it.stat().toString(showFile = false) }.toString()
			)
		}
	}

	@Test
	fun testZipUncompressed3() = suspendTestNoBrowser {
		resourcesVfs["hello.zip"].openAsZip { helloZip ->
			assertEquals(
				"VfsStat(file=/hello/world.txt, exists=true, isDirectory=false, size=12, device=-1, inode=1, mode=511, owner=nobody, group=nobody, createTime=Mon, 26 Dec 2016 00:00:10 UTC, modifiedTime=Thu, 01 Jan 1970 00:00:00 UTC, lastAccessTime=Thu, 01 Jan 1970 00:00:00 UTC, extraInfo=null, id=null)",
				helloZip["hello/world.txt"].stat().toString(showFile = false)
			)
		}
	}

	@Test
	fun testZipUncompressed4() = suspendTestNoBrowser {
		resourcesVfs["hello.zip"].openAsZip { helloZip ->
			assertEquals(
				"HELLO WORLD!",
				helloZip["hello/world.txt"].readString()
			)
		}
	}

	@Test
	fun testZipUncompressed5() = suspendTestNoBrowser {
		resourcesVfs["hello.zip"].openAsZip { helloZip ->
			val stat = helloZip["hello/world.txt"].stat()
			val createTime = stat.createTime

			assertEquals(
				"2016-12-26 00:00:10",
				DateFormat("YYYY-MM-dd HH:mm:ss").format(createTime)
			)
		}
	}

	@Test
	fun testZipCompressed() = suspendTestNoBrowser {
		resourcesVfs["compressedHello.zip"].openAsZip { helloZip ->
			val contents =
				"HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO WORLD!"

			assertEquals(
				contents,
				helloZip["hello/compressedWorld.txt"].readString()
			)

			assertEquals(
				contents,
				helloZip["hello/compressedWorld.txt"].openUse { readAvailable() }.toString(UTF8)
			)

			assertEquals(
				contents.toByteArray().size.toLong(),
				helloZip["hello/compressedWorld.txt"].openUse { getLength() }
			)

			assertEquals(
				"[/hello, /hello/compressedWorld.txt, /hello/world.txt]",
				helloZip.listRecursive().map { it.fullName }.toList().toString()
			)

			println(helloZip.stat())
			assertEquals(true, helloZip.exists())
			assertEquals(true, helloZip.isDirectory())
			assertEquals(true, helloZip["/"].isDirectory())
			val mem = MemoryVfs()
			helloZip.copyToTree(mem)
			assertEquals(contents, mem["hello/compressedWorld.txt"].readString())
		}
	}

	@Test
	fun testCreateZip() = suspendTestNoBrowser {
		val mem = MemoryVfsMix(
			"/test.txt" to "test",
			"/hello/world.txt" to "hello world world world world!"
		)
		//println("[1]")
		val zipBytes = mem.createZipFromTree()
		//println("[2]")
		//zipBytes.writeToFile("c:/temp/mytest.zip")
		zipBytes.openAsync().openAsZip { zip ->
			//println("[3]")
			assertEquals(
				"test",
				zip["/test.txt"].readString()
			)
			assertEquals(
				"hello world world world world!",
				zip["/hello/world.txt"].readString()
			)
		}
	}

	//@Test
	//fun testZip1() = suspendTestNoBrowser {
	//	val mem = MemoryVfs()
	//	//UrlVfs("https://github.com/soywiz/korge-tools/releases/download/binaries/rhubarb-lip-sync-1.4.2-win32.zip").copyTo(LocalVfs["c:/temp/file.zip"])
	//	//val zip = LocalVfs("c:/temp/rhubarb-lip-sync-1.4.2-osx.zip").openAsZip()
	//	localVfs("c:/temp/rhubarb-lip-sync-1.4.2-win32.zip").openAsZip { zip ->
	//		//zip.copyTo(mem) // IOException
	//		zip.copyToTree(mem) // IOException
	//		//assertEquals(
	//		//	listOf("/rhubarb-lip-sync-1.4.2-osx"),
	//		//	zip.list().map { it.fullname }.toList()
	//		//)
	//		//val mem = MemoryVfs()
	//		//zip.copyToTree(mem)
	//	}
	//}

	@Test
	fun testReadChunk() = suspendTestNoBrowser {
		resourcesVfs["simple1.fla.zip"].openAsZip { zip ->
			val xml = zip["DOMDocument.xml"].readXml()
			assertEquals(1, xml.descendants.filter { it.nameLC == "frames" }.count())
		}
	}
}

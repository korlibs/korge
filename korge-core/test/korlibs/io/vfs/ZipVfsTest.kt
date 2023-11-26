package korlibs.io.vfs

import korlibs.time.DateFormat
import korlibs.time.format
import korlibs.io.async.suspendTestNoBrowser
import korlibs.io.file.fullName
import korlibs.io.file.std.MemoryVfs
import korlibs.io.file.std.MemoryVfsMix
import korlibs.io.file.std.createZipFromTree
import korlibs.io.file.std.openAsZip
import korlibs.io.file.std.resourcesVfs
import korlibs.io.lang.UTF8
import korlibs.io.lang.toByteArray
import korlibs.io.lang.toString
import korlibs.io.serialization.xml.readXml
import korlibs.io.stream.SeekNotSupportedException
import korlibs.io.stream.openAsync
import korlibs.io.stream.readAvailable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

@Suppress("EXPERIMENTAL_FEATURE_WARNING")
class ZipVfsTest {
	@Test
	fun testZipUncompressed1() = suspendTestNoBrowser {
		resourcesVfs["hello.zip"].openAsZip { helloZip ->
			assertEquals(
				"[VfsStat(file=/hello, exists=true, isDirectory=true, size=0, device=-1, inode=0, mode=511, owner=nobody, group=nobody, createTime=DateTime(1482710410000), modifiedTime=DateTime(0), lastAccessTime=DateTime(0), extraInfo=null, id=null)]",
				helloZip.list().toList().map { it.stat().toString(showFile = false) }.toString()
			)
		}
	}

	@Test
	fun testZipUncompressed2() = suspendTestNoBrowser {
		resourcesVfs["hello.zip"].openAsZip { helloZip ->
			assertEquals(
				"[VfsStat(file=/hello/world.txt, exists=true, isDirectory=false, size=12, device=-1, inode=1, mode=511, owner=nobody, group=nobody, createTime=DateTime(1482710410000), modifiedTime=DateTime(0), lastAccessTime=DateTime(0), extraInfo=null, id=null)]",
				helloZip["hello"].list().toList().map { it.stat().toString(showFile = false) }.toString()
			)
		}
	}

	@Test
	fun testZipUncompressed3() = suspendTestNoBrowser {
		resourcesVfs["hello.zip"].openAsZip { helloZip ->
			assertEquals(
				"VfsStat(file=/hello/world.txt, exists=true, isDirectory=false, size=12, device=-1, inode=1, mode=511, owner=nobody, group=nobody, createTime=DateTime(1482710410000), modifiedTime=DateTime(0), lastAccessTime=DateTime(0), extraInfo=null, id=null)",
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

			//println(helloZip.stat())
			assertEquals(true, helloZip.exists())
			assertEquals(true, helloZip.isDirectory())
			assertEquals(true, helloZip["/"].isDirectory())
			val mem = MemoryVfs()
			helloZip.copyToRecursively(mem)
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

    @Test
    fun testCopy() = suspendTestNoBrowser {
        val data = MemoryVfs()

        resourcesVfs["compressedHello.zip"].openAsZip { helloZip ->
            helloZip["hello/compressedWorld.txt"].copyTo(data["out.txt"])
        }
        assertEquals(
            "HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO HELLO WORLD!",
            data["out.txt"].readString()
        )
    }

    @Test
    fun testSeekNotAvailable() = suspendTestNoBrowser {
        resourcesVfs["compressedHello.zip"].openAsZip { helloZip ->
            val stream = helloZip["hello/compressedWorld.txt"].open()
            stream.setPosition(10L)
            assertFailsWith(SeekNotSupportedException::class) { stream.read() }
        }
    }

	//@Test
	//fun testZip1() = suspendTestNoBrowser {
	//	val mem = MemoryVfs()
	//	//UrlVfs("https://github.korlibs/korge-tools/releases/download/binaries/rhubarb-lip-sync-1.4.2-win32.zip").copyTo(LocalVfs["c:/temp/file.zip"])
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

	@Test
	fun testSizeNotInHeader() = suspendTestNoBrowser {
		val sizes = mutableMapOf<String, Long>()
		resourcesVfs["android200-sqlite.cblite2.zip"].openAsZip { cblZip ->
			cblZip.listRecursive().collect {
				if (!it.isDirectory()) {
					assertNotEquals(0, it.size())
					sizes[it.fullName] = it.size()
				}
			}

			val mem = MemoryVfs()
			cblZip.copyToRecursively(mem)
			mem.listRecursive().collect {
				if (!it.isDirectory()) {
					assertEquals(sizes[it.fullName], it.size())
				}
			}
		}
	}
}

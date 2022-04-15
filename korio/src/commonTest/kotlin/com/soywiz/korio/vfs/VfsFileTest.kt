package com.soywiz.korio.vfs

import com.soywiz.klock.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.lang.*
import com.soywiz.korio.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.test.*

class VfsFileTest {
    @Test
    fun testRelativePath() {
        val file = MemoryVfs()
        val file1 = file["my/current/path.txt"]
        val file2 = file["my/demo/test/"]
        assertEquals("../../current/path.txt", file1.relativePathTo(file2))
    }

	@Test
	fun name() = suspendTest {
		val file = MemoryVfs()["C:\\this\\is\\a\\test.txt"]
		assertEquals("C:/this/is/a", file.parent.fullName)
	}

    @Test
    fun testLocalRead() = suspendTest({ OS.isJvm }) {
        val processedFileNames = arrayListOf<String>()
        println("************************************")
        localCurrentDirVfs.list().filter { it.baseName == "build.gradle" }.collect {
            if (it.isFile()) {
                println("$it: ${it.readAll().size}")
                processedFileNames += it.baseName
            }
        }
        println("************************************")
        assertEquals(listOf("build.gradle"), processedFileNames)
    }

	@Test
	fun memoryNonExists() = suspendTest {
		val file = MemoryVfs()
		assertEquals(false, file["test"].exists())
	}

	@Test
	fun testCaseSensitiveAccess() = suspendTest {
		val file = MemoryVfs(caseSensitive = true)
		file["test.tXt"].writeString("hello world")
		assertEquals(true, file["test.tXt"].exists())
		assertEquals(false, file["test.txt"].exists())
		assertEquals(false, file["test.TXT"].exists())
	}

	@Test
	fun testCaseInsensitiveAccess() = suspendTest {
		val file = MemoryVfs(caseSensitive = false)
		file["test.tXt"].writeString("hello world")
		assertEquals(true, file["test.txt"].exists())
		assertEquals(true, file["test.tXt"].exists())
		assertEquals(true, file["test.TXT"].exists())
	}

	@Test
	fun redirected() = suspendTest {
		var out = ""
		val file = MemoryVfsMix(
			"hello.txt" to "yay!",
			"hello.bin" to "NEVER-HERE"
		).redirected {
			out += this[it].readString()
			PathInfo(it).fullPathWithExtension("txt")
		}

		assertEquals("yay!", file["hello.txt"].readString())
		assertEquals("yay!", out)
		assertEquals("yay!", file["hello.bin"].readString())
		assertEquals("yay!NEVER-HERE", out)
		//assertEquals("ay", file["hello.bin"].readRangeBytes(1L .. 2L).toString(Charsets.UTF_8)) // CompilationException in Kotlin 1.1.1 -> Couldn't transform method node (probably related to long)
		assertEquals("ay", file["hello.bin"].readRangeBytes(1..2).toString(UTF8))
		assertEquals("ay!", file["hello.bin"].readRangeBytes(1..200).toString(UTF8))
	}

	@Test
	fun avoidStats() = suspendTest {
		val log = LogVfs(MemoryVfsMix("hello.txt" to "yay!"))
		val root = log.root
		root["hello.txt"].readBytes()
		assertEquals(
			"[readRange(/hello.txt, 0..9223372036854775807)]",
			log.logstr
		)
	}

	@Test
	fun testUnescaped() = suspendTest {
		val result = JailVfs(UrlVfs("http://demo.com/demo.txt")).getUnderlyingUnscapedFile()
		//println("result: $result")
		assertEquals(true, result.vfs is UrlVfs)
		assertEquals("/demo.txt", result.path)
	}

	@Test
	fun testUnescapedCallsInit() = suspendTest {
		val memoryVfs = MemoryVfs()
		var initialized = false
		val vfs = object : Vfs.Proxy() {
			override suspend fun access(path: String): VfsFile = memoryVfs[path]
			override suspend fun init() { initialized = true }
		}.root
		assertEquals(memoryVfs["test"], vfs["test"].getUnderlyingUnscapedFile().toFile())
		assertEquals(initialized, true)
	}

    @Test
    fun testRename() = suspendTest {
        // rename is relative to the vfs file folder
        val root = MemoryVfsMix("build/out.txt" to "HELLO")
        suspend fun readString(path: String) = "$path:${root[path].takeIfExists()?.readString()}"
        root["build"].rename("out.txt", "out2.txt")
        assertEquals(
            """
                build/out.txt:null
                build/out2.txt:HELLO
                out2.txt:null
                [NodeVfs[/build], NodeVfs[/build/out2.txt]]
            """.trimIndent(),
            """
                ${readString("build/out.txt")}
                ${readString("build/out2.txt")}
                ${readString("out2.txt")}
                ${root.listRecursive().toList()}
            """.trimIndent()
        )
    }

    @Test
    fun testRenameTo() = suspendTest {
        // renameTo is relative to the root of the vfs
        val root = MemoryVfsMix("build/out.txt" to "HELLO")
        suspend fun readString(path: String) = "$path:${root[path].takeIfExists()?.readString()}"
        root["build/out.txt"].renameTo("out2.txt")
        assertEquals(
            """
                build/out.txt:null
                build/out2.txt:null
                out2.txt:HELLO
                [NodeVfs[/build], NodeVfs[/out2.txt]]
            """.trimIndent(),
            """
                ${readString("build/out.txt")}
                ${readString("build/out2.txt")}
                ${readString("out2.txt")}
                ${root.listRecursive().toList()}
            """.trimIndent()
        )
    }

    @Test
    fun testStatCancellation() = suspendTest {
        val deferred0 = CompletableDeferred<Unit>()
        val myvfs = object : Vfs() {
            override suspend fun stat(path: String): VfsStat {
                deferred0.complete(Unit)
                delay(1000.seconds)
                TODO()
            }
        }

        val deferred = CompletableDeferred<Unit>()
        val log = arrayListOf<String>()
        val job = launchImmediately {
            try {
                val stat = myvfs.root["hello.txt"].stat()
                log.add("$stat")
            } catch (e: Throwable) {
                log.add(e::class.portableSimpleName)
            } finally {
                deferred.complete(Unit)
            }
        }
        deferred0.await()
        delay(10.milliseconds)
        job.cancel()
        deferred.await()
        assertEquals(
            """
                JobCancellationException
            """.trimIndent(),
            log.joinToString("\n")
        )
    }
}

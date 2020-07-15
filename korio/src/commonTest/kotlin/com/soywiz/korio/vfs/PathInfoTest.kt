package com.soywiz.korio.vfs

import com.soywiz.korio.file.*
import kotlin.test.*

class PathInfoTest {
	@Test
	fun name() {
		PathInfo("/test/hello.TxT").apply {
			assertEquals("/test/hello.TxT", fullPath)
			assertEquals("/test/hello", fullPathWithoutExtension)
			assertEquals("/test/hello", fullNameWithoutCompoundExtension)
			assertEquals("/test", folder)
			assertEquals("hello.TxT", baseName)
			assertEquals("hello", baseNameWithoutExtension)
			assertEquals("TxT", extension)
			assertEquals("txt", extensionLC)
		}
	}

	@Test
	fun name2() {
		PathInfo("C:\\dev\\test\\hello.TxT").apply {
			assertEquals("C:\\dev\\test\\hello.TxT", fullPath)
			assertEquals("C:\\dev\\test\\hello", fullPathWithoutExtension)
			assertEquals("C:\\dev\\test\\hello", fullNameWithoutCompoundExtension)
			assertEquals("C:\\dev\\test", folder)
			assertEquals("hello.TxT", baseName)
			assertEquals("hello", baseNameWithoutExtension)
			assertEquals("TxT", extension)
			assertEquals("txt", extensionLC)
		}
	}

	@Test
	fun name3() {
		PathInfo("C:\\dev\\test\\hello").apply {
			assertEquals("C:\\dev\\test\\hello", fullPath)
			assertEquals("C:\\dev\\test\\hello", fullPathWithoutExtension)
			assertEquals("C:\\dev\\test\\hello", fullNameWithoutCompoundExtension)
			assertEquals("C:\\dev\\test", folder)
			assertEquals("hello", baseName)
			assertEquals("hello", baseNameWithoutExtension)
			assertEquals("", extension)
			assertEquals("", extensionLC)
		}
	}

	@Test
	fun name4() {
		PathInfo("C:\\dev\\test\\hello.Voice.Wav").apply {
			assertEquals("C:\\dev\\test\\hello.Voice.Wav", fullPath)
			assertEquals("C:\\dev\\test\\hello.Voice", fullNameWithoutExtension)
			assertEquals("C:\\dev\\test\\hello", fullPathWithoutExtension)
			assertEquals("C:\\dev\\test\\hello", fullNameWithoutCompoundExtension)
			assertEquals("C:\\dev\\test", folder)
			assertEquals("hello.Voice.Wav", baseName)
			assertEquals("hello.Voice", baseNameWithoutExtension)
			assertEquals("hello", baseNameWithoutCompoundExtension)
			assertEquals("Wav", extension)
			assertEquals("wav", extensionLC)
			assertEquals("Voice.Wav", compoundExtension)
			assertEquals("voice.wav", compoundExtensionLC)
		}
	}

	@Test
	fun name5() {
		PathInfo("C:\\dev\\test.demo\\hello.Voice.Wav").apply {
			assertEquals("C:\\dev\\test.demo\\hello.Voice.Wav", fullPath)
			assertEquals("C:\\dev\\test.demo\\hello.Voice", fullNameWithoutExtension)
			assertEquals("C:\\dev\\test.demo\\hello", fullPathWithoutExtension)
			assertEquals("C:\\dev\\test.demo\\hello", fullNameWithoutCompoundExtension)
			assertEquals("C:\\dev\\test.demo", folder)
			assertEquals("hello.Voice.Wav", baseName)
			assertEquals("hello.Voice", baseNameWithoutExtension)
			assertEquals("hello", baseNameWithoutCompoundExtension)
			assertEquals("Wav", extension)
			assertEquals("wav", extensionLC)
			assertEquals("Voice.Wav", compoundExtension)
			assertEquals("voice.wav", compoundExtensionLC)
		}
	}

	@Test
	fun getFullComponents() {
		assertEquals(listOf("a", "b", "c"), PathInfo("a/b/c").getPathComponents())
		assertEquals(listOf("a", "a/b", "a/b/c"), PathInfo("a/b/c").getPathFullComponents())
		assertEquals(listOf("a", "a/b", "a/b/"), PathInfo("a/b/").getPathFullComponents())
	}

	@Test
	fun basenameWithExtension() {
		assertEquals("c.jpg", PathInfo("a/b/c.txt").baseNameWithExtension("jpg"))
		assertEquals("c.jpg", PathInfo("a/b/c").baseNameWithExtension("jpg"))
		assertEquals("c", PathInfo("a/b/c.txt").baseNameWithExtension(""))
		assertEquals("c", PathInfo("a/b/c").baseNameWithExtension(""))
	}

	@Test
	fun pathWithExtension() {
		assertEquals("a/b/c.jpg", PathInfo("a/b/c.txt").fullPathWithExtension("jpg"))
		assertEquals("a/b/c.jpg", PathInfo("a/b/c").fullPathWithExtension("jpg"))
		assertEquals("a/b/c", PathInfo("a/b/c.txt").fullPathWithExtension(""))
		assertEquals("a/b/c", PathInfo("a/b/c").fullPathWithExtension(""))
	}
}
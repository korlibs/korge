package korlibs.io.vfs

import korlibs.io.file.PathInfo
import korlibs.io.file.baseName
import korlibs.io.file.baseNameWithExtension
import korlibs.io.file.baseNameWithoutCompoundExtension
import korlibs.io.file.baseNameWithoutExtension
import korlibs.io.file.compoundExtension
import korlibs.io.file.compoundExtensionLC
import korlibs.io.file.extension
import korlibs.io.file.extensionLC
import korlibs.io.file.folder
import korlibs.io.file.fullNameWithoutCompoundExtension
import korlibs.io.file.fullNameWithoutExtension
import korlibs.io.file.fullPathWithExtension
import korlibs.io.file.fullPathWithoutExtension
import korlibs.io.file.getPathComponents
import korlibs.io.file.getPathFullComponents
import korlibs.io.file.parent
import kotlin.test.Test
import kotlin.test.assertEquals

class PathInfoTest {
	@Test
	fun name() {
		PathInfo("/test/hello.TxT").apply {
			assertEquals("/test/hello.TxT", fullPath)
			assertEquals("/test/hello", fullPathWithoutExtension)
			assertEquals("/test/hello", fullNameWithoutCompoundExtension)
			assertEquals("/test", folder)
            assertEquals("/test", parent.fullPath)
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
            assertEquals("C:\\dev\\test", parent.fullPath)
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
            assertEquals("C:\\dev\\test", parent.fullPath)
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
    fun name6() {
        PathInfo("test").apply {
            assertEquals("test", fullPath)
            assertEquals("test", fullPathWithoutExtension)
            assertEquals("test", fullNameWithoutCompoundExtension)
            assertEquals("", folder)
            assertEquals("", parent.fullPath)
            assertEquals("test", baseName)
            assertEquals("test", baseNameWithoutExtension)
            assertEquals("", extension)
            assertEquals("", extensionLC)
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

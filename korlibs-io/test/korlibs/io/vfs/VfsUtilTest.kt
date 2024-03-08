package korlibs.io.vfs

import korlibs.io.file.combine
import korlibs.io.file.isAbsolute
import korlibs.io.file.pathInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VfsUtilTest {
	@Test
	fun combine() {
		assertEquals("c:/test/hello", """c:\test\demo""".pathInfo.combine("""..\.\hello""".pathInfo).fullPath)
		assertEquals("d:/lol", """c:\test\demo""".pathInfo.combine("""d:\lol""".pathInfo).fullPath)
		assertEquals("http://hello/world", """""".pathInfo.combine("""http://hello/world""".pathInfo).fullPath)
		assertEquals("http://hello/demo", """http://hello/world""".pathInfo.combine("""../demo""".pathInfo).fullPath)
		assertEquals("mailto:demo@demo.com",
			"""http://hello/world""".pathInfo.combine("""mailto:demo@demo.com""".pathInfo).fullPath
		)
	}

	@Test
	fun isAbsolute() {
		// Absolute
		assertTrue("""C:\.\.\hello""".pathInfo.isAbsolute())
		assertTrue("""/test""".pathInfo.isAbsolute())
		assertTrue("""http://hello""".pathInfo.isAbsolute())
		assertTrue("""ftp://hello""".pathInfo.isAbsolute())
		assertTrue("""mailto:demo@demo.com""".pathInfo.isAbsolute())

		// Relative
		assertFalse("""..\.\hello""".pathInfo.isAbsolute())
		assertFalse("""ftp//hello""".pathInfo.isAbsolute())
		assertFalse("""ftp//hello:world""".pathInfo.isAbsolute())
	}
}

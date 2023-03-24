package korlibs.io.vfs

import korlibs.io.async.suspendTest
import korlibs.io.file.VfsFile
import korlibs.io.file.std.MemoryVfs
import korlibs.io.lang.toCharArray
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadSpecialTest {
	class CharArray2(val width: Int, val height: Int, val data: CharArray) {
		fun index(x: Int, y: Int): Int = y * width + x
		operator fun get(x: Int, y: Int): Char = data[index(x, y)]
		operator fun set(x: Int, y: Int, v: Char) { data[index(x, y)] = v }
	}

	suspend fun VfsFile.readCharArray2(): CharArray2 {
		val text = this.readString()
		val side = sqrt(text.length.toDouble()).toInt()
		return CharArray2(side, side, text.toCharArray())
	}

	@Test
	fun testReadSpecial2() = suspendTest {
		val temp = MemoryVfs()
		val f2 = temp["korio.chararray2"]
		f2.writeString("123456789")
		val c2 = f2.readCharArray2()
		assertEquals('1', c2[0, 0])
		assertEquals('5', c2[1, 1])
		assertEquals('9', c2[2, 2])
	}
}

package com.soywiz.korfl

import com.soywiz.korge.view.ViewsLog
import com.soywiz.korge.view.dumpToString
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.vfs.ResourcesVfs
import org.junit.Test
import java.util.*

class KorflTest {
	val viewsLog = ViewsLog()
	val views = viewsLog.views

	@Test
	fun name3() = syncTest {
		val lib = ResourcesVfs["simple.swf"].readSWF(views)
		val mc = lib.createMainTimeLine()
		println(lib.fps)
		println(lib.msPerFrame)
		for (n in 0 until 10) {
			println(mc.dumpToString())
			mc.update(41)
		}
	}

	private fun binarySearch2(array: IntArray, v: Int): Int {
		val res = Arrays.binarySearch(array, v)
		return if (res < 0) -res - 1 else res
	}

	@Test
	fun name4() {
		val v = intArrayOf(7, 10, 14)
		println(binarySearch2(v, -10))
		println(binarySearch2(v, 0))
		println(binarySearch2(v, 7))
		println(binarySearch2(v, 8))
		println(binarySearch2(v, 10))
		println(binarySearch2(v, 11))
		println(binarySearch2(v, 14))
		println(binarySearch2(v, 20))
	}
}

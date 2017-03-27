package com.codeazur.as3swf

import com.codeazur.as3swf.tags.TagFileAttributes
import com.soywiz.korfl.AnMovieClip
import com.soywiz.korfl.readSWF
import com.soywiz.korge.view.ViewsLog
import com.soywiz.korge.view.dumpToString
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.util.hexString
import com.soywiz.korio.util.toHexString
import com.soywiz.korio.util.toUtf8String
import com.soywiz.korio.vfs.ResourcesVfs
import org.junit.Test

class As3SwfTest {
	val viewsLog = ViewsLog()
	val views = viewsLog.views

	@Test
	fun name() = syncTest {
		val swf2 = SWF().loadBytes(ResourcesVfs["empty.swf"].readAll())
		println(swf2.frameSize.rect)
		for (tag in swf2.tags) {
			println(tag)
		}

		val swf = SWF()
		swf.tags += TagFileAttributes()
		val emptyData = swf.publish()
		println(emptyData.toHexString())
		println(emptyData.toUtf8String())
	}

	@Test
	fun name2() = syncTest {
		val swf2 = SWF().loadBytes(ResourcesVfs["simple.swf"].readAll())
		println(swf2.frameSize.rect)
		for (tag in swf2.tags) {
			println(tag)
		}

		val swf = SWF()
		swf.tags += TagFileAttributes()
		val emptyData = swf.publish()
		println(emptyData.hexString)
		println(emptyData.toUtf8String())
	}

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
}

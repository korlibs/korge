package com.codeazur.as3swf

import com.codeazur.as3swf.tags.TagFileAttributes
import com.soywiz.korio.async.syncTest
import com.soywiz.korio.vfs.ResourcesVfs
import org.junit.Test

class As3SwfTest {
	@Test
	fun name() = syncTest {
		val swf2 = SWF().loadBytes(ResourcesVfs["empty.swf"].readAll())
		println(swf2.frameSize.rect)
		for (tag in swf2.tags) {
			println(tag)
		}

		val swf = SWF()
		swf.tags += TagFileAttributes()
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
	}


	@Test
	fun name3() = syncTest {
		val swf2 = SWF().loadBytes(ResourcesVfs["test1.swf"].readAll())
		println(swf2.frameSize.rect)
		for (tag in swf2.tags) {
			println(tag)
		}

		val swf = SWF()
		swf.tags += TagFileAttributes()
	}

	//@Test
	//@Ignore
	//fun name4() = syncTest {
	//	val swf2 = SWF().loadBytes(LocalVfs["c:/temp/ui.swf"].readAll())
	//	println(swf2.frameSize.rect)
	//	for (tag in swf2.tags) {
//
	//		println(tag)
	//	}
//
	//	val swf = SWF()
	//	swf.tags += TagFileAttributes()
	//}

	//@Test
	//fun name4() = syncTest {
	//	//val swf2 = SWF().loadBytes(File("c:/temp/sample1.swf").readBytes())
	//	println(swf2.frameSize.rect)
	//	for (tag in swf2.tags) {
	//		println(tag)
	//	}

	//}
}

package com.dragonbones.parser

import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import kotlin.test.*

class DataParserTest {
	@Test
	fun name() = suspendTest {
		//val data = BinaryDataParser().parseDragonBonesDataJson(resourcesVfs["Dragon/Dragon_ske.json"].readString())
		val json = MyResourcesVfs["Dragon/Dragon_ske.json"].readString()
		val data = DataParser.parseDragonBonesDataJson(json)!!
		assertEquals(listOf("Dragon"), data.armatureNames)
		//println(data)
	}
}

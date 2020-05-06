package com.dragonbones.parser

import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import doIOTest
import kotlin.test.*

class DataParserTest {
	@Test
	fun name() = suspendTest({ doIOTest }) {
		//val data = BinaryDataParser().parseDragonBonesDataJson(resourcesVfs["Dragon/Dragon_ske.json"].readString())
		val json = resourcesVfs["Dragon/Dragon_ske.json"].readString()
		val data = DataParser.parseDragonBonesDataJson(json)!!
		assertEquals(listOf("Dragon"), data.armatureNames)
		//println(data)
	}
}

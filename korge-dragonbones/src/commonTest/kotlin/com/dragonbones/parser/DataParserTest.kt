package com.dragonbones.parser

import com.soywiz.korge.dragonbones.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import doIOTest
import kotlin.test.*

class DataParserTest {
	@Test
	fun testReadingJson() = suspendTest({ doIOTest }) {
		//val data = BinaryDataParser().parseDragonBonesDataJson(resourcesVfs["Dragon/Dragon_ske.json"].readString())
		val json = resourcesVfs["Dragon/Dragon_ske.json"].readString()
		val data = DataParser.parseDragonBonesDataJson(json)!!
		assertEquals(listOf("Dragon"), data.armatureNames)
		//println(data)
	}

    @Test
    fun testReadingBinary() = suspendTest({ doIOTest }) {
        val data = resourcesVfs["Dragon/Dragon_ske.dbbin"].readDbSkeleton(KorgeDbFactory())
        assertEquals(listOf("Dragon"), data.armatureNames)
        //println(data)
    }
}

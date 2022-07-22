package com.dragonbones.parser

import com.soywiz.korge.dragonbones.*
import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.serialization.json.*
import kotlin.test.*

class KorgeDbViewsTestJvm {
	@Test
	fun test() = suspendTest {
		val factory = KorgeDbFactory()
		val data = factory.parseDragonBonesData(Json.parse(resourcesVfs["Dragon/Dragon_ske.json"].readString())!!)
		val atlas = factory.parseTextureAtlasData(
			Json.parse(resourcesVfs["Dragon/Dragon_tex.json"].readString())!!,
			resourcesVfs["Dragon/Dragon_tex.png"].readBitmap().toBMP32()
		)
		val armatureDisplay = factory.buildArmatureDisplay("Dragon", "Dragon")!!.position(100, 100)
		armatureDisplay.dbUpdate()
		factory.clock.advanceTime(0.1)
		//armatureDisplay.dump()
		//println("--------------------")
		//armatureDisplay.dump()
	}

    @Test
    fun testOutOfBoundsSample() = suspendTest {
        val factory = KorgeDbFactory()
        val skeJsonData = resourcesVfs["503/503_ske.json"].readString()
        val texJsonData = resourcesVfs["503/503_tex.json"].readString()

        val dragonBonesData = Json.parse(skeJsonData) ?: throw Exception("Parse ske data error!")
        factory.parseDragonBonesData(dragonBonesData)

        val textureAtlasData = Json.parse(texJsonData) ?: throw Exception("Parse tex data error!")
        factory.parseTextureAtlasData(textureAtlasData, Bitmap32(1, 1))
    }
}

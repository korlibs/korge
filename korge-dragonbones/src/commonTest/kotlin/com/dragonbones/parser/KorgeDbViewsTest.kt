package com.dragonbones.parser

import com.soywiz.korge.dragonbones.*
import com.soywiz.korge.view.*
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.serialization.json.*
import doIOTest
import kotlin.test.*

class KorgeDbViewsTest {
	@Test
	fun test() = suspendTest({ doIOTest }) {
		val factory = KorgeDbFactory()
		val data = factory.parseDragonBonesData(Json.parseFast(resourcesVfs["Dragon/Dragon_ske.json"].readString())!!)
		val atlas = factory.parseTextureAtlasData(
			Json.parseFast(resourcesVfs["Dragon/Dragon_tex.json"].readString())!!,
			//MyResourcesVfs["Dragon/Dragon_tex.png"].readBitmapOptimized().toBMP32()
			resourcesVfs["Dragon/Dragon_tex.png"].readBitmap(PNG).toBMP32()
		)
		val armatureDisplay = factory.buildArmatureDisplay("Dragon", "Dragon")!!.position(100, 100)
		armatureDisplay.dbUpdate()
		factory.clock.advanceTime(0.1)
		//armatureDisplay.dump()
		//println("--------------------")
		//armatureDisplay.dump()
	}
}

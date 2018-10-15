package com.dragonbones.parser

import com.soywiz.korge.dragonbones.*
import com.soywiz.korge.view.*
import com.soywiz.korim.format.*
import com.soywiz.korio.async.*
import com.soywiz.korio.serialization.json.*
import kotlin.test.*

class KorgeDbViewsTestJvm {
	@Test
	fun test() = suspendTest {
		val factory = KorgeDbFactory()
		val data = factory.parseDragonBonesData(Json.parse(MyResourcesVfs["Dragon/Dragon_ske.json"].readString())!!)
		val atlas = factory.parseTextureAtlasData(
			Json.parse(MyResourcesVfs["Dragon/Dragon_tex.json"].readString())!!,
			MyResourcesVfs["Dragon/Dragon_tex.png"].readBitmapOptimized().toBMP32()
		)
		val armatureDisplay = factory.buildArmatureDisplay("Dragon", "Dragon")!!.position(100, 100)
		armatureDisplay.dbUpdate()
		factory.clock.advanceTime(0.1)
		armatureDisplay.dump()
		println("--------------------")
		armatureDisplay.dump()
	}
}

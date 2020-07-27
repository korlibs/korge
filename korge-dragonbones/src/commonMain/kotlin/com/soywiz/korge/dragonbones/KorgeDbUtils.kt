package com.soywiz.korge.dragonbones

import com.dragonbones.model.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korio.dynamic.*
import com.soywiz.korio.file.*
import com.soywiz.korio.serialization.json.*

suspend fun VfsFile.readDbAtlas(factory: KorgeDbFactory): TextureAtlasData {
	val jsonFile = this
	val tex = jsonFile.readString()
	val texInfo = Json.parse(tex, Json.Context(optimizedNumericLists = true))!!
	val imageFile = jsonFile.parent[KDynamic { texInfo["imagePath"].str }]
	val image = imageFile.readBitmapOptimized().mipmaps()
	return factory.parseTextureAtlasData(Json.parse(tex, Json.Context(optimizedNumericLists = true))!!, image)
}

suspend fun VfsFile.readDbSkeleton(factory: KorgeDbFactory): DragonBonesData {
	val ske = Json.parse(this.readString(), Json.Context(optimizedNumericLists = true))!!
	return factory.parseDragonBonesData(ske) ?: error("Can't load skeleton $this")
}

suspend fun VfsFile.readDbSkeletonAndAtlas(factory: KorgeDbFactory): DragonBonesData {
	val atlas = this.parent[this.baseName.replace("_ske", "_tex")].readDbAtlas(factory)
	val skel = this.readDbSkeleton(factory)
	return skel
}

fun DragonBonesData.buildFirstArmatureDisplay(factory: KorgeDbFactory) =
	factory.buildArmatureDisplay(this.armatureNames.firstOrNull() ?: error("DbData doesn't have armatures"))

package com.soywiz.korge.dragonbones

import com.dragonbones.model.*
import com.soywiz.kmem.MemBufferWrap
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korio.dynamic.*
import com.soywiz.korio.file.*
import com.soywiz.korio.serialization.json.*

suspend fun VfsFile.readDbAtlas(factory: KorgeDbFactory): TextureAtlasData? {
	val jsonFile = this
	val tex = jsonFile.readString()
	val texInfo = Json.parseFast(tex)!!
	val imageFile = jsonFile.parent[KDynamic { texInfo["imagePath"].str }].takeIfExists() ?: return null
	val image = imageFile.readBitmap().mipmaps()
	return factory.parseTextureAtlasData(Json.parseFast(tex)!!, image)
}

suspend fun VfsFile.readDbSkeleton(factory: KorgeDbFactory): DragonBonesData {
    val file = this
    val ske = when {
        this.extensionLC.endsWith("json") -> Json.parseFast(this.readString())!!
        this.extensionLC.endsWith("dbbin") -> MemBufferWrap(this.readBytes())
        else -> error("Unsupported DragonBones skeleton ${this.baseName} : ${this.extension}")
    }
    // JSON including textureAtlas
    if (ske is Map<*, *> && ske.containsKey("textureAtlas")) {
        KDynamic {
            val textureAtlasList = ske["textureAtlas"]
            for (textureAtlas in textureAtlasList.list) {
                val imagePath = textureAtlas["imagePath"].str
                val imageFile = file.parent[imagePath].takeIfExists()
                if (imageFile != null) {
                    val image = imageFile.readBitmap().mipmaps()
                    factory.parseTextureAtlasData(textureAtlas!!, image)
                }
            }
        }
    }
    return factory.parseDragonBonesData(ske) ?: error("Can't load skeleton $this")
}

suspend fun VfsFile.readDbSkeletonAndAtlas(factory: KorgeDbFactory): DragonBonesData {
	this.parent[this.baseName.replace("_ske", "_tex").replace(".dbbin", ".json")].takeIf { it.exists() }?.readDbAtlas(factory)
	return this.readDbSkeleton(factory)
}

fun DragonBonesData.buildFirstArmatureDisplay(factory: KorgeDbFactory) =
	factory.buildArmatureDisplay(this.armatureNames.firstOrNull() ?: error("DbData doesn't have armatures"))

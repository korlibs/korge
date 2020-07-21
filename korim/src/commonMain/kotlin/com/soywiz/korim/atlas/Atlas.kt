package com.soywiz.korim.atlas

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.*

class Atlas(val info: AtlasInfo, val textures: Map<String, BitmapSlice<Bitmap>>) {
    constructor(info: AtlasInfo, texture: BitmapSlice<Bitmap>) : this(info, mapOf(info.pages.first().fileName to texture))

    val texture get() = textures.values.first()

    inner class Entry(val info: AtlasInfo.Region, val page: AtlasInfo.Page) {
        val texture = textures[page.fileName]
            ?: error("Can't find '${page.fileName}' in ${textures.keys}")
        val slice = texture.slice(info.frame.rect).let {
            BitmapSlice(it.bmp, it.bounds, info.name, info.rotated)
        }
        val name get() = info.name
        // @TODO: Use name instead
        val filename get() = info.name
    }

	val entries = info.pages.flatMap { page ->
        page.regions.map { frame ->
            Entry(frame, page)
        }
    }
    val entriesMap = entries.associateBy { it.filename }

    fun tryGetEntryByName(name: String): Entry? = entriesMap[name]
    fun tryGet(name: String): BmpSlice? = tryGetEntryByName(name)?.slice
	operator fun get(name: String): BmpSlice = tryGet(name) ?: error("Can't find '$name' it atlas")
}

suspend fun VfsFile.readAtlas(): Atlas {
    val content = this.readString()
    val info = when {
        content.startsWith("{") -> AtlasInfo.loadJsonSpriter(content)
        content.startsWith("<") -> AtlasInfo.loadXml(content)
        content.startsWith('\n') -> AtlasInfo.loadText(content)
        else -> error("Unexpected atlas starting with '${content.firstOrNull()}'")
    }
    val folder = this.parent
    val textures = info.pages.associate { it.fileName to folder[it.fileName].readBitmapSlice() }
    return Atlas(info, textures)
}

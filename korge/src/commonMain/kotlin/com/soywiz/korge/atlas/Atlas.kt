package com.soywiz.korge.atlas

import com.soywiz.korge.view.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import com.soywiz.korio.file.*

class Atlas(val info: AtlasInfo, val texture: BitmapSlice<Bitmap>) {
    inner class Entry(val info: AtlasInfo.Entry) {
        val slice = texture.slice(info.frame.rect).let {
            BitmapSlice(it.bmp, it.bounds, info.filename, info.rotated)
        }
        val filename get() = info.filename
    }

	val entries = info.frames.map { Entry(it) }
    val entriesMap = entries.associateBy { it.filename }

    fun tryGetEntryByName(name: String): Entry? = entriesMap[name]
    fun tryGet(name: String): BmpSlice? = tryGetEntryByName(name)?.slice
	operator fun get(name: String): BmpSlice = tryGet(name) ?: error("Can't find '$name' it atlas")
}

@Deprecated("")
suspend fun VfsFile.readAtlas(views: Views): Atlas = readAtlas()
suspend fun VfsFile.readAtlas(): Atlas {
    val info = AtlasInfo.loadJsonSpriter(this.readString())
    val folder = this.parent
    val atlasTex = folder[info.image].readBitmapSlice()
    return Atlas(info, atlasTex)
}

package com.soywiz.korim.atlas

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korim.bitmap.BitmapSlice
import com.soywiz.korim.bitmap.BmpSlice
import com.soywiz.korim.bitmap.asumePremultiplied
import com.soywiz.korim.bitmap.virtFrame
import com.soywiz.korim.format.readBitmapSlice
import com.soywiz.korio.file.VfsFile

class Atlas(val textures: Map<String, BitmapSlice<Bitmap>>, val info: AtlasInfo = AtlasInfo()) : AtlasLookup {
    constructor(texture: BitmapSlice<Bitmap>, info: AtlasInfo = AtlasInfo()) : this(mapOf(info.pages.first().fileName to texture), info)
    constructor(slices: List<BitmapSlice<Bitmap>>) : this(slices.mapIndexed { index, bmp -> (bmp.name.takeIf { it != "unknown" } ?: "$index") to bmp }.toMap())

    val texture get() = textures.values.first()

    inner class Entry(val info: AtlasInfo.Region, val page: AtlasInfo.Page) {
        val texture = textures[page.fileName]
            ?: error("Can't find '${page.fileName}' in ${textures.keys}")
        val slice = texture.slice(info.frame.toRectangleInt(), info.name, info.imageOrientation)
            .virtFrame(info.virtFrame?.toRectangleInt())
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

    override fun tryGetEntryByName(name: String): Entry? = entriesMap[name]
}

interface AtlasLookup {
    fun tryGetEntryByName(name: String): Atlas.Entry?
    fun tryGet(name: String): BmpSlice? = tryGetEntryByName(name)?.slice
    operator fun get(name: String): BmpSlice = tryGet(name)
        ?: error("Can't find '$name' it atlas")
}

suspend fun VfsFile.readAtlas(asumePremultiplied: Boolean = false): Atlas {
    val content = this.readString()
    val info = when {
        content.startsWith("{") -> AtlasInfo.loadJsonSpriter(content)
        content.startsWith("<") -> AtlasInfo.loadXml(content)
        content.startsWith('\n') -> AtlasInfo.loadText(content)
        content.startsWith("\r\n") -> AtlasInfo.loadText(content)
        else -> error("Unexpected atlas starting with '${content.firstOrNull()}'")
    }
    val folder = this.parent
    val textures = info.pages.associate {
        it.fileName to folder[it.fileName].readBitmapSlice(premultiplied = !asumePremultiplied).also {
            if (asumePremultiplied) it.bmpBase.asumePremultiplied()
        }
    }
    return Atlas(textures, info)
}

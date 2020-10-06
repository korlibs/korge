package com.soywiz.korim.atlas

import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.binpack.BinPacker

class MutableAtlas<T>(var binPacker: BinPacker, val border: Int = 2, val premultiplied: Boolean = true, val allowToGrow: Boolean = true) {
    constructor(width: Int, height: Int, border: Int = 2, allowToGrow: Boolean = true) : this(BinPacker(width, height), border)
    val width get() = binPacker.width.toInt()
    val height get() = binPacker.height.toInt()

    data class Entry<T>(val slice: BitmapSlice<Bitmap32>, val data: T) {
        val name get() = slice.name
    }

    //val bitmap = NativeImage(binPacker.width.toInt(), binPacker.height.toInt(), premultiplied = premultiplied)
    var bitmap = Bitmap32(width, height, premultiplied = premultiplied)
    val entries = arrayListOf<Entry<T>>()
    val entriesByName = LinkedHashMap<String, Entry<T>>()
    val size get() = entries.size

    fun reconstructWithSize(width: Int, height: Int) {
        val slices = entries.toList()
        binPacker = BinPacker(width, height)
        bitmap = Bitmap32(width, height, premultiplied = premultiplied)
        entriesByName.clear()
        entries.clear()
        for (entry in slices) add(entry.slice, entry.data, entry.slice.name)
    }

    fun add(bmp: Bitmap32, data: T, name: String? = null) = add(bmp.slice(name = "Slice$size"), data, name)

    fun add(bmp: BitmapSlice<Bitmap32>, data: T, name: String? = bmp.name): Entry<T> {
        try {
            val rname = name ?: "Slice$size"
            val rect = binPacker.add(bmp.width.toDouble() + border * 2, bmp.height.toDouble() + border * 2)
            val slice = this.bitmap.sliceWithSize(
                (rect.left + border).toInt(),
                (rect.top + border).toInt(),
                bmp.width,
                bmp.height,
                rname
            )
            val dstX = slice.left
            val dstY = slice.top
            this.bitmap.draw(bmp, dstX, dstY)
            //bmp.bmp.copy(srcX, srcY, this.bitmap, dstX, dstY, w, h)
            val entry = Entry(slice, data)
            entries += entry
            entriesByName[rname] = entry
            bitmap.contentVersion++
            return entry
        } catch (e: Throwable) {
            if (!allowToGrow) throw e
            reconstructWithSize(this.width * 2, this.height * 2)
            return this.add(bmp, data, name)
        }
    }

    fun toImmutable(): Atlas {
        val bitmap = this.bitmap.clone()
        return Atlas(this.entries.map {
            val slice = it.slice
            bitmap.sliceWithBounds(slice.left, slice.top, slice.width, slice.height, slice.name)
        })
    }
}

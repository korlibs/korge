package com.soywiz.korim.atlas

import com.soywiz.korim.bitmap.*
import com.soywiz.korma.geom.*
import com.soywiz.korma.geom.binpack.*

typealias MutableAtlasUnit = MutableAtlas<Unit>

fun MutableAtlasUnit.add(bmp: Bitmap32, name: String? = "Slice$size") = add(bmp, Unit, name)
fun MutableAtlasUnit.add(bmp: BmpSlice, name: String? = bmp.name) = this.add(bmp, Unit, name)
fun MutableAtlasUnit.add(bmp: BitmapSlice<Bitmap32>, name: String? = bmp.name) = this.add(bmp, Unit, name)

class MutableAtlas<T>(
    var binPacker: BinPacker,
    val border: Int = 2,
    val premultiplied: Boolean = true,
    val allowToGrow: Boolean = true,
    val growMethod: GrowMethod = GrowMethod.NEW_IMAGES
) {
    constructor(
        width: Int = 2048,
        height: Int = width,
        border: Int = 2,
        premultiplied: Boolean = true,
        allowToGrow: Boolean = true,
        growMethod: GrowMethod = GrowMethod.NEW_IMAGES
    ) : this(BinPacker(width, height), border, premultiplied, allowToGrow, growMethod)

    val width get() = binPacker.width.toInt()
    val height get() = binPacker.height.toInt()

    enum class GrowMethod { GROW_IMAGE, NEW_IMAGES }

    data class Entry<T>(val slice: BitmapSlice<Bitmap32>, val data: T) {
        val name get() = slice.name
    }

    //val bitmap = NativeImage(binPacker.width.toInt(), binPacker.height.toInt(), premultiplied = premultiplied)
    var bitmap = Bitmap32(width, height, premultiplied = premultiplied)
    val allBitmaps = arrayListOf<Bitmap32>(bitmap)
    val entries = arrayListOf<Entry<T>>()
    val entriesByName = LinkedHashMap<String, Entry<T>>()
    val size get() = entries.size

    private fun reconstructWithSize(width: Int, height: Int) {
        val slices = entries.toList()
        binPacker = BinPacker(width, height)
        bitmap = Bitmap32(width, height, premultiplied = premultiplied)
        allBitmaps.clear()
        allBitmaps.add(bitmap)
        entriesByName.clear()
        entries.clear()
        for (entry in slices) add(entry.slice, entry.data, entry.slice.name)
    }

    private fun growAtlas(bmp: BmpSlice) {
        when (growMethod) {
            GrowMethod.GROW_IMAGE -> reconstructWithSize(this.width * 2, this.height * 2)
            GrowMethod.NEW_IMAGES -> {
                if (bmp.width > width || bmp.height > height) error("Atlas is too small (${width}x${height}) to hold a slice of (${bmp.width}x${bmp.height})")
                binPacker = BinPacker(width, height)
                bitmap = Bitmap32(width, height, premultiplied = premultiplied)
                allBitmaps.add(bitmap)
            }
        }
    }

    fun add(bmp: Bitmap32, data: T, name: String? = "Slice$size") = add(bmp.slice(name = name), data, name)

    @Suppress("UNCHECKED_CAST")
    fun add(bmp: BmpSlice, data: T, name: String? = bmp.name): Entry<T> = when {
        bmp is BitmapSlice<*> && bmp.bmp is Bitmap32 -> add(bmp as BitmapSlice<Bitmap32>, data, name)
        else -> add(bmp.extract().toBMP32IfRequired(), data, name)
    }

    var biggestEmptyEntry: Entry<T>? = null

    fun add(bmp: BitmapSlice<Bitmap32>, data: T, name: String? = bmp.name): Entry<T> {
        try {
            val rname = name ?: "Slice$size"
            val isEmpty = bmp.isFullyTransparent()
            var entry: Entry<T>? = null

            if (isEmpty && biggestEmptyEntry != null) {
                val bigEmptySlice = biggestEmptyEntry!!.slice
                if (bigEmptySlice.width >= bmp.width && bigEmptySlice.height >= bmp.height) {
                    entry = Entry(
                        bigEmptySlice.slice(RectangleInt(0, 0, bmp.width, bmp.height)),
                        data
                    )
                }
            }

            if (entry == null) {
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
                this.bitmap.expandBorder(slice.bounds, border)
                //bmp.bmp.copy(srcX, srcY, this.bitmap, dstX, dstY, w, h)
                entry = Entry(slice, data)

                if (biggestEmptyEntry == null || bmp.area > biggestEmptyEntry!!.slice.area) {
                    biggestEmptyEntry = entry
                }
            }

            entries += entry
            entriesByName[rname] = entry
            bitmap.contentVersion++

            return entry
        } catch (e: Throwable) {
            if (!allowToGrow) throw e
            growAtlas(bmp)
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

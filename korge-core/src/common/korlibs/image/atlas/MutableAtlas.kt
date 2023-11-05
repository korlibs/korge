package korlibs.image.atlas

import korlibs.image.bitmap.*
import korlibs.math.geom.*
import korlibs.math.geom.binpack.*
import kotlin.collections.set
import kotlin.collections.toList

typealias MutableAtlasUnit = MutableAtlas<Unit>

fun MutableAtlasUnit.add(bmp: Bitmap32, name: String? = "Slice$size") = add(bmp, Unit, name)
fun MutableAtlasUnit.add(bmp: BmpSlice32, name: String? = bmp.name) = this.add(bmp, Unit, name)

class MutableAtlas<T>(
    var binPacker: BinPacker,
    val border: Int = 2,
    val premultiplied: Boolean = true,
    val allowToGrow: Boolean = true,
    val growMethod: GrowMethod = GrowMethod.NEW_IMAGES
) {
    private val borderMargin = MarginInt(border)

    constructor(
        width: Int = 2048,
        height: Int = width,
        border: Int = 2,
        premultiplied: Boolean = true,
        allowToGrow: Boolean = true,
        growMethod: GrowMethod = GrowMethod.NEW_IMAGES
    ) : this(BinPacker(width, height), border, premultiplied, allowToGrow, growMethod)

    val width: Int get() = binPacker.width.toInt()
    val height: Int get() = binPacker.height.toInt()

    enum class GrowMethod { GROW_IMAGE, NEW_IMAGES }

    data class Entry<T>(val slice: BmpSlice32, val data: T) {
        val name get() = slice.name
    }

    //val bitmap = NativeImage(binPacker.width.toInt(), binPacker.height.toInt(), premultiplied = premultiplied)
    var bitmap = Bitmap32(width, height, premultiplied = premultiplied)
    val allBitmaps = arrayListOf<Bitmap32>(bitmap)
    val entries = arrayListOf<Entry<T>>()
    val entriesByName = LinkedHashMap<String, Entry<T>>()
    val size get() = entries.size

    operator fun contains(name: String): Boolean = name in entriesByName

    operator fun get(name: String): BmpSlice = entriesByName[name]?.slice
        ?: error("Can't find '$name' it atlas")

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

    private fun growAtlas(bmp: BmpSlice32) {
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

    fun add(bmp: Bitmap32, data: T, name: String? = "Slice$size"): Entry<T> = add(bmp.slice(name = name), data, name)

    @Suppress("UNCHECKED_CAST")
    fun add(bmp: BmpSlice, data: T, name: String? = bmp.name): Entry<T> {
        if (bmp.bmp !is Bitmap32) {
            return add(bmp.extract().toBMP32(), data, name)
        }
        bmp as BmpSlice32
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
                val slice: BmpSlice32 = this.bitmap.sliceWithSize(
                    (rect.left + border).toInt(),
                    (rect.top + border).toInt(),
                    bmp.width,
                    bmp.height,
                    rname
                )
                val dstX = slice.left
                val dstY = slice.top
                val boundsWithBorder: RectangleInt = slice.bounds.expanded(borderMargin)
                this.bitmap.lock(boundsWithBorder) {
                    this.bitmap.draw(bmp, dstX, dstY)
                    this.bitmap.expandBorder(slice.rect, border)
                }
                //bmp.bmp.copy(srcX, srcY, this.bitmap, dstX, dstY, w, h)
                entry = Entry(slice, data)

                if (biggestEmptyEntry == null || bmp.area > biggestEmptyEntry!!.slice.area) {
                    biggestEmptyEntry = entry
                }
            }

            entries += entry
            entriesByName[rname] = entry

            return entry
        } catch (e: BinPacker.ImageDoNotFitException) {
            if (!allowToGrow) throw e
            growAtlas(bmp)
            return this.add(bmp, data, name)
        }
    }

    var biggestEmptyEntry: Entry<T>? = null

    fun toImmutable(): Atlas {
        val bitmap = this.bitmap.clone()
        return Atlas(this.entries.map {
            val slice = it.slice
            bitmap.sliceWithBounds(slice.left, slice.top, slice.width, slice.height, slice.name)
        })
    }
}

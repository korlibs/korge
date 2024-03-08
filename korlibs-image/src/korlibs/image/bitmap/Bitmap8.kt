package korlibs.image.bitmap

import korlibs.datastructure.*
import korlibs.image.color.*

class Bitmap8(
	width: Int,
	height: Int,
	data: ByteArray = ByteArray(width * height),
	palette: RgbaArray = RgbaArray(0x100)
) : BitmapIndexed(8, width, height, data, palette) {
	override fun createWithThisFormat(width: Int, height: Int): Bitmap = Bitmap8(width, height, palette = palette)

	override fun setInt(x: Int, y: Int, color: Int) = setIntIndex(index(x, y), color)
	override fun getInt(x: Int, y: Int): Int = datau[index(x, y)]
	override fun getRgbaRaw(x: Int, y: Int): RGBA = palette[get(x, y)]
    override fun getIntIndex(n: Int): Int = datau[n]
    override fun setIntIndex(n: Int, color: Int) { datau[n] = color }

    override fun copyUnchecked(srcX: Int, srcY: Int, dst: Bitmap, dstX: Int, dstY: Int, width: Int, height: Int) {
        if (dst !is Bitmap8) return super.copyUnchecked(srcX, srcY, dst, dstX, dstY, width, height)
        for (y in 0 until height) {
            korlibs.memory.arraycopy(this.data, this.index(srcX, srcY + y), (dst as Bitmap8).data, dst.index(dstX, dstY + y), width)
        }
    }

    override fun clone() = Bitmap8(width, height, data.copyOf(), RgbaArray(palette.ints.copyOf()))

	override fun toString(): String = "Bitmap8($width, $height, palette=${palette.size})"

    companion object {
        inline operator fun invoke(width: Int, height: Int, palette: RgbaArray = RgbaArray(0x100), pixelProvider: (x: Int, y: Int) -> Byte): Bitmap8 {
            return Bitmap8(width, height, ByteArray(width * height) { pixelProvider(it % width, it / width) }, palette)
        }

        fun copyRect(
            src: Bitmap8,
            srcX: Int,
            srcY: Int,
            dst: Bitmap8,
            dstX: Int,
            dstY: Int,
            width: Int,
            height: Int
        ) = src.copy(srcX, srcY, dst, dstX, dstY, width, height)
    }
}

fun Bitmap.tryToExactBitmap8(): Bitmap8? {
    if (this is BitmapIndexed) {
        return Bitmap8(width, height, ByteArray(width * height) { this.getIntIndex(it).toByte() }, RgbaArray(this.palette.ints.copyOf(256)))
    }

    val bmp = this.toBMP32IfRequired().depremultipliedIfRequired()
    val bmpInts = bmp.ints
    val palette = Palette(RgbaArray(256))
    val colors = IntIntMap()
    var ncolor = 0
    for (color in bmpInts) {
         if (!colors.contains(color)) {
            palette.colors[ncolor] = if (bmp.premultiplied) RGBAPremultiplied(color).depremultiplied else RGBA(color)
            colors[color] = ncolor++
        }
        if (colors.size >= 256) return null
    }
    return Bitmap8(bmp.width, bmp.height, ByteArray(bmp.width * bmp.height) { colors[bmpInts[it]].toByte() }, palette.colors)
}

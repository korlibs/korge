package korlibs.image.color

import korlibs.image.bitmap.*
import korlibs.memory.*

interface ColorFormat {
    val bpp: Int

	fun getR(v: Int): Int
	fun getG(v: Int): Int
	fun getB(v: Int): Int
	fun getA(v: Int): Int
	fun pack(r: Int, g: Int, b: Int, a: Int): Int

	class Mixin(
        override val bpp: Int,
		val rOffset: Int, val rSize: Int,
		val gOffset: Int, val gSize: Int,
		val bOffset: Int, val bSize: Int,
		val aOffset: Int, val aSize: Int
	) : ColorFormat {
		override fun getR(v: Int): Int = v.extractScaledFF(rOffset, rSize)
		override fun getG(v: Int): Int = v.extractScaledFF(gOffset, gSize)
		override fun getB(v: Int): Int = v.extractScaledFF(bOffset, bSize)
		override fun getA(v: Int): Int = v.extractScaledFFDefault(aOffset, aSize, default = 0xFF)
		override fun pack(r: Int, g: Int, b: Int, a: Int): Int {
			return 0
				.insertScaledFF(r, rOffset, rSize)
				.insertScaledFF(g, gOffset, gSize)
				.insertScaledFF(b, bOffset, bSize)
				.insertScaledFF(a, aOffset, aSize)
		}
	}
}

interface ColorFormat16 : ColorFormat

abstract class ColorFormat24 : ColorFormat {
    override val bpp = 24
}

abstract class ColorFormat32 : ColorFormat {
    override val bpp = 32
}

fun ColorFormat.numberOfBytes(pixels: Int): Int = (pixels * bpp) / 8
val ColorFormat.bytesPerPixel: Double get() = bpp.toDouble() / 8

fun ColorFormat.toRGBA(v: Int): RGBA = RGBA(getR(v), getG(v), getB(v), getA(v))
fun ColorFormat.packRGBA(c: RGBA): Int = pack(c.r, c.g, c.b, c.a)

fun ColorFormat.getRf(v: Int): Float = getR(v).toFloat() / 255f
fun ColorFormat.getGf(v: Int): Float = getG(v).toFloat() / 255f
fun ColorFormat.getBf(v: Int): Float = getB(v).toFloat() / 255f
fun ColorFormat.getAf(v: Int): Float = getA(v).toFloat() / 255f

fun ColorFormat.getRd(v: Int): Double = getR(v).toDouble() / 255.0
fun ColorFormat.getGd(v: Int): Double = getG(v).toDouble() / 255.0
fun ColorFormat.getBd(v: Int): Double = getB(v).toDouble() / 255.0
fun ColorFormat.getAd(v: Int): Double = getA(v).toDouble() / 255.0

fun ColorFormat.unpackToRGBA(packed: Int): RGBA = RGBA(getR(packed), getG(packed), getB(packed), getA(packed))

fun ColorFormat.convertTo(color: Int, target: ColorFormat): Int = target.pack(
    this.getR(color), this.getG(color), this.getB(color), this.getA(color)
)

inline fun ColorFormat.decodeInternal(
    data: ByteArray,
    dataOffset: Int,
    out: RgbaArray,
    outOffset: Int,
    size: Int,
    read: (data: ByteArray, io: Int) -> Int
) {
    var io = dataOffset
    var oo = outOffset
    val bytesPerPixel = this.bytesPerPixel
    val outdata = out
    val Bpp = bytesPerPixel.toInt()

    for (n in 0 until size) {
        val c = read(data, io)
        io += Bpp
        outdata[oo++] = RGBA(getR(c), getG(c), getB(c), getA(c))
    }
}

fun ColorFormat.decode(
    data: ByteArray,
    dataOffset: Int,
    out: RgbaArray,
    outOffset: Int,
    size: Int,
    littleEndian: Boolean = true
) {
    val readFunc = when (bpp) {
        16 -> if (littleEndian) ByteArray::getU16LE else ByteArray::getU16BE
        24 -> if (littleEndian) ByteArray::getU24LE else ByteArray::getU24BE
        32 -> if (littleEndian) ByteArray::getS32LE else ByteArray::getS32BE
        else -> throw IllegalArgumentException("Unsupported bpp $bpp")
    }
    decodeInternal(data, dataOffset, out, outOffset, size, readFunc)
}

fun ColorFormat.decode(
    data: ByteArray,
    dataOffset: Int = 0,
    size: Int = (data.size / bytesPerPixel).toInt(),
    littleEndian: Boolean = true
): RgbaArray {
    val out = RgbaArray(size)
    decode(data, dataOffset, out, 0, size, littleEndian)
    return out
}

fun ColorFormat.decodeToBitmap32(
    width: Int,
    height: Int,
    data: ByteArray,
    dataOffset: Int = 0,
    littleEndian: Boolean = true
): Bitmap32 {
    return Bitmap32(width, height, decode(data, dataOffset, width * height, littleEndian))
}

fun ColorFormat.decodeToBitmap32(
    bmp: Bitmap32,
    data: ByteArray,
    dataOffset: Int = 0,
    littleEndian: Boolean = true
): Bitmap32 {
    val array = RgbaArray(bmp.ints)
    decode(data, dataOffset, array, 0, bmp.area)
    if (bmp.premultiplied) array.premultiplyInplace()
    return bmp
}

fun ColorFormat.encode(
    colors: RgbaArray,
    colorsOffset: Int,
    out: ByteArray,
    outOffset: Int,
    size: Int,
    littleEndian: Boolean = true
) {
    var io = colorsOffset
    var oo = outOffset
    val Bpp = bytesPerPixel.toInt()
    for (n in 0 until size) {
        val c = colors[io++]
        val ec = pack(c.r, c.g, c.b, c.a)
        when (bpp) {
            16 -> if (littleEndian) out.set16LE(oo, ec) else out.set16BE(oo, ec)
            24 -> if (littleEndian) out.set24LE(oo, ec) else out.set24BE(oo, ec)
            32 -> if (littleEndian) out.set32LE(oo, ec) else out.set32BE(oo, ec)
            else -> throw IllegalArgumentException("Unsupported bpp $bpp")
        }
        oo += Bpp
    }
}

fun ColorFormat.encode(
    colors: RgbaArray,
    colorsOffset: Int = 0,
    size: Int = colors.size,
    littleEndian: Boolean = true
): ByteArray {
    val out = ByteArray((size * bytesPerPixel).toInt())
    encode(colors, colorsOffset, out, 0, size, littleEndian)
    return out
}

fun ColorFormat16.encode(colors: IntArray, colorsOffset: Int, out: ShortArray, outOffset: Int, size: Int) {
    var io = colorsOffset
    var oo = outOffset
    for (n in 0 until size) {
        val c = colors[io++]
        out[oo++] = pack(RGBA.getR(c), RGBA.getG(c), RGBA.getB(c), RGBA.getA(c)).toShort()
    }
}

fun ColorFormat32.encode(colors: IntArray, colorsOffset: Int, out: IntArray, outOffset: Int, size: Int) {
    var io = colorsOffset
    var oo = outOffset
    for (n in 0 until size) {
        val c = colors[io++]
        out[oo++] = pack(RGBA.getR(c), RGBA.getG(c), RGBA.getB(c), RGBA.getA(c))
    }
}

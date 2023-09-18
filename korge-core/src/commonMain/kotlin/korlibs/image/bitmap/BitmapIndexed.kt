package korlibs.image.bitmap

import korlibs.image.color.RGBA
import korlibs.image.color.RgbaArray
import korlibs.math.*
import korlibs.memory.*
import kotlin.math.max

abstract class BitmapIndexed(
	bpp: Int,
	width: Int, height: Int,
	var bytes: ByteArray = ByteArray(width * height / (8 / bpp)),
	var palette: RgbaArray = RgbaArray(1 shl bpp)
) : Bitmap(width, height, bpp, false, bytes) {

    val data: ByteArray get() = bytes

	init {
		if (bytes.size < width * height / (8 / bpp)) throw RuntimeException("Bitmap data is too short: width=$width, height=$height, data=ByteArray(${data.size}), area=${width * height}")
	}

	override fun toString() = "BitmapIndexed(bpp=$bpp, width=$width, height=$height, clut=${palette.size})"

	protected val temp = ByteArray(max(width, height))

	val datau = UByteArrayInt(data)
    private val n8_dbpp: Int = 8 / bpp
    private val n8_dbppLog2 = ilog2(n8_dbpp)
    private val n8_dbppMask = (n8_dbpp - 1)

	inline operator fun get(x: Int, y: Int): Int = getInt(x, y)
	inline operator fun set(x: Int, y: Int, color: Int): Unit = setInt(x, y, color)

	override fun getInt(x: Int, y: Int): Int = getIntIndex(index(x, y))
	override fun setInt(x: Int, y: Int, color: Int) = setIntIndex(index(x, y), color)

    open fun getIntIndex(n: Int): Int {
        val iD = index_d(n)
        val iM = index_m(n)
        //println("[$n]: $iD, $iM :: bpp=$bpp, n8_dbpp=$n8_dbpp, n8_mask=$n8_dbpp")
        return datau[iD].extract(bpp * iM, bpp)
    }
    open fun setIntIndex(n: Int, color: Int) {
        val iD = index_d(n)
        val iM = index_m(n)
        //println("[$n]: $iD, $iM")
        datau[iD] = datau[iD].insert(color, bpp * iM, bpp)
    }

	override fun getRgbaRaw(x: Int, y: Int): RGBA = palette[this[x, y]]
	fun index_d(x: Int, y: Int) = index_d(index(x, y))
	fun index_m(x: Int, y: Int) = index_m(index(x, y))

    fun index_d(n: Int) = n ushr n8_dbppLog2
    fun index_m(n: Int) = n and n8_dbppMask

    fun setRow(y: Int, row: UByteArray) {
		arraycopy(row.asByteArray(), 0, data, index(0, y), stride)
	}

	fun setRow(y: Int, row: ByteArray) {
		arraycopy(row, 0, data, index(0, y), stride)
	}

	fun setWhitescalePalette() = this.apply {
		for (n in 0 until palette.size) {
			val col = ((n.toFloat() / palette.size.toFloat()) * 255).toInt()
			palette[n] = RGBA(col, col, col, 0xFF)
		}
		return this
	}

	override fun swapRows(y0: Int, y1: Int) {
		val s0 = index_d(0, y0)
		val s1 = index_d(0, y1)
		arraycopy(data, s0, temp, 0, stride)
		arraycopy(data, s1, data, s0, stride)
		arraycopy(temp, 0, data, s1, stride)
	}

	fun toLines(palette: String): List<String> {
		return (0 until height).map { y -> (0 until height).map { x -> palette[this[x, y]] }.joinToString("") }
	}

	fun setRowChunk(x: Int, y: Int, data: ByteArray, width: Int, increment: Int) {
		if (increment == 1) {
			arraycopy(data, 0, this.data, index(x, y), width / n8_dbpp)
		} else {
			var m = index(x, y)
			for (n in 0 until width / n8_dbpp) {
				this.data[m] = data[n]
				m += increment
			}
		}
	}

    fun computeMaxReferencedColors(): Int {
        var maxRefColor = -1
        for (n in 0 until area) maxRefColor = max(maxRefColor, getIntIndex(n))
        return maxRefColor + 1
    }

    override fun contentEquals(other: Bitmap): Boolean = (other is BitmapIndexed) && (this.width == other.width) && (this.height == other.height) && data.contentEquals(other.data)
    override fun contentHashCode(): Int = (width * 31 + height) + data.contentHashCode() + premultiplied.toInt()

    override fun toBMP32(): Bitmap32 = Bitmap32(width, height, premultiplied = premultiplied).also { outBmp ->
        val out = outBmp.ints
        val pal = this@BitmapIndexed.palette.ints
        for (n in 0 until area) out[n] = pal[getIntIndex(n)]
	}
}

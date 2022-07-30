package com.soywiz.korim.bitmap

import com.soywiz.kmem.arraycopy
import com.soywiz.kmem.clamp
import com.soywiz.kmem.toInt
import com.soywiz.korim.annotation.KorimInternal
import com.soywiz.korim.color.ColorFormat
import com.soywiz.korim.color.ColorTransform
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.color.RGBAPremultiplied
import com.soywiz.korim.color.RgbaArray
import com.soywiz.korim.color.RgbaPremultipliedArray
import com.soywiz.korim.color.YCbCr
import com.soywiz.korim.color.decode
import com.soywiz.korim.color.encode
import com.soywiz.korim.color.mix
import com.soywiz.korim.color.toRGBA
import com.soywiz.korim.color.toYCbCr
import com.soywiz.korim.vector.Bitmap32Context2d
import com.soywiz.korim.vector.Context2d
import com.soywiz.korma.geom.IRectangleInt
import com.soywiz.korma.geom.Matrix3D
import com.soywiz.korma.geom.RectangleInt
import com.soywiz.korma.geom.Vector3D
import com.soywiz.korma.geom.bottom
import com.soywiz.korma.geom.left
import com.soywiz.korma.geom.right
import com.soywiz.korma.geom.top
import kotlin.js.JsName
import kotlin.jvm.JvmName
import kotlin.jvm.JvmOverloads
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// @TODO: Create separate classes for premultiplied and non-premultiplied variants
@OptIn(KorimInternal::class)
class Bitmap32(
    width: Int,
    height: Int,
    val ints: IntArray = IntArray(width * height),
    premultiplied: Boolean
) : Bitmap(width, height, 32, premultiplied, ints), Iterable<RGBA> {
    @Deprecated("This is unsafe and might throw if you are accessing the wrong premultiplied version", level = DeprecationLevel.HIDDEN)
    val data = RgbaArray(ints)
        get() {
            if (premultiplied) {
                error("Trying to access data in a premultiplied bitmap")
            }
            return field
        }
    @Deprecated("This is unsafe and might throw if you are accessing the wrong premultiplied version", level = DeprecationLevel.HIDDEN)
    val dataPremult: RgbaPremultipliedArray = RgbaPremultipliedArray(ints)
        get() {
            if (!premultiplied) {
                error("Trying to access dataPremult in a non-premultiplied bitmap")
            }
            return field
        }

	init {
		if (ints.size < width * height) throw RuntimeException("Bitmap data is too short: width=$width, height=$height, data=ByteArray(${ints.size}), area=${width * height}")
	}

	private val temp = IntArray(max(width, height))
    val bounds: IRectangleInt = RectangleInt(0, 0, width, height)

    @Deprecated("Specify premultiplied instead")
    constructor(width: Int, height: Int) : this(width, height, premultiplied = false)
	constructor(width: Int, height: Int, value: RGBA) : this(width, height, premultiplied = false) { ints.fill(value.value) }
    constructor(width: Int, height: Int, value: RgbaArray) : this(width, height, value.ints, premultiplied = false)
	constructor(width: Int, height: Int, generator: (x: Int, y: Int) -> RGBA) : this(width, height, premultiplied = false) { setEach(callback = generator) }

    override fun createWithThisFormat(width: Int, height: Int): Bitmap = Bitmap32(width, height, premultiplied = premultiplied)

    fun copyTo(other: Bitmap32): Bitmap32 = checkMatchDimensions(other).also { arraycopy(this.ints, 0, other.ints, 0, this.ints.size) }

    override fun copyUnchecked(srcX: Int, srcY: Int, dst: Bitmap, dstX: Int, dstY: Int, width: Int, height: Int) {
        if (dst !is Bitmap32) return super.copyUnchecked(srcX, srcY, dst, dstX, dstY, width, height)
        val src = this
        val srcArray = src.ints
        val dstArray = dst.ints
        for (y in 0 until height) {
            arraycopy(srcArray, src.index(srcX, srcY + y), dstArray, dst.index(dstX, dstY + y), width)
        }
    }

    operator fun set(x: Int, y: Int, color: RGBA) { setRgba(x, y, color) }
	operator fun get(x: Int, y: Int): RGBA = getRgba(x, y)

	override fun setInt(x: Int, y: Int, color: Int) { ints[index(x, y)] = color }
	override fun getInt(x: Int, y: Int): Int = ints[index(x, y)]

    override fun getRgbaRaw(x: Int, y: Int): RGBA = RGBA(getInt(x, y))
	override fun setRgbaRaw(x: Int, y: Int, v: RGBA) = setInt(x, y, v.value)

    override fun setRgba(x: Int, y: Int, v: RGBA): Unit = setRgbaAtIndex(index(x, y), v)
    override fun setRgba(x: Int, y: Int, v: RGBAPremultiplied): Unit = setRgbaPremultipliedAtIndex(index(x, y), v)

    override fun getRgba(x: Int, y: Int): RGBA = getRgbaAtIndex(index(x, y))
    override fun getRgbaPremultiplied(x: Int, y: Int): RGBAPremultiplied = getRgbaPremultipliedAtIndex(index(x, y))

    fun setRgbaAtIndex(n: Int, color: RGBA) {
        this.ints[n] = if (premultiplied) color.premultiplied.value else color.value
    }

    fun setRgbaPremultipliedAtIndex(n: Int, color: RGBAPremultiplied) {
        this.ints[n] = if (premultiplied) color.value else color.depremultiplied.value
    }

    fun getRgbaAtIndex(n: Int): RGBA = if (premultiplied) RGBAPremultiplied(this.ints[n]).depremultiplied else RGBA(this.ints[n])
    fun getRgbaPremultipliedAtIndex(n: Int): RGBAPremultiplied = if (premultiplied) RGBAPremultiplied(this.ints[n]) else RGBA(this.ints[n]).premultiplied

    fun setRow(y: Int, row: IntArray) {
		arraycopy(row, 0, ints, index(0, y), width)
	}

    @KorimInternal
    fun _drawUnchecked(src: Bitmap32, dx: Int, dy: Int, sleft: Int, stop: Int, sright: Int, sbottom: Int, mix: Boolean) {
        val dst = this
        val width = sright - sleft
        val height = sbottom - stop


        for (y in 0 until height) {
            val dstOffset = dst.index(dx, dy + y)
            val srcOffset = src.index(sleft, stop + y)
            if (mix) {
                for (x in 0 until width) dst.setRgbaPremultipliedAtIndex(dstOffset + x, dst.getRgbaPremultipliedAtIndex(dstOffset + x) mix src.getRgbaPremultipliedAtIndex(srcOffset + x))
            } else {
                when {
                    dst.premultiplied == src.premultiplied -> arraycopy(src.ints, srcOffset, dst.ints, dstOffset, width)
                    !dst.premultiplied -> for (x in 0 until width) dst.setRgbaAtIndex(dstOffset + x, src.getRgbaAtIndex(srcOffset + x))
                    else -> for (x in 0 until width) dst.setRgbaPremultipliedAtIndex(dstOffset + x, src.getRgbaPremultipliedAtIndex(srcOffset + x))
                }
            }
        }
    }

    @KorimInternal
	fun _draw(src: Bitmap32, dx: Int, dy: Int, sleft: Int, stop: Int, sright: Int, sbottom: Int, mix: Boolean) {
        var sleft = sleft
        var stop = stop
        var dx = dx
        var dy = dy
        if (dx < 0) {
            sleft -= dx
            dx = 0
        }
        if (dy < 0) {
            stop -= dy
            dy = 0
        }
        val availableWidth = width - dx
        val availableHeight = height - dy
        val awidth = min(availableWidth, sright - sleft)
        val aheight = min(availableHeight, sbottom - stop)
        _drawUnchecked(src, dx, dy, sleft, stop, sleft + awidth, stop + aheight, mix)
	}

	fun drawPixelMixed(x: Int, y: Int, c: RGBA) {
		this[x, y] = RGBA.mix(this[x, y], c)
	}

    @KorimInternal
	fun _drawPut(mix: Boolean, other: Bitmap32, _dx: Int = 0, _dy: Int = 0) {
		_draw(other, _dx, _dy, 0, 0, other.width, other.height, mix)
	}

    fun historiogram(channel: BitmapChannel, out: IntArray = IntArray(256)): IntArray {
        check(out.size >= 256) { "output array size must be 256" }
        out.fill(0)
        forEach { n, _, _ -> out[channel.extract(RGBA(ints[n]))]++ }
        return out
    }

	fun fill(color: RGBA, x: Int = 0, y: Int = 0, width: Int = this.width - x, height: Int = this.height - y) {
		val x1 = clampX(x)
		val x2 = clampX(x + width - 1)
		val y1 = clampY(y)
		val y2 = clampY(y + height - 1)
        val colorInt = color.premultipliedValue(premultiplied)
		for (cy in y1..y2) this.ints.fill(colorInt, index(x1, cy), index(x2, cy) + 1)
	}

    @KorimInternal
	fun _draw(src: BitmapSlice<Bitmap32>, dx: Int = 0, dy: Int = 0, mix: Boolean) {
		val b = src.bounds
		_draw(src.bmp, dx, dy, b.left, b.top, b.right, b.bottom, mix = mix)
	}

    fun put(src: Bitmap32, dx: Int = 0, dy: Int = 0) = _drawPut(false, src, dx, dy)
	fun draw(src: Bitmap32, dx: Int = 0, dy: Int = 0) = _drawPut(true, src, dx, dy)

	fun put(src: BitmapSlice<Bitmap32>, dx: Int = 0, dy: Int = 0) = _draw(src, dx, dy, mix = false)
	fun draw(src: BitmapSlice<Bitmap32>, dx: Int = 0, dy: Int = 0) = _draw(src, dx, dy, mix = true)

	fun drawUnoptimized(src: BitmapSlice<Bitmap>, dx: Int = 0, dy: Int = 0, mix: Boolean = true) {
		if (src.bmpBase is Bitmap32) {
			_draw(src as BitmapSlice<Bitmap32>, dx, dy, mix = mix)
		} else {
			drawUnoptimized(src.bmpBase, dx, dy, src.left, src.top, src.right, src.bottom, mix = mix)
		}
	}

	fun drawUnoptimized(src: Bitmap, dx: Int, dy: Int, sleft: Int, stop: Int, sright: Int, sbottom: Int, mix: Boolean) {
		val dst = this
		val width = sright - sleft
		val height = sbottom - stop
		for (y in 0 until height) {
			val dstOffset = dst.index(dx, dy + y)
			if (mix) {
				for (x in 0 until width) {
                    dst.setRgbaPremultipliedAtIndex(dstOffset + x, dst.getRgbaPremultipliedAtIndex(dstOffset + x) mix src.getRgbaPremultiplied(sleft + x, stop + y))
                }
			} else {
				for (x in 0 until width) {
                    dst.setRgbaAtIndex(dstOffset + x,src.getRgba(sleft + x, stop + y))
                }
			}
		}
	}

	fun copySliceWithBounds(left: Int, top: Int, right: Int, bottom: Int): Bitmap32 =
		copySliceWithSize(left, top, right - left, bottom - top)

	fun copySliceWithSize(x: Int, y: Int, width: Int, height: Int): Bitmap32 = Bitmap32(width, height, premultiplied).also { out ->
        for (yy in 0 until height) {
            arraycopy(this.ints, this.index(x, y + yy), out.ints, out.index(0, yy), width)
        }
    }

    inline fun any(callback: (RGBA) -> Boolean): Boolean = (0 until area).any { callback(getRgbaAtIndex(it)) }
	inline fun all(callback: (RGBA) -> Boolean): Boolean = (0 until area).all { callback(getRgbaAtIndex(it)) }

	inline fun setEach(sx: Int = 0, sy: Int = 0, width: Int = this.width - sx, height: Int = this.height - sy, callback: (x: Int, y: Int) -> RGBA) = forEach(sx, sy, width, height) { n, x, y -> setRgbaAtIndex(n, callback(x, y)) }
    inline fun setEachPremultiplied(sx: Int = 0, sy: Int = 0, width: Int = this.width - sx, height: Int = this.height - sy, callback: (x: Int, y: Int) -> RGBAPremultiplied) = forEach(sx, sy, width, height) { n, x, y -> setRgbaPremultipliedAtIndex(n, callback(x, y)) }
	inline fun updateColors(sx: Int = 0, sy: Int = 0, width: Int = this.width - sx, height: Int = this.height - sy, callback: (rgba: RGBA) -> RGBA) = forEach(sx, sy, width, height) { n, x, y -> setRgbaAtIndex(n, callback(getRgbaAtIndex(n))) }
    inline fun updateColorsXY(sx: Int = 0, sy: Int = 0, width: Int = this.width - sx, height: Int = this.height - sy, callback: (x: Int, y: Int, rgba: RGBA) -> RGBA) = forEach(sx, sy, width, height) { n, x, y -> this.setRgbaAtIndex(n, callback(x, y, getRgbaAtIndex(n))) }

	fun writeChannel(destination: BitmapChannel, input: Bitmap32, source: BitmapChannel) = Bitmap32.copyChannel(input, source, this, destination)
	fun writeChannel(destination: BitmapChannel, input: Bitmap8) = Bitmap32.copyChannel(input, this, destination)
	fun extractChannel(channel: BitmapChannel, out: Bitmap8 = Bitmap8(width, height)): Bitmap8 = out.also { Bitmap32.copyChannel(this, channel, it) }

    fun inverted(target: Bitmap32 = Bitmap32(width, height, this.premultiplied)): Bitmap32 = copyTo(target).apply { invert() }
    fun xored(value: RGBA, target: Bitmap32 = Bitmap32(width, height, this.premultiplied)) = copyTo(target).apply { xor(value) }

    fun invert() = xor(RGBA(255, 255, 255, 0))
	fun xor(value: RGBA) = updateColors { RGBA(it.value xor value.value) }

	override fun toString(): String = "Bitmap32($width, $height)"

	override fun swapRows(y0: Int, y1: Int) {
		val s0 = index(0, y0)
		val s1 = index(0, y1)
		arraycopy(ints, s0, temp, 0, width)
		arraycopy(ints, s1, ints, s0, width)
		arraycopy(temp, 0, ints, s1, width)
	}

	fun writeDecoded(color: ColorFormat, data: ByteArray, offset: Int = 0, littleEndian: Boolean = true): Bitmap32 =
		this.apply {
			color.decode(data, offset, RgbaArray(this.ints), 0, this.area, littleEndian = littleEndian)
		}

    override fun clone(): Bitmap32 = Bitmap32(width, height, this.ints.copyOf(), premultiplied)

	override fun getContext2d(antialiasing: Boolean): Context2d = Context2d(Bitmap32Context2d(this, antialiasing))

	fun premultipliedIfRequired(): Bitmap32 = if (this.premultiplied) this else premultiplied()
	fun depremultipliedIfRequired(): Bitmap32 = if (!this.premultiplied) this else depremultiplied()

    @JsName("copyPremultiplied")
	fun premultiplied(): Bitmap32 = this.clone().apply { premultiplyInplaceIfRequired() }
	fun depremultiplied(): Bitmap32 = this.clone().apply { depremultiplyInplaceIfRequired() }

	fun premultiplyInplaceIfRequired() {
		if (premultiplied) return
		premultiplied = true
        for (n in 0 until area) ints[n] = RGBA(ints[n]).premultiplied.value
	}

	fun depremultiplyInplaceIfRequired() {
		if (!premultiplied) return
		premultiplied = false
        for (n in 0 until area) ints[n] = RGBAPremultiplied(ints[n]).depremultiplied.value
	}

	fun withColorTransform(ct: ColorTransform, x: Int = 0, y: Int = 0, width: Int = this.width - x, height: Int = this.height - y): Bitmap32
        = extract(x, y, width, height).apply { applyColorTransform(ct) }

	fun applyColorTransform(ct: ColorTransform, x: Int = 0, y: Int = 0, width: Int = this.width - x, height: Int = this.height - y) {
		val R = IntArray(256) { ((it * ct.mR) + ct.aR).toInt().clamp(0x00, 0xFF) }
		val G = IntArray(256) { ((it * ct.mG) + ct.aG).toInt().clamp(0x00, 0xFF) }
		val B = IntArray(256) { ((it * ct.mB) + ct.aB).toInt().clamp(0x00, 0xFF) }
		val A = IntArray(256) { ((it * ct.mA) + ct.aA).toInt().clamp(0x00, 0xFF) }
        updateColors(x, y, width, height) { RGBA(R[it.r], G[it.g], B[it.b], A[it.a]) }
	}

    fun applyColorMatrix(matrix: Matrix3D, x: Int = 0, y: Int = 0, width: Int = this.width - x, height: Int = this.height - y, temp: Vector3D = Vector3D()) {
        val v = temp
        updateColors(x, y, width, height) {
            v.setTo(it.rf, it.gf, it.bf, it.af)
            matrix.transform(v, v)
            RGBA.float(v.x, v.y, v.z, v.w)
        }
    }

    fun mipmap(levels: Int): Bitmap32 {
		val temp = this.clone()
		temp.premultiplyInplaceIfRequired()
		val dst = RgbaPremultipliedArray(temp.ints)

		var twidth = width
		var theight = height

		for (level in 0 until levels) {
			twidth /= 2
			theight /= 2
			for (y in 0 until theight) {
				var n = temp.index(0, y)
				var m = temp.index(0, y * 2)

				for (x in 0 until twidth) {
                    val c1 = dst[m + 0]
                    val c2 = dst[m + 1]
                    val c3 = dst[m + width + 0]
                    val c4 = dst[m + width + 1]
					dst[n] = RGBAPremultiplied.blend(c1, c2, c3, c4)
					m += 2
					n++
				}
			}
		}
        return temp.copySliceWithSize(0, 0, twidth, theight)
	}

	override fun iterator(): Iterator<RGBA> = iterator {
        for (n in 0 until area) yield(getRgbaAtIndex(n))
    }

	fun setRowChunk(x: Int, y: Int, data: RgbaArray, width: Int, increment: Int) {
		if (increment == 1) {
			arraycopy(data.ints, 0, this.ints, index(x, y), width)
		} else {
			var m = index(x, y)
			for (n in 0 until width) {
				this.ints[m] = data.ints[n]
				m += increment
			}
		}
	}

	fun extractBytes(format: ColorFormat = RGBA): ByteArray = format.encode(RgbaArray(ints))

    //fun scroll(sx: Int, sy: Int) {
    //    scrollX(sx)
    //    scrollY(sy)
    //}
    //
    //private fun scrollX(sx: Int) {
    //    val displacement = sx umod width
    //    if (displacement == 0) return
    //    for (y in 0 until height) {
    //        arraycopy(this.data.ints, index(0, y), temp.ints, 0, width)
    //        arraycopy(temp.ints, 0, this.data.ints, index(0, y), displacement)
    //        arraycopy(temp.ints, displacement, this.data.ints, index(displacement, y), width - displacement)
    //    }
    //}
    //
    //private fun scrollY(sy: Int) {
    //    arraycopy(this.data.ints, 0, temp.ints, 0, width)
    //    for (y in 0 until height - 1) {
    //
    //    }
    //}

    fun scaleNearest(sx: Int, sy: Int): Bitmap32 = scaled(width * sx, height * sy, smooth = false)
    fun scaleLinear(sx: Double, sy: Double): Bitmap32 = scaled((width * sx).toInt(), (height * sy).toInt(), smooth = true)

    /**
     * Creates a new [Bitmap32] with the specified new dimensions [width]x[height]
     * scaling the original content.
     * The [smooth] parameter determines the quality of the interpolation. [smooth]=false will use a nearest neighborhood implementation.
     */
    @JvmOverloads
    fun scaled(width: Int, height: Int, smooth: Boolean = true): Bitmap32 {
        val sx = width.toDouble() / this.width.toDouble()
        val sy = height.toDouble() / this.height.toDouble()
        val isx = 1.0 / sx
        val isy = 1.0 / sy
        val out = Bitmap32(width, height, this.premultiplied)
        if (smooth) {
            out.setEach { x, y -> this@Bitmap32[(x * isx).toInt(), (y * isy).toInt()] }
        } else {
            val gWidth = width > (this.width / 2 + 1)
            val gHeight = height > (this.height / 2 + 1)
            //println("gWidth: $gWidth, gHeight=$gHeight")
            //println("width=$width, height=$height")
            //println("this.width=${this.width}, this.height=${this.height}")
            // @TODO: Reduce memory usage here
            if (gWidth || gHeight) {
                return scaled(
                    if (gWidth) this.width / 2 else width,
                    if (gHeight) this.height / 2 else height,
                    smooth = true
                ).scaled(width, height)
            }
            out.setEach { x, y -> this@Bitmap32.getRgbaSampled(x * isx, y * isy) }
        }
        return out
    }

	fun rgbaToYCbCr(): Bitmap32 = clone().apply { rgbaToYCbCrInline() }
    fun rgbaToYCbCrInline() = updateColors { RGBA(it.toYCbCr().value) }

	fun yCbCrToRgba(): Bitmap32 = clone().apply { yCbCrToRgbaInline() }
    fun yCbCrToRgbaInline() = updateColors { YCbCr(it.value).toRGBA() }

    fun contentEquals(other: Any?): Boolean = (other is Bitmap32) && (this.width == other.width) && (this.height == other.height) && ints.contentEquals(other.ints)
    fun contentHashCode(): Int = (width * 31 + height) + ints.contentHashCode() + premultiplied.toInt()

    // @TODO: Can't do this or won't be able to put Bitmaps on hashmaps
    //override fun equals(other: Any?): Boolean = (other is Bitmap32) && (this.width == other.width) && (this.height == other.height) && data.ints.contentEquals(other.data.ints)
    //override fun hashCode(): Int = (width * 31 + height) + data.ints.contentHashCode() + premultiplied.toInt()

    companion object {
        val EMPTY = Bitmap32(0, 0, premultiplied = true)

        operator fun invoke(width: Int, height: Int, value: RgbaPremultipliedArray): Bitmap32 = Bitmap32(width, height, value.ints, premultiplied = false)
        operator fun invoke(width: Int, height: Int, value: RGBAPremultiplied): Bitmap32 = Bitmap32(width, height, premultiplied = true).apply { ints.fill(value.value) }

        operator fun invoke(width: Int, height: Int, premultiplied: Boolean) = Bitmap32(width, height, IntArray(width * height), premultiplied)
        @JvmName("invokeRGBA")
        operator fun invoke(width: Int, height: Int, generator: (x: Int, y: Int) -> RGBA): Bitmap32 = Bitmap32(width, height, IntArray(width * height).also {
            var n = 0; for (y in 0 until height) for (x in 0 until width) it[n++] = generator(x, y).value
        }, premultiplied = false)
        @JvmName("invokeRGBAPremultiplied")
        operator fun invoke(width: Int, height: Int, generator: (x: Int, y: Int) -> RGBAPremultiplied): Bitmap32 = Bitmap32(width, height, IntArray(width * height).also {
            var n = 0; for (y in 0 until height) for (x in 0 until width) it[n++] = generator(x, y).value
        }, premultiplied = true)

        fun copyChannel(
            src: Bitmap32,
            srcChannel: BitmapChannel,
            dst: Bitmap32,
            dstChannel: BitmapChannel
        ) {
            val srcShift = srcChannel.shift
            val dstShift = dstChannel.shift
            val dstClear = dstChannel.clearMask
            val dstData = dst.ints
            val srcData = src.ints
            for (n in 0 until dst.area) {
                val c = (srcData[n] ushr srcShift) and 0xFF
                dstData[n] = ((dstData[n] and dstClear) or (c shl dstShift))
            }

        }

        fun copyChannel(
            src: Bitmap8,
            dst: Bitmap32,
            dstChannel: BitmapChannel
        ) {
            val destShift = dstChannel.index * 8
            val destClear = (0xFF shl destShift).inv()
            for (n in 0 until dst.area) {
                val c = src.data[n].toInt() and 0xFF
                dst.ints[n] = ((dst.ints[n] and destClear) or (c shl destShift))
            }
        }

        fun copyChannel(
            src: Bitmap32,
            srcChannel: BitmapChannel,
            dst: Bitmap8
        ) {
            val shift = srcChannel.shift
            for (n in 0 until src.area) {
                dst.data[n] = ((src.ints[n] ushr shift) and 0xFF).toByte()
            }
        }

        fun copyRect(
            src: Bitmap32,
            srcX: Int,
            srcY: Int,
            dst: Bitmap32,
            dstX: Int,
            dstY: Int,
            width: Int,
            height: Int
        ) = src.copy(srcX, srcY, dst, dstX, dstY, width, height)

        fun createWithAlpha(
            color: Bitmap32,
            alpha: Bitmap32,
            alphaChannel: BitmapChannel = BitmapChannel.RED
        ): Bitmap32 = Bitmap32(color.width, color.height, color.premultiplied).also { out ->
            out.put(color)
            Bitmap32.copyChannel(alpha, BitmapChannel.RED, out, BitmapChannel.ALPHA)
        }

        // https://en.wikipedia.org/wiki/Structural_similarity
        suspend fun matchesSSMI(a: Bitmap, b: Bitmap): Boolean = TODO()

        suspend fun matches(a: Bitmap, b: Bitmap, threshold: Int = 32): Boolean {
            val diff = diff(a, b)
            //for (c in diff.data) println("%02X, %02X, %02X".format(RGBA.getR(c), RGBA.getG(c), RGBA.getB(c)))
            return RgbaArray(diff.ints).all {
                (it.r < threshold) && (it.g < threshold) && (it.b < threshold) && (it.a < threshold)
            }
        }

        data class MatchResult(
            val sizeMatches: Boolean,
            val differentPixels: Int = 0,
            val samePixels: Int = 0
        )

        fun matchesWithResult(a: Bitmap32, b: Bitmap32): MatchResult {
            if (a.width != b.width || a.height != b.height) return MatchResult(sizeMatches = false)
            var different = 0
            var same = 0
            for (n in 0 until a.area) {
                val av = a.getRgbaAtIndex(n)
                val bv = b.getRgbaAtIndex(n)
                if (av == bv || (av.a == 0 && bv.a == 0)) {
                    same++
                } else {
                    different++
                }
            }
            return MatchResult(sizeMatches = true, differentPixels = different, samePixels = same)
        }

        fun diff(a: Bitmap, b: Bitmap): Bitmap32 {
            if (a.width != b.width || a.height != b.height) throw IllegalArgumentException("$a not matches $b size")
            val a32 = a.toBMP32()
            val b32 = b.toBMP32()
            val out = Bitmap32(a.width, a.height, premultiplied = true)
            //showImageAndWait(a32)
            //showImageAndWait(b32)
            for (n in 0 until out.area) {
                val c1 = a32.getRgbaPremultipliedAtIndex(n)
                val c2 = b32.getRgbaPremultipliedAtIndex(n)

                //println("%02X, %02X, %02X".format(RGBA.getR(c1), RGBA.getR(c2), dr))
                out.setRgbaPremultipliedAtIndex(n, RGBAPremultiplied(abs(c1.r - c2.r), abs(c1.g - c2.g), abs(c1.b - c2.b), abs(c1.a - c2.a)))

                //println("$dr, $dg, $db, $da : ${out.data[n]}")
            }
            //showImageAndWait(out)
            return out
        }
    }

    override fun toBMP32(): Bitmap32 = this
}


fun BitmapSlice<Bitmap32>.isFullyTransparent(): Boolean {
    val bmp = this.bmp
    val data = RgbaArray(this.bmp.ints)
    val width = right - left
    for (y in top until bottom) {
        val index = bmp.index(left, y)
        for (n in 0 until width) if (data[index + n].a != 0) return false
    }
    return true
}

fun Bitmap32.expandBorder(area: IRectangleInt, border: Int) {
    val data = this.ints
    var x0Index = index(area.left, area.top)
    var x1Index = index(area.right - 1, area.top)
    for (n in 0 until area.height) {
        val x0Color = data[x0Index]
        val x1Color = data[x1Index]
        for (m in 0 until border) {
            data[x0Index - m - 1] = x0Color
            data[x1Index + m + 1] = x1Color
        }
        x0Index += width
        x1Index += width
    }
    for (m in 0 until border) {
        val x = area.left - border
        val npixels = area.width + border * 2
        arraycopy(data, index(x, area.top), data, index(x, area.top - m - 1), npixels)
        arraycopy(data, index(x, area.bottom - 1), data, index(x, area.bottom + m), npixels)
    }
}

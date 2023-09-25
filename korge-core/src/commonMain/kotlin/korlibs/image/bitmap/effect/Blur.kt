@file:OptIn(ExperimentalUnsignedTypes::class)

package korlibs.image.bitmap.effect

import korlibs.image.bitmap.*
import korlibs.math.*
import korlibs.memory.*
import kotlin.math.*

fun Bitmap32.blur(r: Int): Bitmap32 {
    val out = Bitmap32(width + r * 2, height + r * 2, premultiplied = this.premultiplied)
    Bitmap32.copyRect(this, 0, 0, out, r, r, width, height)
    out.premultiplyInplaceIfRequired()
    out.blurInplace(r)
    if (!this.premultiplied) {
        out.depremultiplyInplaceIfRequired()
    }
    return out
}

fun Bitmap8.blur(r: Int): Bitmap8 {
    val out = Bitmap8(width + r * 2, height + r * 2)
    Bitmap8.copyRect(this, 0, 0, out, r, r, width, height)
    out.blurInplace(r)
    return out
}

fun Bitmap32.blurInplace(r: Int) {
    val r = r.coerceAtMost(width / 2 - 1).coerceAtMost(height / 2 - 1)
    val t1 = Bitmap8(width, height)
    val t2 = Bitmap8(width, height)
    for (n in 0 until 4) {
        val channel = BitmapChannel[n]
        extractChannel(channel, t1)
        arraycopy(t1.data, 0, t2.data, 0, t2.area)
        gaussBlur(t1.data, t2.data, width, height, r)
        writeChannel(channel, t2)
    }
}

fun Bitmap8.blurInplace(r: Int) {
    val r = r.coerceAtMost(width / 2 - 1).coerceAtMost(height / 2 - 1)
    val t2 = Bitmap8(width, height)
    arraycopy(this.data, 0, t2.data, 0, this.area)
    gaussBlur(t2.data, this.data, width, height, r)
}

// http://blog.ivank.net/fastest-gaussian-blur.html

private fun gaussBlur(scl: ByteArray, tcl: ByteArray, w: Int, h: Int, r: Int) {
    var a = 0
    var b = 0
    var c = 0
    boxesForGaussN3(r) { v0, v1, v2 ->
        a = v0
        b = v1
        c = v2
    }
    boxBlur(scl, tcl, w, h, (a - 1) / 2)
    boxBlur(tcl, scl, w, h, (b - 1) / 2)
    boxBlur(scl, tcl, w, h, (c - 1) / 2)
}

private fun boxBlur(scl: ByteArray, tcl: ByteArray, w: Int, h: Int, r: Int) {
    arraycopy(scl, 0, tcl, 0, scl.size)
    boxBlurH(tcl, scl, w, h, r)
    boxBlurT(scl, tcl, w, h, r)
}

private fun boxBlurH(scl: ByteArray, tcl: ByteArray, w: Int, h: Int, r: Int) {
    val arr = (r + r + 1)
    for (i in 0 until h) {
        var ti = i * w
        var li = ti
        var ri = ti + r
        val fv = scl.getU8(ti)
        val lv = scl.getU8(ti + w - 1)
        var v = (r + 1) * fv
        for (j in 0 until r) v += scl.getU8(ti + j)
        for (j in 0..r) {
            v += scl.getU8(ri++) - fv
            tcl[ti++] = (v / arr).toByte()
        }
        for (j in r + 1 until w - r) {
            v += scl.getU8(ri++) - scl.getU8(li++)
            tcl[ti++] = (v / arr).toByte()
        }
        for (j in w - r until w) {
            v += lv - scl.getU8(li++)
            tcl[ti++] = (v / arr).toByte()
        }
    }
}

private fun boxBlurT(scl: ByteArray, tcl: ByteArray, w: Int, h: Int, r: Int) {
    val arr = (r + r + 1)
    for (i in 0 until w) {
        var ti = i
        var li = ti
        var ri = ti + r * w
        val fv = scl.getU8(ti)
        val lv = scl.getU8(ti + w * (h - 1))
        var v = (r + 1) * fv
        for (j in 0 until r) v += scl.getU8(ti + j * w)
        for (j in 0..r) {
            v += scl.getU8(ri) - fv
            tcl[ti] = (v / arr).toByte()
            ri += w
            ti += w
        }
        for (j in r + 1 until h - r) {
            v += scl.getU8(ri) - scl.getU8(li)
            tcl[ti] = (v / arr).toByte()
            li += w
            ri += w
            ti += w
        }
        for (j in h - r until h) {
            v += lv - scl.getU8(li)
            tcl[ti] = (v / arr).toByte()
            li += w
            ti += w
        }
    }
}

private inline fun boxesForGaussN3(sigma: Int, emit: (a: Int, b: Int, c: Int) -> Unit) { // standard deviation, number of boxes
    val n = 3
    val wIdeal = sqrt((12.0 * sigma * sigma / n) + 1)  // Ideal averaging filter width
    var wl = wIdeal.toIntFloor()
    if (wl % 2 == 0) wl--
    val wu = wl + 2

    val mIdeal = (12.0 * sigma * sigma - n * wl * wl - 4 * n * wl - 3 * n) / (-4 * wl - 4)
    val m = round(mIdeal)
    // var sigmaActual = Math.sqrt( (m*wl*wl + (n-m)*wu*wu - n)/12 );

    emit(
        if (0 < m) wl else wu,
        if (1 < m) wl else wu,
        if (2 < m) wl else wu
    )
}

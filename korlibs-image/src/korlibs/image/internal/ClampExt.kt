package korlibs.image.internal

// a.rgba + (b.rgba * mul) // mul = [0, 256]
internal fun sumPacked4MulR(a: Int, b: Int, mul: Int): Int {
    val dstRB = (((b and 0x00FF00FF) * mul)) ushr 8
    val dstGA = ((((b ushr 8) and 0x00FF00FF) * mul)) ushr 8
    val r = ((a ushr 0) and 0xFF) + ((dstRB ushr 0) and 0xFF)
    val g = ((a ushr 8) and 0xFF) + ((dstGA ushr 0) and 0xFF)
    val b = ((a ushr 16) and 0xFF) + ((dstRB ushr 16) and 0xFF)
    val a = ((a ushr 24) and 0xFF) + ((dstGA ushr 16) and 0xFF)
    return packIntClamped(r, g, b, a)
}

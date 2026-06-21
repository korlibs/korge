package korlibs.render.osx.metal

import com.sun.jna.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.math.geom.*

fun MTLTexture.readBitmap(rect: RectangleInt = RectangleInt(0, 0, width.toInt(), height.toInt())): Bitmap32 {
    val texWidth = width
    val texHeight = height
    val dataOut = Memory((rect.width * rect.height * 4).toLong())
    getBytes(
        dataOut, (texWidth * 4), (texWidth * texHeight * 4),
        rect.toMTLRegion(),
        0L, 0L
    )
    return Bitmap32(rect.width, rect.height, RgbaArray(IntArray(rect.width * rect.height) { dataOut.getInt((4 * it).toLong()) }))
}

fun RectangleInt.toMTLRegion(): MTLRegion.ByValue = MTLRegion.make2D(x.toLong(), y.toLong(), width.toLong(), height.toLong())

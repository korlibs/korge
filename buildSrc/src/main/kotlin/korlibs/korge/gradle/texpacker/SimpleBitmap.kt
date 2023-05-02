package korlibs.korge.gradle.texpacker

import java.awt.*
import java.awt.image.*
import java.io.*
import javax.imageio.*
import kotlin.math.*

inline class SimpleRGBA(val data: Int) {
    val r: Int get() = (data ushr 0) and 0xFF
    val g: Int get() = (data ushr 8) and 0xFF
    val b: Int get() = (data ushr 16) and 0xFF
    val a: Int get() = (data ushr 24) and 0xFF
}

class SimpleBitmap(val width: Int, val height: Int, val data: IntArray = IntArray(width * height)) {
    override fun toString(): String = "SimpleBitmap($width, $height)"

    companion object {
        operator fun invoke(image: BufferedImage): SimpleBitmap {
            val width = image.width
            val height = image.height
            val out = IntArray(width * height)
            image.getRGB(0, 0, width, height, out, 0, width)
            return SimpleBitmap(width, height, out)
        }
        operator fun invoke(file: File): SimpleBitmap {
            return invoke(ImageIO.read(file))
        }
    }
    private fun index(x: Int, y: Int): Int = y * width + x
    operator fun get(x: Int, y: Int): SimpleRGBA = SimpleRGBA(data[index(x, y)])
    operator fun set(x: Int, y: Int, value: SimpleRGBA) { data[index(x, y)] = value.data }

    fun toBufferedImage(): BufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also {
        it.setRGB(0, 0, width, height, this.data, 0, width)
    }

    fun writeTo(file: File) {
        ImageIO.write(toBufferedImage(), file.extension, file)
    }

    fun put(px: Int, py: Int, other: SimpleBitmap) {
        //println("put $other in $this at [$px, $py]")
        for (y in 0 until other.height) {
            System.arraycopy(other.data, other.index(0, y), this.data, this.index(px, py + y), other.width)
        }
    }

    fun slice(rect: Rectangle): SimpleBitmap {
        val out = SimpleBitmap(rect.width, rect.height)
        for (y in 0 until rect.height) {
            System.arraycopy(this.data, this.index(rect.x, rect.y + y), out.data, out.index(0, y), rect.width)
        }
        return out
    }

    fun trim(): Rectangle {
        var minLeft = width
        var minRight = width
        var minTop = height
        var minBottom = height
        for (y in 0 until height) {
            for (x in 0 until width) if (this[x, y].a != 0) { minLeft = min(minLeft, x); break }
            for (x in 0 until width) if (this[width - x - 1, y].a != 0) { minRight = min(minRight, x); break }
        }
        for (x in 0 until width) {
            for (y in 0 until height) if (this[x, y].a != 0) { minTop = min(minTop, y); break }
            for (y in 0 until height) if (this[x, height - y - 1].a != 0) { minBottom = min(minBottom, y); break }
        }
        if (minLeft == width || minTop == height) {
            return Rectangle(0, 0, 0, 0)
        }
        return Rectangle(minLeft, minTop, width - minRight - minLeft, height - minBottom - minTop)
    }

    fun transferRect(x: Int, y: Int, width: Int, height: Int, out: IntArray, write: Boolean) {
        for (n in 0 until height) {
            val tindex = this.index(x, y + n)
            val oindex = width * n
            when {
                write -> System.arraycopy(out, oindex, this.data, tindex, width)
                else -> System.arraycopy(this.data, tindex, out, oindex, width)
            }
        }
    }
    fun getRect(x: Int, y: Int, width: Int, height: Int, out: IntArray = IntArray(width * height)): IntArray {
        transferRect(x, y, width, height, out, write = false)
        return out
    }
    fun putRect(x: Int, y: Int, width: Int, height: Int, out: IntArray) {
        transferRect(x, y, width, height, out, write = true)
    }

    fun flipY(): SimpleBitmap {
        val out = SimpleBitmap(width, height)
        val row = IntArray(width)
        for (y in 0 until height) {
            getRect(0, y, width, 1, row)
            out.putRect(0, height - 1 - y, width, 1, row)
        }
        return out
    }

    fun rotate90(): SimpleBitmap {
        val out = SimpleBitmap(height, width)
        val row = IntArray(width)
        for (y in 0 until height) {
            getRect(0, y, width, 1, row)
            out.putRect(y, 0, 1, width, row)
        }
        return out
    }

    fun extrude(border: Int): SimpleBitmap {
        val nwidth = width + border * 2
        val nheight = height + border * 2
        val out = SimpleBitmap(nwidth, nheight)
        out.put(border, border, this)
        // left
        run {
            val part = out.slice(Rectangle(border, 0, 1, nheight))
            for (n in 0 until border) out.put(n, 0, part)
        }
        // right
        run {
            val part = out.slice(Rectangle(nwidth - border - 1, 0, 1, nheight))
            for (n in 0 until border) out.put(nwidth - n - 1, 0, part)
        }
        // top
        run {
            val part = out.slice(Rectangle(0, border, nwidth, 1))
            for (n in 0 until border) out.put(0, n, part)
        }
        // bottom
        run {
            val part = out.slice(Rectangle(0, nheight - border - 1, nwidth, 1))
            for (n in 0 until border) out.put(0, nheight - n - 1, part)
        }
        return out
    }
}

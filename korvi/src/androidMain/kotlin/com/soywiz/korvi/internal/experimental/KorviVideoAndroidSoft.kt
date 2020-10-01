package com.soywiz.korvi.internal.experimental

import android.content.Context
import android.graphics.ImageFormat
import android.os.Build
import com.soywiz.klock.*
import com.soywiz.klock.hr.*
import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.color.RGBA
import com.soywiz.korim.color.RgbaArray
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.VfsFile
import com.soywiz.korvi.KorviVideo
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext

// @TODO: Move to KorIM
inline fun decodeYUVA(out: (index: Int, rgba: RGBA) -> Unit, size: Int, getY: (index: Int) -> Int, getU: (index: Int) -> Int, getV: (index: Int) -> Int, getA: (index: Int) -> Int) {
    for (n in 0 until size) {
        val Y = getY(n)
        val U = getU(n)
        val V = getV(n)
        val A = getA(n)
        val Y0 = 1.164f * (Y - 16)
        val V0 = V - 128
        val U0 = (U - 128)
        val R = (Y0 + 1.596f * V0).toInt()
        val G = (Y0 - 0.813f * V0 - 0.391f * U0).toInt()
        val B = (Y0 + 2.018f * U0).toInt()
        out(n, RGBA(R, G, B, A))
    }
}

// @TODO: Move to KorIM
inline fun decodeYUVA(out: RgbaArray, outOffset: Int, size: Int, getY: (index: Int) -> Int, getU: (index: Int) -> Int, getV: (index: Int) -> Int, getA: (index: Int) -> Int) {
    decodeYUVA(
        { n, col -> out[outOffset + n] = col },
        size,
        getY, getU, getV, getA
    )
}

class AndroidKorviVideoSoft(val file: VfsFile, val androidContext: Context, val coroutineContext: CoroutineContext) : KorviVideo() {
    //val realPath = path.trimStart('/')

    init {
        println("TRYING TO OPEN VIDEO '$file'")
    }

    val player =
        VideoPlayer(file, androidContext) { image, player ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // plane #0 is always Y, plane #1 is always U (Cb), and plane #2 is always V (Cr)
                if (image.format != ImageFormat.YUV_420_888) error("Only supported YUV_420 formats")

                val py = image.planes[0]
                val pu = image.planes[1]
                val pv = image.planes[2]

                val _y = py.buffer
                val _u = pu.buffer
                val _v = pv.buffer

                val bmp = Bitmap32(image.width, image.height, premultiplied = true)

                val bmpData = bmp.data
                for (y in 0 until image.height) {
                    val yPos = y * py.rowStride
                    val uvPos = (y / 2) * pu.rowStride

                    decodeYUVA(
                        bmpData,
                        y * bmp.width,
                        bmp.width,
                        getY = { _y.get(yPos + it).toInt() and 0xFF },
                        getU = { _u.get(uvPos + (it / 2)).toInt() and 0xFF },
                        getV = { _v.get(uvPos + (it / 2)).toInt() and 0xFF },
                        getA = { 0xFF }
                    )
                }

                launchImmediately(coroutineContext) {
                    onVideoFrame(Frame(bmp, image.timestamp.toDouble().nanoseconds.hr, 40.milliseconds.hr))
                }
            } else {
                TODO("VERSION.SDK_INT < KITKAT")
            }
        }

    override var running: Boolean = false
    override val elapsedTimeHr: HRTimeSpan get() = 0.milliseconds.hr


    override suspend fun getTotalFrames(): Long? = null
    override suspend fun getDuration(): HRTimeSpan? = null

    override suspend fun play() {
        thread {
            try {
                running = true
                player.play()
            } finally {
                running = false
            }
        }
    }

    override suspend fun seek(frame: Long) {
        TODO()
    }

    override suspend fun seek(time: HRTimeSpan) {
        TODO()
    }

    override suspend fun stop() {
        if (running) {
            player.requestStop()
        }
    }

    override suspend fun close() {
        stop()
    }
}

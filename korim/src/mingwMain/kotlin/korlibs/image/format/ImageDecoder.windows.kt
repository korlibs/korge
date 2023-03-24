package korlibs.image.format

import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.io.async.*
import korlibs.io.concurrent.atomic.korAtomic
import korlibs.io.lang.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.posix.*
import platform.gdiplus.*
import platform.windows.*
import kotlin.native.concurrent.*

actual val nativeImageFormatProvider: NativeImageFormatProvider get() = Win32BaseNativeImageFormatProvider

open class Win32BaseNativeImageFormatProvider : StbImageNativeImageFormatProvider() {
    companion object : Win32BaseNativeImageFormatProvider()
    override fun createBitmapNativeImage(bmp: Bitmap): GdiNativeImage = GdiNativeImage(bmp.toBMP32())
}

open class Win32NativeImageFormatProvider : BaseNativeImageFormatProvider() {
    companion object : Win32NativeImageFormatProvider()

    override suspend fun decodeInternal(data: ByteArray, props: ImageDecodingProps): NativeImageResult = withContext(Dispatchers.ResourceDecoder) {
        initGdiPlusOnce()
        val premultiplied = props.premultipliedSure
        memScoped {
            val width = alloc<FloatVar>()
            val height = alloc<FloatVar>()
            val pimage = allocArray<COpaquePointerVar>(1)

            data.usePinned { datap ->
                val pdata = datap.addressOf(0)
                val pstream = SHCreateMemStream(pdata.reinterpret(), data.size.convert())!!
                try {
                    if (GdipCreateBitmapFromStream(pstream, pimage).toInt() != 0) {
                        return@withContext null
                    }
                } finally {
                    pstream.pointed.lpVtbl?.pointed?.Release?.invoke(pstream)
                }
            }

            GdipGetImageDimension(pimage[0], width.ptr, height.ptr)

            val rect = alloc<GpRect>().apply {
                X = 0
                Y = 0
                Width = width.value.toInt()
                Height = height.value.toInt()
            }
            val bmpData = alloc<BitmapData>()
            if (GdipBitmapLockBits(pimage[0], rect.ptr.reinterpret(), ImageLockModeRead, if (premultiplied) PixelFormat32bppPARGB else PixelFormat32bppARGB, bmpData.ptr.reinterpret()).toInt() != 0) {
                return@withContext null
            }

            val bmpWidth = bmpData.Width.toInt()
            val bmpHeight = bmpData.Height.toInt()
            val out = IntArray((bmpWidth * bmpHeight).toInt())
            out.usePinned { outp ->
                val o = outp.addressOf(0)
                for (y in 0 until bmpHeight) {
                    val optr = (o.reinterpret<IntVar>() + bmpWidth * y)!!
                    val iptr = (bmpData.Scan0.toLong() + (bmpData.Stride * y)).toCPointer<IntVar>()!!
                    memcpy(optr, iptr, (bmpData.Width * 4.convert()).convert())
                    for (x in 0 until bmpWidth) optr[x] = argbToAbgr(optr[x])
                }
            }

            GdipBitmapUnlockBits(pimage[0], bmpData.ptr)
            GdipDisposeImage(pimage[0])

            //println(out.toList())
            Bitmap32(bmpWidth, bmpHeight, RgbaArray(out))
        }
    }?.wrapNativeExt(props) ?: throw IOException("Can't load image from ByteArray")
}

// val r: Int get() = (value ushr 0) and 0xFF
// val g: Int get() = (value ushr 8) and 0xFF
// val b: Int get() = (value ushr 16) and 0xFF
// val a: Int get() = (value ushr 24) and 0xFF
private fun argbToAbgr(col: Int): Int {
    return (col and 0xFF00FF00.toInt()) or // GREEN + ALPHA are in place
        ((col and 0xFF) shl 16) or // Swap R
        ((col shr 16) and 0xFF) // Swap B
}

private var initializedGdiPlus = korAtomic(false)
internal fun initGdiPlusOnce() {
    if (!initializedGdiPlus.compareAndSet(expect = false, update = true)) return
    memScoped {
        val ptoken = allocArray<ULONG_PTRVar>(1)
        val si = alloc<GdiplusStartupInput>().apply {
            GdiplusVersion = 1.convert()
            DebugEventCallback = null
            SuppressExternalCodecs = FALSE
            SuppressBackgroundThread = FALSE
        }
        GdiplusStartup(ptoken, si.ptr, null)
    }
}
package korlibs.image.format.provider

import korlibs.ffi.*
import korlibs.image.bitmap.*
import korlibs.image.format.*
import korlibs.memory.*
import kotlin.math.*

// https://github.com/korlibs/korge/blob/2d6152c4aebb4120cf883dea8c85f053f86f5e2f/korim/src/mingwMain/kotlin/korlibs/image/format/GdiNativeImage.kt
// https://github.com/korlibs/korge/blob/2d6152c4aebb4120cf883dea8c85f053f86f5e2f/korim/src/mingwMain/kotlin/korlibs/image/format/ImageDecoder.windows.kt
object FFIGdiNativeImageFormatProvider : BaseNativeImageFormatProvider() {
    object shlwapi : FFILib("shlwapi") {
        // https://learn.microsoft.com/en-us/windows/win32/api/shlwapi/nf-shlwapi-shcreatememstream
        val SHCreateMemStream: (pInit: ByteArray, cbInit: Int) -> FFIPointer by func()
    }

    object Gdiplus : FFILib("Gdiplus") {
        val GdipCreateBitmapFromStream: (stream: FFIPointer?, bitmapPtr: FFIPointerArray) -> Int by func()
        val GdiplusStartup: (token: FFIPointerArray, input: IntArray, output: FFIPointer?) -> Int by func()
        val GdipGetImageDimension: (ptr: FFIPointer?, pwidth: FloatArray, pheight: FloatArray) -> Int by func()
        val GdipBitmapLockBits: (bitmap: FFIPointer?, rect: IntArray, flags: Int, format: Int, lockedBitmapData: Buffer) -> Int by func()
        val GdipDisposeImage: (ffiPointer: FFIPointer?) -> Int by func()
        val GdipBitmapUnlockBits: (ffiPointer: FFIPointer?, bmpData: Buffer) -> Int by func()
    }

    object Kernel32 : FFILib("Kernel32") {
        val GetProcessHeap: () -> FFIPointer? by func()
        val HeapAlloc: (FFIPointer?, Int, Int) -> FFIPointer? by func()
        val HeapFree : (FFIPointer?, Int, FFIPointer?) -> Boolean by func()

        private val heap get() = Kernel32.GetProcessHeap()

        fun alloc(size: Int): FFIPointer? = Kernel32.HeapAlloc(heap, 8, size)
        fun free(ptr: FFIPointer?): Boolean = Kernel32.HeapFree(heap, 0, ptr)
        //val VirtualAlloc: (lpAddress: FFIPointer?, dwSize: Int, flAllocationType: Int, flProtect: Int) -> FFIPointer? by func()
        //val VirtualFree: (lpAddress: FFIPointer?, dwSize: Int, flAllocationType: Int, flProtect: Int) -> FFIPointer? by func()
    }

    val initializeOnce by lazy {
        val ptoken = FFIPointerArray(1)

        Gdiplus.GdiplusStartup(ptoken, intArrayOf(1, 0, 0, 0, 0, 0, 0), null)
        true
    }

    override suspend fun decodeInternal(data: ByteArray, props: ImageDecodingProps): NativeImageResult {
        initializeOnce
        val pstream = shlwapi.SHCreateMemStream(data, data.size) ?: error("Internal error")
        //println("SHCreateMemStream: ${pstream.str}")
        //FFIMemory
        val pimage = FFIPointerArray(1)
        //val pimage = LongArray(1)
        val pwidth = FloatArray(1)
        val pheight = FloatArray(1)
        try {
            val res = Gdiplus.GdipCreateBitmapFromStream(pstream, pimage)
            if (res != 0) {
                error("Couldn't load image res=$res")
            }
        } finally {
            // IMarshalVtblRelease : [AddRef, DisconnectObject, GetMarshalSizeMax, GetUnmarshalClas, MarshalInterface, QueryInterface, Release, ReleaseMarshalData, UnmarshalInterface]
            pstream.getAlignedFFIPointer(0)?.getAlignedFFIPointer(6)?.castToFunc<(FFIPointer?) -> Unit>()?.invoke(pstream)
        }
        Gdiplus.GdipGetImageDimension(pimage[0], pwidth, pheight)
        val width = pwidth[0].toInt()
        val height = pheight[0].toInt()
        //println("bitmapPtr: ${pimage.toList()}, res=$res, width=$width, height=$height")
        val rect = intArrayOf(0, 0, width, height)
        val ImageLockModeRead = 1
        val premultiplied = true
        val Format32bppPArgb = 925707
        val Format32bppArgb = 2498570
        //val bitmapData = IntArray(32)
        val bmpData = Buffer(4 * 32)
        val res2 = Gdiplus.GdipBitmapLockBits(pimage[0], rect, ImageLockModeRead, if (premultiplied) Format32bppPArgb else Format32bppArgb, bmpData)
        val strideS = bmpData.getS32(8)
        val stride = strideS.absoluteValue
        val ptr = bmpData.getUnalignedFFIPointer(16)

        val out = IntArray(width * height)
        val rawData = ptr!!.getIntArray(stride * height / 4)
        for (y in 0 until height) {
            val outPos = y * width
            val rowPos = y * stride / 4
            for (x in 0 until width) {
                out[outPos + x] = argbToAbgr(rawData[rowPos + x])
            }
        }

        Gdiplus.GdipBitmapUnlockBits(pimage[0], bmpData)
        Gdiplus.GdipDisposeImage(pimage[0])

        return NativeImageResult(BitmapNativeImage(Bitmap32(width, height, out)))
    }

    private fun argbToAbgr(col: Int): Int {
        return (col and 0xFF00FF00.toInt()) or // GREEN + ALPHA are in place
            ((col and 0xFF) shl 16) or // Swap R
            ((col shr 16) and 0xFF) // Swap B
    }
}

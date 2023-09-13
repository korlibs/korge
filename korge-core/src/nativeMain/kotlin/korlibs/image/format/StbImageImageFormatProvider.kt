package korlibs.image.format

import korlibs.image.bitmap.*
import korlibs.io.async.*
import korlibs.io.lang.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.posix.*
import kotlin.native.concurrent.*

open class StbImageNativeImageFormatProvider : BaseNativeImageFormatProvider() {
    companion object : StbImageNativeImageFormatProvider()

    override suspend fun decodeInternal(data: ByteArray, props: ImageDecodingProps): NativeImageResult = withContext(Dispatchers.ResourceDecoder) {
        data.usePinned { dataPin ->
            memScoped {
                val width = alloc<IntVar>()
                val height = alloc<IntVar>()
                val comp = alloc<IntVar>() // Might be 3 (no alpha) or 4 (alpha)
                //val success = stbi_info_from_memory(pin.addressOf(0).reinterpret(), data.size, width.ptr, height.ptr, comp.ptr) != 0

                val pixelsPtr = stb_image.stbi_load_from_memory(dataPin.addressOf(0).reinterpret(), data.size, width.ptr, height.ptr, comp.ptr, 4)
                if (pixelsPtr != null) {
                    val bmp = Bitmap32(width.value, height.value, premultiplied = false)
                    //println("IMAGE: ${width.value}, ${height.value}, ${comp.value}")
                    bmp.ints.usePinned { pixelsPin ->
                        //val components = comp.value
                        val components = 4 // comp.value might be 3, but it is still packed on 32-bits
                        memcpy(pixelsPin.addressOf(0), pixelsPtr, (width.value * height.value * components).convert())
                    }
                    stb_image.stbi_image_free(pixelsPtr)
                    bmp
                } else {
                    null
                }
            }
        }?.wrapNativeExt(props) ?: throw IOException("Failed to decode image using stbi_load_from_memory")
    }

    override suspend fun encodeSuspend(image: ImageDataContainer, props: ImageEncodingProps): ByteArray =
        PNG.encodeSuspend(image, props)
}

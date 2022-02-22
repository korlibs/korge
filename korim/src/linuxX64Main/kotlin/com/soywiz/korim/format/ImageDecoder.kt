package com.soywiz.korim.format

import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.*
import com.soywiz.korio.lang.*
import kotlinx.cinterop.*
import platform.posix.*
import kotlin.native.concurrent.*

@ThreadLocal
actual val nativeImageFormatProvider: NativeImageFormatProvider = object : BaseNativeImageFormatProvider() {
    override suspend fun decodeInternal(data: ByteArray, props: ImageDecodingProps): NativeImageResult {
        val premultiplied = props.premultiplied
        //ImageIOWorker.execute(
        //            TransferMode.SAFE,
        return executeInImageIOWorker { worker ->
            worker.execute(
                TransferMode.SAFE,
                { if (data.isFrozen) data else data.copyOf().freeze() },
                { data ->
                    data.usePinned { dataPin ->
                        memScoped {
                            val width = alloc<IntVar>()
                            val height = alloc<IntVar>()
                            val comp = alloc<IntVar>() // Might be 3 (no alpha) or 4 (alpha)
                            //val success = stbi_info_from_memory(pin.addressOf(0).reinterpret(), data.size, width.ptr, height.ptr, comp.ptr) != 0

                            val pixelsPtr = stb_image.stbi_load_from_memory(dataPin.addressOf(0).reinterpret(), data.size, width.ptr, height.ptr, comp.ptr, 4)
                            if (pixelsPtr != null) {
                                val bmp = Bitmap32(width.value, height.value)
                                //println("IMAGE: ${width.value}, ${height.value}, ${comp.value}")
                                bmp.data.ints.usePinned { pixelsPin ->
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
                    } ?: throw IOException("Failed to decode image using stbi_load_from_memory")
                }
            )
        }.wrapNativeExt(props)
    }
}


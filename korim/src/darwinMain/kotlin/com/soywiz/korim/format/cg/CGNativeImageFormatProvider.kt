package com.soywiz.korim.format.cg

import com.soywiz.kmem.*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.*
import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.CoreGraphics.*
import platform.ImageIO.*
import platform.posix.memcpy
import kotlin.native.concurrent.*

open class CGBaseNativeImageFormatProvider : StbImageNativeImageFormatProvider() {
    companion object : CGBaseNativeImageFormatProvider()
    override fun createBitmapNativeImage(bmp: Bitmap): CoreGraphicsNativeImage = CoreGraphicsNativeImage(bmp.toBMP32().premultipliedIfRequired())
}

open class CGNativeImageFormatProvider : CGBaseNativeImageFormatProvider() {
    companion object : CGNativeImageFormatProvider()
    override suspend fun decodeHeaderInternal(data: ByteArray): ImageInfo {
        memScoped {
            autoreleasepool {
                val cfdata = data.usePinned { dataPin ->
                    CFDataCreate(null, dataPin.addressOf(0).reinterpret(), data.size.convert())
                }
                val imgSource = CGImageSourceCreateWithData(data = cfdata, options = null)
                val props = CGImageSourceCopyPropertiesAtIndex(imgSource, 0, null)
                    ?: error("Failed trying to read image in decodeHeaderInternal")

                try {
                    return ImageInfo().apply {
                        this.width = getIntFromDict(props, kCGImagePropertyPixelWidth)
                        this.height = getIntFromDict(props, kCGImagePropertyPixelHeight)
                    }
                } finally {
                    CFRelease(props)
                }
            }
        }

    }

    //override fun createBitmapNativeImage(bmp: Bitmap) = BitmapNativeImage(bmp.toBMP32().premultipliedIfRequired())
    override suspend fun decodeInternal(data: ByteArray, props: ImageDecodingProps): NativeImageResult {
        // Since we are decoding as premultiplied, we need a decoder that decodes un-multiplied
        if (props.asumePremultiplied) {
            return super.decodeInternal(data, props)
        }

        val premultiplied = props.premultipliedSure

        data class Info(val data: ByteArray, val premultiplied: Boolean, val maxSize: Int?)
        return executeInImageIOWorker { worker ->
            worker.execute(
                TransferMode.SAFE,
                { Info(if (data.isFrozen) data else data.copyOf().freeze(), premultiplied, props.requestedMaxSize) },
                { info ->
                    val data = info.data
                    val premultiplied = info.premultiplied
                    val maxSize = info.maxSize
                    memScoped {
                        val maxSizePtr = alloc<IntVar>()
                        autoreleasepool {
                            val cfdata = data.usePinned { dataPin ->
                                CFDataCreate(null, dataPin.addressOf(0).reinterpret(), data.size.convert())
                            }
                            val imgSource = CGImageSourceCreateWithData(data = cfdata, options = null)

                            val dict = CFDictionaryCreateMutable(null, 0, null, null)

                            CFDictionaryAddValue(dict, kCGImageSourceShouldCache, kCFBooleanFalse)

                            CFDictionaryAddValue(dict, kCGImageSourceCreateThumbnailWithTransform, kCFBooleanFalse)
                            CFDictionaryAddValue(dict, kCGImageSourceCreateThumbnailFromImageAlways, kCFBooleanTrue)

                            val cgImage: CPointer<CGImage>? = if (maxSize != null) {
                                maxSizePtr.value = maxSize

                                // kCGImageSourceSubsampleFactor
                                CFDictionaryAddValue(
                                    dict,
                                    kCGImageSourceThumbnailMaxPixelSize,
                                    CFNumberCreate(null, kCFNumberSInt32Type, maxSizePtr.ptr)
                                )
                                CGImageSourceCreateThumbnailAtIndex(imgSource, 0, dict)
                            } else {
                                CGImageSourceCreateImageAtIndex(imgSource, 0, dict)
                            }
                            val iwidth = CGImageGetWidth(cgImage).toInt()
                            val iheight = CGImageGetHeight(cgImage).toInt()

                            // This might have channels changed? RGBA -> ARGB?, might be in float, etc.
                            // https://developer.apple.com/documentation/coregraphics/1455401-cgimagegetalphainfo
                            // https://developer.apple.com/documentation/coregraphics/cgbitmapinfo
                            if (false) {
                                val data = CGDataProviderCopyData(CGImageGetDataProvider(cgImage))
                                try {
                                    val pixels = CFDataGetBytePtr(data);
                                    Bitmap32(iwidth, iheight, premultiplied = false).also { bmp ->
                                        bmp.ints.usePinned { pin ->
                                            memcpy(pin.startAddressOf, pixels, (iwidth * iheight * 4).convert())
                                        }
                                    }
                                } finally {
                                    CFRelease(data)
                                }
                            } else {

                                //val colorSpace = CGColorSpaceCreateWithName(kCGColorSpaceGenericRGB)
                                val colorSpace = CGColorSpaceCreateWithName(kCGColorSpaceSRGB)
                                //val colorSpace = CGColorSpaceCreateDeviceRGB()
                                try {
                                    val realPremultiplied = true
                                    //val realPremultiplied = premultiplied

                                    val alphaInfo = when (realPremultiplied) {
                                        true -> CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
                                        false -> CGImageAlphaInfo.kCGImageAlphaLast.value
                                    }

                                    Bitmap32(iwidth, iheight, realPremultiplied).also { bmp ->
                                        bmp.ints.usePinned { pin ->
                                            val context = CGBitmapContextCreate(
                                                pin.startAddressOf, iwidth.convert(), iheight.convert(), 8,
                                                (iwidth * 4).convert(), colorSpace, alphaInfo
                                            )
                                                ?: error("Couldn't create context for $iwidth, $iheight, premultiplied=$premultiplied")

                                            try {
                                                val rect = CGRectMake(0.cg, 0.cg, iwidth.cg, iheight.cg)
                                                CGContextDrawImage(context, rect, cgImage)
                                                CGContextFlush(context)
                                            } finally {
                                                CGContextRelease(context)
                                            }
                                        }
                                    }
                                } finally {
                                    CGColorSpaceRelease(colorSpace)
                                }
                            }
                        }
                    }
                })
        }.wrapNativeExt(props)
    }

    private fun getIntFromDict(props: CFDictionaryRef?, key: CFStringRef?): Int {
        return memScoped {
            val vvar = alloc<IntVar>()
            CFNumberGetValue(CFDictionaryGetValue(props, key)?.reinterpret(), kCFNumberIntType, vvar.ptr)
            vvar.value
        }
    }
}

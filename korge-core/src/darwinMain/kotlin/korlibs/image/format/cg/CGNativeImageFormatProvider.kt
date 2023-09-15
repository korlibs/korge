@file:OptIn(ExperimentalForeignApi::class)

package korlibs.image.format.cg

import cnames.structs.CGImage
import korlibs.memory.*
import korlibs.image.bitmap.*
import korlibs.image.format.*
import korlibs.io.async.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import platform.CoreFoundation.*
import platform.CoreGraphics.*
import platform.CoreServices.*
import platform.ImageIO.*
import platform.posix.*
import kotlin.Boolean
import kotlin.ByteArray
import kotlin.Int
import kotlin.native.concurrent.*

open class CGBaseNativeImageFormatProvider : BaseNativeImageFormatProvider() {
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
                val props = CGImageSourceCopyPropertiesAtIndex(imgSource, 0.convert(), null)
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
            return wrapNative(PNG.decode(data, props), props)
        }

        return withContext(Dispatchers.ResourceDecoder) {
            val premultiplied = props.premultipliedSure
            val maxSize = props.requestedMaxSize
            memScoped {
                val maxSizePtr = alloc<IntVar>()
                autoreleasepool {
                    val cfdata = data.usePinned { dataPin ->
                        CFDataCreate(null, dataPin.addressOf(0).reinterpret(), data.size.convert())
                    }
                    val imgSource = CGImageSourceCreateWithData(data = cfdata, options = null)

                    //println("imgSource=$imgSource")

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
                        CGImageSourceCreateThumbnailAtIndex(imgSource, 0.convert(), dict)
                    } else {
                        CGImageSourceCreateImageAtIndex(imgSource, 0.convert(), dict)
                    }
                    val iwidth = CGImageGetWidth(cgImage).toInt()
                    val iheight = CGImageGetHeight(cgImage).toInt()

                    if (iwidth == 0 && iheight == 0) error("Couldn't decode image with CG")

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
                        //val colorSpace = CGColorSpaceCreateWithName(kCGColorSpaceSRGB)
                        val colorSpace = CGColorSpaceCreateDeviceRGB()
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
                                        pin.startAddressOf, iwidth.convert(), iheight.convert(), 8.convert(),
                                        (iwidth * 4).convert(), colorSpace, alphaInfo
                                    )
                                        ?: error("Couldn't create context for $iwidth, $iheight, premultiplied=$premultiplied")

                                    try {
                                        val rect = CGRectMakeExt(0, 0, iwidth, iheight)
                                        CGContextDrawImage(context, rect, cgImage)
                                        CGContextFlush(context)
                                    } finally {
                                        CGImageRelease(cgImage)
                                        CGContextRelease(context)
                                        CFRelease(imgSource)
                                        CFRelease(cfdata)
                                    }
                                }
                            }
                        } finally {
                            CGColorSpaceRelease(colorSpace)
                        }
                    }
                }
            }
        }.wrapNativeExt(props)
    }

    private fun getIntFromDict(props: CFDictionaryRef?, key: CFStringRef?): Int {
        return memScoped {
            val vvar = alloc<IntVar>()
            CFNumberGetValue(CFDictionaryGetValue(props, key)?.reinterpret(), kCFNumberIntType, vvar.ptr)
            vvar.value
        }
    }

    override suspend fun encodeSuspend(image: ImageDataContainer, props: ImageEncodingProps): ByteArray = memScoped {
        val data = CFDataCreateMutable(null, 0)
        val destination = CGImageDestinationCreateWithData(data, when (props.mimeType) {
            "image/jpeg", "image/jpg" -> kUTTypeJPEG
            //"image/heif", "image/heic" -> UTTypeHEIF
            else -> kUTTypePNG
        }, 1.convert(), null)
            ?: error("Failed to create CGImageDestination")

        val imageProperties = CFDictionaryCreateMutable(null, 0, null, null)
        val ref = alloc<DoubleVar>()
        ref.value = props.quality
        val num = CFNumberCreate(null, kCFNumberDoubleType, ref.ptr)
        CFDictionaryAddValue(imageProperties, kCGImageDestinationLossyCompressionQuality, num)

        //println("CGNativeImageFormatProvider.encodeSuspend")
        val cgImage = image.mainBitmap.toBMP32().toCGImage()

        try {
            CGImageDestinationAddImage(destination, cgImage, imageProperties)
            if (!CGImageDestinationFinalize(destination)) error("Can't write image")
        } finally {
            CGImageRelease(cgImage)
            CFRelease(imageProperties)
            CFRelease(num)
            CFRelease(destination)
        }
        val length: Int = CFDataGetLength(data).convert()
        val bytes = CFDataGetMutableBytePtr(data)?.readBytes(length.convert())
        CFRelease(data)
        return bytes ?: error("Can't write image")
    }
}

/*
fun Map<*, *>.toCFDictionary(): CFDictionaryRef = memScoped {
    val dict = CFDictionaryCreateMutable(null, 0, null, null)
    for ((key, value) in this) {
        val ref = alloc<DoubleVar>()
        ref.value = value.todo
        CFNumberCreate(null, kCFNumberDoubleType, null, ref.ptr)
        CFDictionaryAddValue()
    }
    return dict
}
*/

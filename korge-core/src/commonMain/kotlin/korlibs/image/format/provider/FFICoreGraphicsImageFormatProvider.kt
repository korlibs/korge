package korlibs.image.format.provider

import korlibs.datastructure.*
import korlibs.ffi.*
import korlibs.image.bitmap.*
import korlibs.image.format.*
import korlibs.math.geom.*
import korlibs.platform.*

object FFICoreGraphicsImageFormatProvider : BaseNativeImageFormatProvider() {
    override val formats: ImageFormat get() = RegisteredImageFormats
    override suspend fun encodeSuspend(image: ImageDataContainer, props: ImageEncodingProps): ByteArray {
        //Deno.ffi()
        return RegisteredImageFormats.formats.first().encode(image.default)
        //return PNG.encode(image.default.mainBitmap)
    }

    override suspend fun decodeHeaderInternal(data: ByteArray): ImageInfo {

        when {
            Platform.isMac -> {
                val size = getImageSize(data)
                return ImageInfo {width = size.width; height = size.height}
            }
            else -> {
                error("Unsupported platform decodeHeaderInternal '${Platform.os}'")
            }
        }
    }

    override suspend fun decodeInternal(data: ByteArray, props: ImageDecodingProps): NativeImageResult {
        //println("DECODE INTERNAL!")
        when  {
            Platform.isMac -> {
                val (data, size) = getImageData(data)
                //println("DECODE INTERNAL! $data, size=$size")
                return NativeImageResult(
                    BitmapNativeImage(Bitmap32(size.width, size.height, data))
                )
            }
            else -> {
                error("Unsupported platform decodeInternal '${Platform.os}'")
            }
        }
    }

    //init { RegisteredImageFormats.register(PNG) }


    private object CoreFoundation : FFILib("CoreFoundation") {
        val CFDataCreate: (FFIPointer?, ByteArray, Int) -> FFIPointer? by func()
        val CFStringCreateWithBytes: (FFIPointer?, ByteArray, Int, Int, Boolean) -> FFIPointer? by func()
        val CFDictionaryGetValue: (FFIPointer?, FFIPointer?) -> FFIPointer? by func()
        val CFNumberGetValue: (FFIPointer?, Int, IntArray) -> Boolean by func()
        val CFDataGetBytePtr: (FFIPointer?) -> FFIPointer? by func()
        val CFDataGetLength: (FFIPointer?) -> Int by func()
        //val memcpy: ("buffer", FFIPointer?, "usize") -> FFIPointer? by func()
        val CFRelease: (FFIPointer?) -> Unit by func()
    }

    private object CoreGraphics : FFILib("CoreGraphics") {
        val CGImageGetWidth: (FFIPointer?) -> Int by func()
        val CGImageGetHeight: (FFIPointer?) -> Int by func()
        val CGColorSpaceCreateDeviceRGB: () -> FFIPointer? by func()
        val CGBitmapContextCreate: (FFIPointer?, Int, Int, Int, Int, FFIPointer?, Int) -> FFIPointer? by func()
        val CGBitmapContextGetData: (FFIPointer?) -> FFIPointer? by func()
        //val CGBitmapContextCreate: (Buffer, Int, Int, Int, Int, FFIPointer?, Int) -> FFIPointer? by func()
        val CGImageRelease: (FFIPointer?) -> Unit by func()
        val CGContextRelease: (FFIPointer?) -> Unit by func()
        val CGColorSpaceRelease: (FFIPointer?) -> Unit by func()
        val CGContextFlush: (FFIPointer?) -> Unit by func()
        //val CGContextDrawImage: (FFIPointer?, DoubleArray, FFIPointer?) -> Unit by func()
        val CGContextDrawImage: (FFIPointer?, Double, Double, Double, Double, FFIPointer?) -> Unit by func()
    }

    private object ImageIO : FFILib("ImageIO") {
        val CGImageSourceCreateWithData: (FFIPointer?, FFIPointer?) -> FFIPointer? by func()
        val CGImageSourceCopyPropertiesAtIndex: (FFIPointer?, Int, FFIPointer?) -> FFIPointer? by func()
        val CGImageSourceCreateImageAtIndex: (FFIPointer?, Int, FFIPointer?) -> FFIPointer? by func()
    }

    private fun getIntFromDict(props: FFIPointer?, key: String): Int {
        val kCFNumberIntType = 9
        val buffer = IntArray(2)
        val keyPtr = createCFString(key)
        CoreFoundation.CFNumberGetValue(CoreFoundation.CFDictionaryGetValue(props, keyPtr), kCFNumberIntType, buffer)
        CoreFoundation.CFRelease(keyPtr)
        return buffer[0]
    }

    private fun createCFString(str: String): FFIPointer? {
        val NSUTF8StringEncoding = 4
        val bytes = str.encodeToByteArray()//TextEncoder().encode(str)
        return CoreFoundation.CFStringCreateWithBytes(null, bytes, bytes.size, NSUTF8StringEncoding, false)
    }

    private fun getImageSize(bytes: ByteArray): SizeInt {
        val data = CoreFoundation.CFDataCreate(null, bytes, bytes.size)
        val imgSource = ImageIO.CGImageSourceCreateWithData(data, null)
        val props = ImageIO.CGImageSourceCopyPropertiesAtIndex(imgSource, 0, null)
        val width = getIntFromDict(props, "PixelWidth")
        val height = getIntFromDict(props, "PixelHeight")
        CoreFoundation.CFRelease(props)
        CoreFoundation.CFRelease(imgSource)
        CoreFoundation.CFRelease(data)
        return SizeInt(width, height)
    }

    private fun CGRectMake(x: Double, y: Double, width: Double, height: Double): DoubleArray {
        return doubleArrayOf(x, y, width, height)
    }

    private fun getImageData(bytes: ByteArray): Pair<IntArray, SizeInt> {
        //console.log(readPointer(dataPtr, dataLen));
        val premultiplied = true
        val data = CoreFoundation.CFDataCreate(null, bytes, bytes.size)
        val imgSource = ImageIO.CGImageSourceCreateWithData(data, null)
        val dict = null
        val cgImage = ImageIO.CGImageSourceCreateImageAtIndex(imgSource, 0, dict)
        val width: Int = CoreGraphics.CGImageGetWidth(cgImage).fastCastTo<Double>().toInt()
        val height: Int = CoreGraphics.CGImageGetHeight(cgImage).fastCastTo<Double>().toInt()
        val colorSpace = CoreGraphics.CGColorSpaceCreateDeviceRGB()
        val alphaInfo = if (premultiplied) 1 else 3

        val context = CoreGraphics.CGBitmapContextCreate(null, width, height, 8, width * 4, colorSpace, alphaInfo)
        val rect = CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble())
        //CoreGraphics.CGContextDrawImage(context, rect, cgImage)
        CoreGraphics.CGContextDrawImage(context, rect[0], rect[1], rect[2], rect[3], cgImage)
        CoreGraphics.CGContextFlush(context)

        val pixels = CoreGraphics.CGBitmapContextGetData(context)!!.getIntArray(width * height)

        CoreGraphics.CGImageRelease(cgImage)
        CoreGraphics.CGContextRelease(context)
        CoreFoundation.CFRelease(imgSource)
        CoreFoundation.CFRelease(data)
        CoreGraphics.CGColorSpaceRelease(colorSpace)


//    const data = CGDataProviderCopyData(CGImageGetDataProvider(cgImage))
        return pixels to SizeInt(width, height)
    }
}

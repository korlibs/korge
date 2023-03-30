package korlibs.image.format

import korlibs.image.format.cg.*
import korlibs.image.format.ui.*
import kotlinx.cinterop.*
import platform.UIKit.*

//actual val nativeImageFormatProvider: NativeImageFormatProvider = UIImageNativeImageFormatProvider
actual val nativeImageFormatProvider: NativeImageFormatProvider get() = UINativeImageFormatProvider
//actual val nativeImageFormatProvider: NativeImageFormatProvider get() = CGBaseNativeImageFormatProvider

object UINativeImageFormatProvider : CGNativeImageFormatProvider() {
    @OptIn(UnsafeNumber::class)
    override suspend fun encodeSuspend(image: ImageDataContainer, props: ImageEncodingProps): ByteArray {
        val uiImage: UIImage = image.mainBitmap.toBMP32().toUIImage()
        //println("Using UINativeImageFormatProvider.encodeSuspend")
        return when (props.mimeType) {
            "image/jpeg", "image/jpg" -> UIImageJPEGRepresentation(uiImage, compressionQuality = props.quality.cg)?.toByteArray()
            else -> UIImagePNGRepresentation(uiImage)?.toByteArray()
        } ?: error("Can't encode using UIImageRepresentation")
    }
}


/*
object UIImageNativeImageFormatProvider : BaseNativeImageFormatProvider() {
    override fun createBitmapNativeImage(bmp: Bitmap) = CoreGraphicsNativeImage(bmp.toBMP32().premultipliedIfRequired())
    //override fun createBitmapNativeImage(bmp: Bitmap) = BitmapNativeImage(bmp.toBMP32().premultipliedIfRequired())

    override suspend fun decodeInternal(data: ByteArray, props: ImageDecodingProps): NativeImageResult {
        val premultiplied = props.premultipliedSure
        data class Info(val data: ByteArray, val premultiplied: Boolean)

        return executeInImageIOWorker { worker ->
            worker.execute(TransferMode.SAFE, { Info(data.copyOf(), premultiplied) }, { info ->
                //return run { val info = Info(data.copyOf(), premultiplied)
                val data = info.data
                val premultiplied = info.premultiplied
                val nsdata: NSData = data.usePinned { pin -> NSData.dataWithBytes(pin.addressOf(0), data.size.convert()) }

                val image = UIImage.imageWithData(nsdata) ?: error("Can't read image")
                val imageRef = image.CGImage
                //val colorSpace = CGColorSpaceCreateWithName(kCGColorSpaceGenericRGB)

                val width = CGImageGetWidth(imageRef).toInt()
                val height = CGImageGetHeight(imageRef).toInt()
                val area = width * height
                //println("UIImage.imageWithData: nsdata=${data.size}, width=$width, height=$height")
                val out = IntArray(width * height)

                val colorSpace = CGColorSpaceCreateDeviceRGB()
                //val ctxWidth = width.coerceAtLeast(16)
                //val ctxHeight = height.coerceAtLeast(16)
                val ctxWidth = width
                val ctxHeight = height
                //val ctxReqStride = 0
                val ctxReqStride = ctxWidth * 4
                val ctx = CGBitmapContextCreate(
                    null,
                    ctxWidth.convert(),
                    ctxHeight.convert(),
                    8.convert(),
                    ctxReqStride.convert(),
                    colorSpace,
                    when (premultiplied) {
                        true -> CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
                        false -> CGImageAlphaInfo.kCGImageAlphaLast.value
                    }
                )
                CGColorSpaceRelease(colorSpace)
                UIGraphicsPushContext(ctx)
                run {
                    CGContextDrawImage(ctx, CGRectMake(0.toCgFloat(), 0.0.toCgFloat(), width.toCgFloat(), height.toCgFloat()), imageRef)
                    val startPtr = CGBitmapContextGetData(ctx)!!.reinterpret<IntVar>()
                    val ctxStride = CGBitmapContextGetBytesPerRow(ctx).toInt()
                    out.usePinned { pin ->
                        memcpy(pin.addressOf(0), startPtr, (area * 4).convert())
                        //for (n in 0 until height) memcpy(pin.addressOf(width * n), startPtr + (n * ctxStride), ctxStride.convert())
                    }
                    UIGraphicsPopContext()
                }
                CGContextRelease(ctx)

                Bitmap32(width, height, RgbaArray(out), premultiplied = premultiplied)
                //}.wrapNativeExt(premultiplied)
            })
        }.wrapNativeExt(props)
    }
}
*/

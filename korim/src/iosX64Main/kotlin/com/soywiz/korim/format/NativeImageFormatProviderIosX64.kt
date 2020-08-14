package com.soywiz.korim.format

/*
import com.soywiz.korim.bitmap.*
import com.soywiz.korim.color.*
import com.soywiz.korim.format.cg.*
import com.soywiz.korio.async.*
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*
import platform.posix.*
import kotlin.native.concurrent.*

actual val nativeImageFormatProvider: NativeImageFormatProvider = object : BaseNativeImageFormatProvider() {
    override fun createBitmapNativeImage(bmp: Bitmap) = CoreGraphicsNativeImage(bmp.toBMP32().premultipliedIfRequired())

    override suspend fun decode(data: ByteArray, premultiplied: Boolean): NativeImage {
        data class Info(val data: ByteArray, val premultiplied: Boolean)

        return ImageIOWorker.execute(TransferMode.SAFE, { Info(if (data.isFrozen) data else data.copyOf().freeze(), premultiplied) }, { info ->
        //return run { val info = Info(if (data.isFrozen) data else data.copyOf().freeze(), premultiplied)
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
        }).await().wrapNativeExt(premultiplied)
    }
}
*/

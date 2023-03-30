package korlibs.image.format

import korlibs.image.format.cg.CGNativeImageFormatProvider

// https://developer.apple.com/library/archive/documentation/GraphicsImaging/Conceptual/drawingwithquartz2d/dq_context/dq_context.html#//apple_ref/doc/uid/TP30001066-CH203-BCIBHHBB
//@ThreadLocal
//actual val nativeImageFormatProvider: NativeImageFormatProvider get() = CGBaseNativeImageFormatProvider
actual val nativeImageFormatProvider: NativeImageFormatProvider get() = CGNativeImageFormatProvider

//actual val nativeImageFormatProvider: NativeImageFormatProvider = NSNativeImageFormatProvider

/*
object NSNativeImageFormatProvider : BaseNativeImageFormatProvider() {
    override fun createBitmapNativeImage(bmp: Bitmap) = CoreGraphicsNativeImage(bmp.toBMP32().premultipliedIfRequired())

    //override fun createBitmapNativeImage(bmp: Bitmap) = BitmapNativeImage(bmp.toBMP32().premultipliedIfRequired())
    override suspend fun decodeInternal(data: ByteArray, props: ImageDecodingProps): NativeImageResult {
        val premultiplied = props.premultipliedSure

        data class Info(val data: ByteArray, val premultiplied: Boolean)
        return executeInImageIOWorker { worker ->
            worker.execute(
                TransferMode.SAFE,
                { Info(data.copyOf(), premultiplied) },
                { info ->
                    val data = info.data
                    val premultiplied = info.premultiplied
                    autoreleasepool {
                        data.usePinned { dataPin ->
                            val nsdata: NSData = NSData.dataWithBytes(dataPin.addressOf(0), data.size.convert())

                            /*
                            val cfdata = data.usePinned { dataPin ->
                                CFDataCreate(null, dataPin.addressOf(0).reinterpret(), data.size.convert())
                            }
                            CGImageSourceCreateWithData(data = cfdata, options = null)
                            CGImageSourceCreateImageAtIndex()
                            */

                            val image = NSImage(data = nsdata)
                            var iwidth = 0
                            var iheight = 0
                            val imageSize = image.size
                            imageSize.useContents { iwidth = width.toInt(); iheight = height.toInt() }
                            val imageRect = NSMakeRect(0.0, 0.0, iwidth.toDouble(), iheight.toDouble())
                            val colorSpace = CGColorSpaceCreateDeviceRGB()
                            //val colorSpace = CGColorSpaceCreateWithName(kCGColorSpaceGenericRGB)
                            //val colorSpace = CGColorSpaceCreateWithName(kCGColorSpaceExtendedLinearSRGB)
                            try {
                                val ctx = CGBitmapContextCreate(
                                    null, iwidth.convert(), iheight.convert(),
                                    8.convert(), 0.convert(), colorSpace, when (premultiplied) {
                                        true -> CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
                                        false -> CGImageAlphaInfo.kCGImageAlphaLast.value
                                    }
                                )
                                try {
                                    val oldContext = NSGraphicsContext.currentContext
                                    val gctx = NSGraphicsContext.graphicsContextWithCGContext(ctx, flipped = false)
                                    NSGraphicsContext.setCurrentContext(gctx)
                                    try {
                                        image.drawInRect(imageRect)
                                        Bitmap32(iwidth, iheight, premultiplied = premultiplied).also { bmp ->
                                            transferBitmap32CGContext(bmp, ctx, toBitmap = true)
                                        }
                                    } finally {
                                        NSGraphicsContext.setCurrentContext(oldContext)
                                    }
                                } finally {
                                    CGContextRelease(ctx)
                                }
                            } finally {
                                CGColorSpaceRelease(colorSpace)
                            }
                        }
                    }
                })
        }.wrapNativeExt(props)
    }
}
*/

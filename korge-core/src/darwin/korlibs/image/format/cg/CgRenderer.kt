@file:Suppress("UnusedImport")

package korlibs.image.format.cg

import korlibs.memory.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.image.vector.*
import korlibs.image.format.*
import korlibs.image.paint.*
import korlibs.math.geom.*
import korlibs.math.geom.vector.*
import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.CoreGraphics.*
import platform.posix.*
import kotlin.math.*

fun test() {
}

//fun transferBitmap32CGImageRef(bmp: Bitmap32, image: CGImageRef?, toBitmap: Boolean) {
//    val width = CGImageGetWidth(image).toInt()
//    val height = CGImageGetHeight(image).toInt()
//    val colorSpace = CGColorSpaceCreateDeviceRGB()
//    val out = bmp.ints
//    out.usePinned { outPin ->
//        val bytesPerPixel = 4
//        val bytesPerRow = bytesPerPixel * width
//        val bitsPerComponent = 8
//        val context = CGBitmapContextCreate(
//            outPin.startAddressOf, width.convert(), height.convert(), bitsPerComponent.convert(), bytesPerRow.convert(), colorSpace,
//            CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value or kCGBitmapByteOrder32Big
//        )
//        try {
//            CGColorSpaceRelease(colorSpace)
//
//            CGContextDrawImage(context, CGRectMake(0.cg, 0.cg, width.cg, height.cg), image)
//        } finally {
//            CGContextRelease(context)
//        }
//    }
//}

private fun swapRB(value: Int): Int = (value and 0xFF00FF00.toInt()) or ((value and 0xFF) shl 16) or ((value ushr 16) and 0xFF)

fun swapRB(ptr: CPointer<IntVar>?, count: Int) {
    if (ptr == null) return
    for (n in 0 until count) ptr[n] = swapRB(ptr[n])
}

//fun swapRB(ptr: CPointer<ByteVar>?, count: Int) {
//    if (ptr == null) return
//    for (n in 0 until count) {
//        val r = ptr[n * 4 + 0]
//        val g = ptr[n * 4 + 2]
//        ptr[n * 4 + 2] = r
//        ptr[n * 4 + 0] = g
//    }
//}

fun transferBitmap32CGContext(bmp: Bitmap32, ctx: CGContextRef?, toBitmap: Boolean) {
    val ctxBytesPerRow = CGBitmapContextGetBytesPerRow(ctx).toInt()
    val ctxWidth = CGBitmapContextGetWidth(ctx).toInt()
    val ctxHeight = CGBitmapContextGetHeight(ctx).toInt()
    val pixels = CGBitmapContextGetData(ctx)?.reinterpret<IntVar>() ?: error("Can't get pixels!")
    val minWidth = min(ctxWidth, bmp.width)
    val minHeight = min(ctxHeight, bmp.height)
    val out = bmp.ints
    out.usePinned { outPin ->
        val outStart: CPointer<IntVarOf<Int>> = outPin.startAddressOf
        val widthInBytes: size_t = (minWidth * 4).convert()
        for (n in 0 until minHeight) {
            val bmpPtr = outStart + ctxWidth * n
            val ctxPtr = pixels.reinterpret<ByteVar>() + ctxBytesPerRow * n
            when {
                toBitmap -> {
                    memcpy(bmpPtr, ctxPtr, widthInBytes)
                    swapRB(bmpPtr?.reinterpret(), minWidth)
                }
                else -> {
                    //swapRB(bmpPtr?.reinterpret(), minWidth)
                    memcpy(ctxPtr, bmpPtr, widthInBytes)
                    //swapRB(bmpPtr?.reinterpret(), minWidth) // Reverse since we cannot write directly without memcopy to ctxPtr
                    //swapRB(ctxPtr?.reinterpret(), minWidth)
                }
            }
        }
    }
}

/**
 * Returned image must be deallocated with [CGImageRelease]
 */
fun transferBitmap32ToCGImage(bmp: Bitmap32, colorSpace: CGColorSpaceRef? = null): CGImageRef? {
    val allocColorSpace = if (colorSpace == null) CGColorSpaceCreateDeviceRGB() else null

    val imageCtx: CGContextRef? = CGBitmapContextCreate(
        null, bmp.width.convert(), bmp.height.convert(),
        8.convert(), 0.convert(), colorSpace ?: allocColorSpace,
        CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
    )
    try {
        transferBitmap32CGContext(bmp, imageCtx, toBitmap = false)
        return CGBitmapContextCreateImage(imageCtx)
    } finally {
        CGContextRelease(imageCtx)
        if (allocColorSpace != null) CGColorSpaceRelease(allocColorSpace)
    }
}

class CoreGraphicsNativeImage(bitmap: Bitmap32) : BitmapNativeImage(bitmap) {
    override val name: String get() = "CoreGraphicsNativeImage"
    override fun toBMP32(): Bitmap32 = bitmap.clone()
    override fun getContext2d(antialiasing: Boolean): Context2d = Context2d(CoreGraphicsRenderer(bitmap, antialiasing))
}

@PublishedApi
internal inline fun <T> cgKeepState(ctx: CGContextRef?, callback: () -> T): T {
    CGContextSaveGState(ctx)
    try {
        return callback()
    } finally {
        CGContextRestoreGState(ctx)
    }
}

@OptIn(UnsafeNumber::class)
class CoreGraphicsRenderer(val bmp: Bitmap32, val antialiasing: Boolean) : korlibs.image.vector.renderer.BufferedRenderer() {
    override val width: Int get() = bmp.width
    override val height: Int get() = bmp.height

    override fun Paint.isPaintSupported(): Boolean = when {
        this is GradientPaint && this.isSweep -> false
        else -> true
    }

    fun Matrix.toCGAffineTransform() = CGAffineTransformMake(a.cg, b.cg, c.cg, d.cg, tx.cg, ty.cg)
    fun MMatrix.toCGAffineTransform() = CGAffineTransformMake(a.cg, b.cg, c.cg, d.cg, tx.cg, ty.cg)

    private fun cgDrawBitmap(bmp: Bitmap32, ctx: CGContextRef?, colorSpace: CGColorSpaceRef?, tiled: Boolean = false) {
        val image = transferBitmap32ToCGImage(bmp, colorSpace)
        try {
            val rect = CGRectMakeExt(0, 0, bmp.width, bmp.height)

            if (tiled) {
                CGContextDrawTiledImage(ctx, rect, image)
            } else {
                //println("CGContextDrawImage: $ctx, ${bmp.size}")
                CGContextDrawImage(ctx, rect, image)
            }
        } finally {
            CGImageRelease(image)
        }
    }

    override fun flushCommands(commands: List<RenderCommand>) {
        if (bmp.ints.size == 0) return
        bmp.flipY() // @TODO: This shouldn't be required, can we do an affine transform somewhere?
        Releases { releases ->
            autoreleasepool {
                bmp.ints.usePinned { dataPin ->
                    //val colorSpace = CGColorSpaceCreateWithName(kCGColorSpaceGenericRGB)
                    val colorSpace = CGColorSpaceCreateDeviceRGB()
                    try {
                        val ctx: CGContextRef? = CGBitmapContextCreate(
                            dataPin.addressOf(0), bmp.width.convert(), bmp.height.convert(),
                            8.convert(), (bmp.width * 4).convert(), colorSpace,
                            CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
                        )

                        //transferBitmap32CGContext(bmp, ctx, toBitmap = false)

                        try {
                            // Restore context
                            //cgKeepState(ctx) {
                            //    cgDrawBitmap(bmp, ctx, colorSpace)
                            //}

                            fun visitCgContext(ctx: CGContextRef?, path: VectorPath) {
                                path.visitCmds(
                                    moveTo = { (x, y) -> CGContextMoveToPoint(ctx, x.cg, y.cg) },
                                    lineTo = { (x, y) -> CGContextAddLineToPoint(ctx, x.cg, y.cg) },
                                    quadTo = { (cx, cy), (ax, ay) -> CGContextAddQuadCurveToPoint(ctx, cx.cg, cy.cg, ax.cg, ay.cg) },
                                    cubicTo = { (cx1, cy1), (cx2, cy2), (ax, ay) -> CGContextAddCurveToPoint(ctx, cx1.cg, cy1.cg, cx2.cg, cy2.cg, ax.cg, ay.cg) },
                                    close = { CGContextClosePath(ctx) }
                                )
                            }

                            for (command in commands) {
                                val state = command.state
                                val fill = command.fill

                                //println("command=$command")

                                cgKeepState(ctx) {
                                    CGContextSetAllowsAntialiasing(ctx, antialiasing)
                                    CGContextSetAlpha(ctx, state.globalAlpha.cg)
                                    //CGContextConcatCTM(ctx, state.transform.toCGAffineTransform()) // Points already transformed

                                    // PATH
                                    visitCgContext(ctx, state.path)

                                    // CLIP
                                    val clip = state.clip
                                    if (clip != null) {
                                        when (clip.winding) {
                                            Winding.EVEN_ODD -> CGContextEOClip(ctx)
                                            else -> CGContextClip(ctx)
                                        }
                                        CGContextBeginPath(ctx)
                                        visitCgContext(ctx, clip)
                                    }

                                    memScoped {
                                        if (!fill) {
                                            CGContextSetLineWidth(ctx, state.lineWidth.cg)
                                            CGContextSetMiterLimit(ctx, state.miterLimit.cg)
                                            CGContextSetLineJoin(
                                                ctx, when (state.lineJoin) {
                                                    LineJoin.BEVEL -> CGLineJoin.kCGLineJoinBevel
                                                    LineJoin.MITER -> CGLineJoin.kCGLineJoinMiter
                                                    LineJoin.ROUND -> CGLineJoin.kCGLineJoinRound
                                                }
                                            )
                                            CGContextSetLineCap(
                                                ctx, when (state.lineCap) {
                                                    LineCap.BUTT -> CGLineCap.kCGLineCapButt
                                                    LineCap.ROUND -> CGLineCap.kCGLineCapRound
                                                    LineCap.SQUARE -> CGLineCap.kCGLineCapSquare
                                                }
                                            )
                                            val lineDashFloatArray = state.lineDashFloatArray
                                            if (lineDashFloatArray != null) {
                                                val lengths = allocArray<CGFloatVar>(lineDashFloatArray.size)
                                                for (n in lineDashFloatArray.indices) {
                                                    lengths[n] = lineDashFloatArray[n].cg
                                                }
                                                CGContextSetLineDash(
                                                    ctx,
                                                    state.lineDashOffset.cg,
                                                    lengths,
                                                    lineDashFloatArray.size.convert()
                                                )
                                            } else {
                                                CGContextSetLineDash(ctx, 0.0.cg, null, 0.convert())
                                            }
                                            CGContextReplacePathWithStrokedPath(ctx)
                                        }

                                        val style = if (fill) state.fillStyle else state.strokeStyle
                                        when (style) {
                                            is NonePaint -> Unit
                                            is ColorPaint -> {
                                                CGContextSetFillColorWithColor(
                                                    ctx,
                                                    style.color.toCgColor(releases, colorSpace)
                                                )
                                                when (command.winding ?: state.path.winding) {
                                                    Winding.EVEN_ODD -> CGContextEOFillPath(ctx)
                                                    Winding.NON_ZERO -> CGContextFillPath(ctx)
                                                }
                                            }

                                            is GradientPaint -> {
                                                val nelements = style.colors.size
                                                val colors = CFArrayCreateMutable(null, nelements.convert(), null)
                                                val locations = allocArray<CGFloatVar>(nelements)
                                                for (n in 0 until nelements) {
                                                    val color = RGBA(style.colors[n])
                                                    val stop = style.stops[n]
                                                    CFArrayAppendValue(colors, color.toCgColor(releases, colorSpace))
                                                    locations[n] = stop.cg
                                                }
                                                val options =
                                                    kCGGradientDrawsBeforeStartLocation or kCGGradientDrawsAfterEndLocation

                                                CGContextClip(ctx)
                                                val gradient = CGGradientCreateWithColors(colorSpace, colors, locations)
                                                val start = CGPointMake(style.x0.cg, style.y0.cg)
                                                val end = CGPointMake(style.x1.cg, style.y1.cg)
                                                cgKeepState(ctx) {
                                                    CGContextConcatCTM(ctx, state.transform.toCGAffineTransform())
                                                    CGContextConcatCTM(ctx, style.transform.toCGAffineTransform())
                                                    when (style.kind) {
                                                        GradientKind.LINEAR -> {
                                                            CGContextDrawLinearGradient(ctx, gradient, start, end, options)
                                                        }

                                                        GradientKind.RADIAL -> {
                                                            CGContextDrawRadialGradient(ctx, gradient, start, style.r0.cg, end, style.r1.cg, options)
                                                        }

                                                        GradientKind.SWEEP -> {
                                                            // @TODO: Fix me! Can we implement it by creating a bitmap with the size of the vector path?
                                                            // https://stackoverflow.com/questions/40188058/how-to-draw-a-circle-path-with-color-gradient-stroke
                                                            CGContextDrawLinearGradient(ctx, gradient, start, end, options)
                                                        }

                                                        else -> Unit
                                                    }
                                                }
                                                CGGradientRelease(gradient)
                                            }

                                            is BitmapPaint -> {
                                                CGContextClip(ctx)
                                                cgKeepState(ctx) {
                                                    CGContextConcatCTM(ctx, state.transform.toCGAffineTransform())
                                                    CGContextConcatCTM(ctx, style.transform.toCGAffineTransform())
                                                    val fillBmp = style.bmp32
                                                    fillBmp.flipY() // @TODO: This shouldn't be required, can we do an affine transform somewhere?
                                                    cgDrawBitmap(fillBmp, ctx, colorSpace, tiled = style.repeat)
                                                    fillBmp.flipY() // @TODO: This shouldn't be required, can we do an affine transform somewhere?
                                                }
                                                //println("Not implemented style $style fill=$fill")
                                            }

                                            else -> {
                                                println("Not implemented style $style fill=$fill")
                                            }
                                        }
                                    }
                                }
                            }
                        } finally {
                            //transferBitmap32CGContext(bmp, ctx, toBitmap = true)
                            CGContextRelease(ctx)
                        }
                    } finally {
                        CGColorSpaceRelease(colorSpace)
                    }
                }
            }
        }
        bmp.flipY() // @TODO: This shouldn't be required, can we do an affine transform somewhere?
    }
}

//fun RGBA.toCG() = CGColorCreateGenericRGB(rd, gd, bd, ad)

internal class Releases {
    val colors = arrayListOf<CGColorRef?>()
    val arena = Arena()

    companion object {
        inline operator fun invoke(callback: (Releases) -> Unit) {
            val releases = Releases()
            try {
                callback(releases)
            } finally {
                releases.release()
            }
        }
    }

    fun release() {
        for (color in colors) {
            CGColorRelease(color)
        }
        arena.clear()
    }
}

internal fun RGBA.toCgColor(releases: Releases, space: CGColorSpaceRef?): CGColorRef? = memScoped {
    // Not available on iOS
    //CGColorCreateGenericRGB(color.rd.cg, color.gd.cg, color.bd.cg, color.ad.cg)
    val data = allocArray<CGFloatVar>(4)
    //val data = releases.arena.allocArray<CGFloatVar>(4)
    data[0] = this@toCgColor.rd.cg
    data[1] = this@toCgColor.gd.cg
    data[2] = this@toCgColor.bd.cg
    data[3] = this@toCgColor.ad.cg
    val color = CGColorCreate(space, data)
    releases.colors.add(color)
    color
}

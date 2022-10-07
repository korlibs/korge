package com.soywiz.korim.format.ns

import com.soywiz.korim.bitmap.Bitmap32
import com.soywiz.korim.format.cg.cg
import com.soywiz.korim.format.cg.transferBitmap32CGContext
import com.soywiz.korma.geom.IPoint
import com.soywiz.korma.geom.IPointInt
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.Rectangle
import kotlinx.cinterop.CValue
import kotlinx.cinterop.convert
import kotlinx.cinterop.useContents
import platform.AppKit.NSImage
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageRelease
import platform.Foundation.NSMakePoint
import platform.Foundation.NSMakeSize
import platform.Foundation.NSPoint
import platform.Foundation.NSRect

fun Bitmap32.toNSImage(): NSImage {
    val bmp = this
    val colorSpace = CGColorSpaceCreateDeviceRGB()
    val ctx = CGBitmapContextCreate(
        null, bmp.width.convert(), bmp.height.convert(),
        8.convert(), 0.convert(), colorSpace,
        CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
    )
    val image = CGBitmapContextCreateImage(ctx)
    try {
        transferBitmap32CGContext(this, ctx, toBitmap = false)
        return NSImage(image, NSMakeSize(bmp.width.cg, bmp.height.cg))
    } finally {
        CGImageRelease(image)
        CGContextRelease(ctx)
        CGColorSpaceRelease(colorSpace)
    }
}

//fun NSImage.toBitmap32(): Bitmap32 { TODO() }

fun IPoint.toNSPoint(): CValue<NSPoint> = NSMakePoint(x.cg, y.cg)
fun IPointInt.toNSPoint(): CValue<NSPoint> = NSMakePoint(x.cg, y.cg)
fun CValue<NSPoint>.toPoint(): Point = useContents { Point(this.x, this.y) }
fun CValue<NSRect>.toRectangle(): Rectangle = useContents { Rectangle(this.origin.x, this.origin.y, this.size.width, this.size.height) }

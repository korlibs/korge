package com.soywiz.korim.format.ns

import com.soywiz.korim.bitmap.*
import com.soywiz.korim.format.cg.*
import com.soywiz.korma.annotations.*
import com.soywiz.korma.geom.*
import kotlinx.cinterop.*
import platform.AppKit.*
import platform.CoreGraphics.*
import platform.Foundation.*

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
        return NSImage(image, NSMakeSize(bmp.width.toDouble(), bmp.height.toDouble()))
    } finally {
        CGImageRelease(image)
        CGContextRelease(ctx)
        CGColorSpaceRelease(colorSpace)
    }
}

//fun NSImage.toBitmap32(): Bitmap32 { TODO() }

@KormaValueApi fun Point.toNSPoint(): CValue<NSPoint> = NSMakePoint(x.toDouble(), y.cg.toDouble())
@KormaValueApi fun PointInt.toNSPoint(): CValue<NSPoint> = NSMakePoint(x.cg.toDouble(), y.cg.toDouble())
@KormaMutableApi fun CValue<NSPoint>.toPoint(): Point = useContents { Point(this.x, this.y) }
@KormaMutableApi fun CValue<NSRect>.toRectangle(): Rectangle = useContents { Rectangle(this.origin.x, this.origin.y, this.size.width, this.size.height) }

@KormaMutableApi fun IPoint.toNSPoint(): CValue<NSPoint> = NSMakePoint(x.toDouble(), y.cg.toDouble())
@KormaMutableApi fun IPointInt.toNSPoint(): CValue<NSPoint> = NSMakePoint(x.cg.toDouble(), y.cg.toDouble())
@KormaMutableApi fun CValue<NSPoint>.toMPoint(): MPoint = useContents { MPoint(this.x, this.y) }
@KormaMutableApi fun CValue<NSRect>.toMRectangle(): MRectangle = useContents { MRectangle(this.origin.x, this.origin.y, this.size.width, this.size.height) }

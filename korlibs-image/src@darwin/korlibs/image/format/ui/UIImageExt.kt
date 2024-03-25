package korlibs.image.format.ui

import cnames.structs.CGContext
import korlibs.image.bitmap.*
import korlibs.image.format.cg.*
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.UIKit.*

fun Bitmap.toUIImage(): UIImage {
    val cgImage = transferBitmap32ToCGImage(this.toBMP32IfRequired())
    try {
        return UIImage(cGImage = cgImage)
    } finally {
        CGImageRelease(cgImage)
    }
}

fun UIImage.toBitmap32(): Bitmap32 {
    val out = Bitmap32(this.size.useContents { width }.toInt(), this.size.useContents { height }.toInt(), premultiplied = true)
    UIGraphicsBeginImageContext(this.size)
    try {
        val ctx: CPointer<CGContext>? = UIGraphicsGetCurrentContext(); // here you don't need this reference for the context but if you want to use in the future for drawing anything else on the context you could get it for it
        this.drawInRect(CGRectMake(0.cg, 0.cg, out.width.cg, out.height.cg))
        transferBitmap32CGContext(out, ctx, toBitmap = true)
        return out
    } finally {
        UIGraphicsEndImageContext()
    }
}

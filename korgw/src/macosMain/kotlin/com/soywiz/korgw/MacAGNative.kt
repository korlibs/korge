package com.soywiz.korgw

import com.soywiz.korag.gl.AGNative
import com.soywiz.korma.geom.Size
import kotlinx.cinterop.useContents
import platform.AppKit.NSWindow
import platform.CoreGraphics.CGDisplayScreenSize
import platform.Foundation.NSNumber

open class MacAGNative(val window: NSWindow) : AGNative() {
    override val devicePixelRatio: Double
        get() {
            //return NSScreen.mainScreen?.backingScaleFactor?.toDouble() ?: field
            return window.backingScaleFactor
        }

    override val pixelsPerInch: Double
        get() {
            val screen = window.screen ?: return 96.0
            val screenSizeInPixels = screen.visibleFrame.useContents { Size(size.width, size.height) }
            val screenSizeInMillimeters = CGDisplayScreenSize(((screen.deviceDescription["NSScreenNumber"]) as NSNumber).unsignedIntValue).useContents { Size(width, height) }

            val dpmm = screenSizeInPixels.width / screenSizeInMillimeters.width
            val dpi = dpmm / 0.0393701

            //println("screenSizeInPixels=$screenSizeInPixels")
            //println("screenSizeInMillimeters=$screenSizeInMillimeters")
            //println("dpmm=$dpmm")
            //println("dpi=$dpi")

            return dpi // 1 millimeter -> 0.0393701 inches
        }
}

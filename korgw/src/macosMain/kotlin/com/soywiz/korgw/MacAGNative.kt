package com.soywiz.korgw

import com.soywiz.korag.gl.AGNative
import com.soywiz.korma.geom.Size
import kotlinx.cinterop.useContents
import platform.AppKit.NSWindow
import platform.CoreGraphics.CGDisplayScreenSize
import platform.Foundation.NSNumber

open class MacAGNative(val window: NSWindow) : AGNative() {

}

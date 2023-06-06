package korlibs.render.macos

import kotlinx.cinterop.*
import platform.AppKit.*
import platform.Foundation.*

class Window(
    contentRect: CValue<NSRect>,
    styleMask: NSWindowStyleMask,
    backing: NSBackingStoreType,
    defer: Boolean
) : NSWindow(
    contentRect, styleMask, backing, defer
)

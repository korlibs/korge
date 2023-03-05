package korge.graphics.backend.metal

import kotlinx.cinterop.CValue
import kotlinx.cinterop.autoreleasepool
import platform.AppKit.*
import platform.CoreGraphics.CGSize
import platform.Foundation.NSMakeRect
import platform.Foundation.NSNotification
import platform.Foundation.NSRect
import platform.Metal.MTLClearColorMake
import platform.Metal.MTLCreateSystemDefaultDevice
import platform.Metal.MTLDeviceProtocol
import platform.Metal.MTLPixelFormatBGRA8Unorm_sRGB
import platform.MetalKit.MTKView
import platform.MetalKit.MTKViewDelegateProtocol
import platform.darwin.NSObject
import platform.foundation.height
import platform.foundation.width

val windowStyle = NSWindowStyleMaskTitled or NSWindowStyleMaskMiniaturizable or
        NSWindowStyleMaskClosable or NSWindowStyleMaskResizable or NSBackingStoreBuffered

class MetalApplication(
    private val windowTitle: String,
    private val rendererProvider: (MTLDeviceProtocol) -> Renderer
) {

    fun run() {

        autoreleasepool {
            val application = NSApplication.sharedApplication()

            val windowRect: CValue<NSRect> = run {
                val frame = NSScreen.mainScreen()!!.frame
                NSMakeRect(
                    .0, .0,
                    frame.width * 0.5,
                    frame.height * 0.5
                )
            }

            val window = NSWindow(windowRect, windowStyle, NSBackingStoreBuffered, false)
            val device = MTLCreateSystemDefaultDevice() ?: error("fail to create device")
            val renderer = rendererProvider(device)
            val mtkView = MTKView(windowRect, device).apply {
                colorPixelFormat = MTLPixelFormatBGRA8Unorm_sRGB
                clearColor = MTLClearColorMake(0.0, 0.0, 0.0, 1.0)
            }

            application.delegate = object : NSObject(), NSApplicationDelegateProtocol {

                override fun applicationShouldTerminateAfterLastWindowClosed(sender: NSApplication): Boolean {
                    println("applicationShouldTerminateAfterLastWindowClosed")
                    return true
                }

                override fun applicationWillFinishLaunching(notification: NSNotification) {
                    println("applicationWillFinishLaunching")
                }

                override fun applicationDidFinishLaunching(notification: NSNotification) {
                    println("applicationDidFinishLaunching")

                    mtkView.delegate = object : NSObject(), MTKViewDelegateProtocol {
                        override fun drawInMTKView(view: MTKView) {
                            renderer.drawOnView(view)
                        }

                        override fun mtkView(view: MTKView, drawableSizeWillChange: CValue<CGSize>) {

                        }

                    }

                    window.setContentView(mtkView)
                    window.setTitle(windowTitle)


                    window.orderFrontRegardless()
                    window.center()
                    window.level = NSFloatingWindowLevel
                }

                override fun applicationWillTerminate(notification: NSNotification) {
                    println("applicationWillTerminate")
                    // Insert code here to tear down your application
                }
            }

            application.run()
        }
    }
}

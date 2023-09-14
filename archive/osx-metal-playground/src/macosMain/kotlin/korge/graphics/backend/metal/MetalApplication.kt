package korge.graphics.backend.metal

import kotlinx.cinterop.*
import platform.AppKit.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.Metal.*
import platform.MetalKit.*
import platform.darwin.*
import platform.foundation.*

val windowStyle = NSWindowStyleMaskTitled or NSWindowStyleMaskMiniaturizable or
        NSWindowStyleMaskClosable or NSWindowStyleMaskResizable or NSBackingStoreBuffered

class MetalApplication(
    private val windowTitle: String,
    private val rendererProvider: (MTKView) -> Renderer
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
            val mtkView = MTKView(windowRect, device).apply {
                colorPixelFormat = MTLPixelFormatBGRA8Unorm_sRGB
                clearColor = MTLClearColorMake(0.0, 0.0, 0.0, 1.0)
            }
            val renderer = rendererProvider(mtkView)

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

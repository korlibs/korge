package korlibs.render.awt

import korlibs.datastructure.event.*
import korlibs.datastructure.lock.*
import korlibs.datastructure.thread.*
import korlibs.image.bitmap.*
import korlibs.image.color.*
import korlibs.korge.render.*
import korlibs.platform.*
import korlibs.render.osx.*
import korlibs.time.*
import java.awt.*
import javax.swing.*
import kotlin.math.*
import kotlin.test.*

class AwtGameCanvasTest {
    @Test
    @Ignore
    fun test() = autoreleasePool {
        //System.setProperty("sun.java2d.metal", "true")
        //System.setProperty("sun.java2d.opengl", "false")
        System.setProperty("sun.java2d.opengl", "true")
        //System.setProperty("sun.java2d.opengl", "false")

        val frame = object : JFrame("Title") {
        }
        val bmp = Bitmap32(128, 128) { x, y -> Colors.BLACK.withB(x * 2).withG(y * 2) }.premultiplied()
        val bmp2 = Bitmap32(128, 128) { x, y -> Colors.BLACK.withR(x * 2).withG(y * 2) }.premultiplied()

        val el = SyncEventLoop()
        val viewsLock = Lock()
        var canvas: Component? = null
        var fpsLabel: JLabel? = null

        var x = 0

        el.setInterval(60.hz.timeSpan) {
            viewsLock {
                x++
                if (!Platform.isMac) {
                    canvas?.repaint()
                }
            }
        }

        nativeThread {
            el.runTasksForever()
        }

        frame.contentPane.layout = GridLayout(2, 2)
        frame.contentPane.add(AwtAGOpenglCanvas().also {
            canvas = it
            val renderContext = RenderContext(it.ag, it)
            it.doRender = { ag ->
                renderContext.doRenderNew {
                    //println(renderContext.currentWidth)
                    renderContext.clear(Colors.LIGHTPINK)
                    viewsLock {
                        renderContext.drawBitmapXY(renderContext.currentFrameBuffer, bmp, (sin(x.toFloat() / 50f) * 200 + 200).toInt(), 100)
                        //SolidRect(100, 100, Colors.MEDIUMPURPLE).render(renderContext)
                    }
                }
                fpsLabel?.text = "FPS: ${(canvas as AwtAGOpenglCanvas).renderFps}"
            }
            it.visible = true
        })
        /*
        frame.contentPane.add(GLCanvas().also {
            canvas = it
            //val renderContext = RenderContext(it.ag, it)
            val renderContext = RenderContext(it.ag)
            it.defaultRendererAG = { ag ->
                renderContext.doRenderNew {
                    //println(renderContext.currentWidth)
                    renderContext.clear(Colors.LIGHTPINK)
                    viewsLock {
                        renderContext.drawBitmapXY(renderContext.currentFrameBuffer, bmp, (sin(x.toFloat() / 50f) * 200 + 200).toInt(), 100)
                        //SolidRect(100, 100, Colors.MEDIUMPURPLE).render(renderContext)
                    }
                }
            }
        })

         */
        frame.contentPane.add(JLabel().also {
            fpsLabel = it
            it.isOpaque = true; it.background = Color.YELLOW })
        frame.contentPane.add(JLabel().also { it.isOpaque = true; it.background = Color.GREEN })
        //frame.contentPane.add(GLCanvas().also {
        //    it.defaultRenderer = { gl, g ->
        //        gl.clearColor(1f, .5f, 1f, 1f)
        //        gl.clear(KmlGl.COLOR_BUFFER_BIT)
        //    }
        //})
        /*
        frame.contentPane.add(AwtAGOpenglCanvas().also {
            val renderContext = RenderContext(it.ag, it)
            it.doRender = { ag ->
                renderContext.doRenderNew {
                    //println(renderContext.currentWidth)
                    renderContext.clear(Colors.MEDIUMPURPLE)
                    renderContext.drawBitmap(renderContext.currentFrameBuffer, bmp2, -.5f, +.5f, +.5f, -.5f)
                }
                //SolidRect(100, 100, Colors.MEDIUMPURPLE).render(renderContext)
            }
        })
         */
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.preferredSize = Dimension(600, 600)
        frame.pack()
        frame.setLocationRelativeTo(null)
        frame.isVisible = true

        /*
        //println("PEER: " + AWTAccessor.getComponentAccessor().getPeer<ComponentPeer>(frame))
        val layer = frame.getCAMetalLayer()
        println("layer=$layer, device=${layer?.device}")
        val dict = NSMutableDictionary()
        dict["test"] = 9
        println(NSNumber(dict["test"].id))
        dict.setValue(NSNumber(9), NSString("hello"))
        println(dict.count)
        val tex = CoreVideoOpenGLMetalSharedTexture(512, 512)
        println("tex=$tex")
        //val dic = NSDictionary()
        //println("dic=$dic : ${dic.count}")
        println(NSNumber(9).intValue)
        println(NSNumber(10L).longValue)
        println(NSNumber(11.1).doubleValue)
        //println(NSClass("NSNumber").alloc().msgSend("init"))
         */

        //CPlatformWindow

        NativeThread.sleep(100.seconds)
    }
}

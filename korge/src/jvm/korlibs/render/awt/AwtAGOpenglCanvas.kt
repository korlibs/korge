package korlibs.render.awt

import korlibs.event.*
import korlibs.graphics.*
import korlibs.graphics.gl.*
import korlibs.kgl.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.platform.*
import korlibs.render.*
import korlibs.render.platform.*
import java.awt.Container
import java.awt.Graphics

// @TODO: Use Metal, OpenGL or whatever required depending on what's AWT is using
class AwtAGOpenglCanvas : Container(), EventListener by BaseEventListener(), BoundsProvider by BoundsProvider.Base() {
    //override val ag: AGOpengl = AGOpenglAWT(checkGl = true, logGl = true)
    val ag: AG = AGOpenglAWT()

    var ctx: BaseOpenglContext? = null

    override fun paint(g: Graphics) {
        //super.paint(g)
        dispatch(RenderEvent)
        ensureContext()
        //g.fillRect(0, 0, 100, 100)
        ctx?.useContext(g, ag, paintInContextDelegate)
    }

    val state: KmlGlState by lazy { (ag as AGOpengl).createGlState() }

    override var actualVirtualBounds: Rectangle = Rectangle(0, 0, 200, 200)
    //val renderContext = RenderContext(ag, this)

    var doRender: (AG) -> Unit = { ag -> }

    val paintInContextDelegate: (Graphics, BaseOpenglContext.ContextInfo) -> Unit = { g, info ->
        //println("--------------------------------------")
        state.keep {
        //run {
            //println("g.clipBounds=${g.clipBounds}, info=$info")
            val viewport = info.viewport
            if (viewport != null) {
                ag.startEndFrame {
                    val frameBuffer = AGFrameBuffer()
                    //ag.mainFrameBuffer.setSize(viewport.x, viewport.y, viewport.width, viewport.height)
                    val mainFrameBuffer = ag.mainFrameBuffer
                    //mainFrameBuffer.scissor(RectangleInt(0, 0, 100, 100))
                    val frameOrComponent = this
                    val factor = 2f

                    //mainFrameBuffer.setSize(100, 100)

                    mainFrameBuffer.setSize(
                        viewport.x, viewport.y, viewport.width, viewport.height,
                        (frameOrComponent.width * factor).toInt(),
                        (frameOrComponent.height * factor).toInt(),
                    )
                    frameBuffer.setSize(0, 0, viewport.width, viewport.height)
                    //frameBuffer.scissor = RectangleInt(0, 0, 50, 50)
                    ag.setMainFrameBufferTemporarily(frameBuffer) {
                        doRender(ag)
                    }

                    //val tex = AGTexture()
                    //tex.upload(Bitmap32(128, 128) { x, y -> Colors.BLACK.withB(x * 2).withG(y * 2) })

                    //ag.textureDrawer.draw(frameBuffer, tex, -.5f, -.5f, +.5f, +.5f)

                    /*
                    ag.clear(frameBuffer.base, frameBuffer.info, Colors.FUCHSIA)

                     */
                    //ag.gl.viewport(0, 0, 500, 500)
                    //ag.gl.scissor(0, 0, 500, 500)
                    //mainFrameBuffer.setSize(0, 200, 600, 600)
                    //println("mainFrameBuffer.info=${mainFrameBuffer.info}")
                    ag.textureDrawer.draw(mainFrameBuffer, frameBuffer.tex)
                    //ag.clear(mainFrameBuffer, Colors.FUCHSIA)
                }
            }
        }
    }

    private fun ensureContext() {
        val frame = this
        val cfg = GameWindowConfig.Impl()
        if (ctx == null) {
            if (Platform.isMac) {
                try {
                    //ctx = ProxiedMacAWTOpenglContext(frame)
                    ctx = glContextFromComponent(frame, cfg)

                } catch (e: Throwable) {
                    e.printStackTrace()
                    ctx = glContextFromComponent(frame, cfg)
                }
            } else {
                ctx = glContextFromComponent(frame, cfg)
            }
        }
    }

}

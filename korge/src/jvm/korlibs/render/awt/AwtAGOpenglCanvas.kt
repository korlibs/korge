package korlibs.render.awt

import korlibs.graphics.*
import korlibs.graphics.gl.*
import korlibs.kgl.*
import korlibs.korge.view.*
import korlibs.math.geom.*
import korlibs.math.geom.Rectangle
import korlibs.render.*
import korlibs.render.platform.*
import java.awt.*
import java.awt.Graphics

// @TODO: Use Metal, OpenGL or whatever required depending on what's AWT is using
open class AwtAGOpenglCanvas : Canvas(), BoundsProvider by BoundsProvider.Base() {
    //override val ag: AGOpengl = AGOpenglAWT(checkGl = true, logGl = true)
    val ag: AG = AGOpenglAWT()

    var ctx: BaseOpenglContext? = null

    override fun paint(g: Graphics) {
        //super.paint(g)
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
            ag.startEndFrame {
                //ag.mainFrameBuffer.setSize(viewport.x, viewport.y, viewport.width, viewport.height)
                val mainFrameBuffer = ag.mainFrameBuffer

                val viewport = info.viewport
                if (viewport != null) {
                //if (false) {
                    viewport!!
                    val frameOrComponent = this
                    val transform = this.graphicsConfiguration.defaultTransform
                    val factor = transform.scaleX
                    val frameBuffer = AGFrameBuffer()
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
                    ag.textureDrawer.draw(mainFrameBuffer, frameBuffer.tex)
                } else {
                    doRender(ag)
                }
            }
        }
    }

    private fun ensureContext() {
        if (ctx == null) {
            ctx = glContextFromComponent(this, GameWindowConfig.Impl())
        }
    }

}

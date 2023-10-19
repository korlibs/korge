package korlibs.render.awt

import korlibs.graphics.*
import korlibs.graphics.gl.*
import korlibs.kgl.*
import korlibs.korge.view.*
import korlibs.math.geom.Rectangle
import korlibs.platform.*
import korlibs.render.*
import korlibs.render.osx.*
import korlibs.render.platform.*
import korlibs.time.*
import java.awt.*
import java.awt.Graphics
import javax.swing.*

// @TODO: Use Metal, OpenGL or whatever required depending on what's AWT is using
open class AwtAGOpenglCanvas : Canvas(), BoundsProvider by BoundsProvider.Base() {
    //override val ag: AGOpengl = AGOpenglAWT(checkGl = true, logGl = true)
    val ag: AG = AGOpenglAWT()

    var ctx: BaseOpenglContext? = null

    var autoRepaint = true

    private val counter = TimeSampler(1.seconds)
    val renderFps: Int get() = counter.count

    override fun paint(g: Graphics) {
        counter.add()
        //super.paint(g)
        if (ctx == null) {
            ctx = glContextFromComponent(this, GameWindowConfig.Impl())
            if (ctx!!.supportsSwapInterval()) {
                ctx?.swapInterval(1)
            }
        }
        //g.fillRect(0, 0, 100, 100)
        val ctx = this.ctx
        if (ctx != null) {
            ctx.useContext(g, ag, paintInContextDelegate)
            if (autoRepaint && ctx.supportsSwapInterval()) {
                SwingUtilities.invokeLater { repaint() }
            }
        }
    }

    private val dl = if (!Platform.isMac) null else OSXDisplayLink {
        if (autoRepaint && ctx?.supportsSwapInterval() != true) {
            repaint()
            //SwingUtilities.invokeLater { repaint() }
        }
        //println("FRAME!")
    }

    init {
        addHierarchyListener {
            if (dl == null) return@addHierarchyListener
            val added = getContainerFrame()?.isVisible == true
            if (dl.running != added) {
                if (added) dl.start() else dl.stop()
                println("!!! processHierarchyEvent: added=$added")
            }
        }
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

                val frameOrComponent = this
                val transform = this.graphicsConfiguration.defaultTransform
                val factor = transform.scaleX

                val scaledWidth = (frameOrComponent.width * factor).toInt()
                val scaledHeight = (frameOrComponent.height * factor).toInt()
                val viewportScale = 1.0

                //println("factor=$factor")

                if (viewport != null) {
                //if (false) {
                    viewport!!
                    val frameBuffer = AGFrameBuffer()
                    mainFrameBuffer.setSize(
                        (viewport.x * viewportScale).toInt(),
                        (viewport.y * viewportScale).toInt(),
                        (viewport.width * viewportScale).toInt(),
                        (viewport.height * viewportScale).toInt(),
                        scaledWidth,
                        scaledHeight,
                    )
                    frameBuffer.setSize(scaledWidth, scaledHeight)
                    //frameBuffer.scissor = RectangleInt(0, 0, 50, 50)

                    //ag.clear(frameBuffer)

                    ag.setMainFrameBufferTemporarily(frameBuffer) {
                        doRender(ag)
                    }
                    ag.textureDrawer.draw(mainFrameBuffer, frameBuffer.tex)
                } else {
                    mainFrameBuffer.setSize(scaledWidth, scaledHeight)
                    doRender(ag)
                }
            }
        }
    }

}

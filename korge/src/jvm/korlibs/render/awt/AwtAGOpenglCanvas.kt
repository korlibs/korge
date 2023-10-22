package korlibs.render.awt

import korlibs.datastructure.*
import korlibs.datastructure.lock.*
import korlibs.graphics.*
import korlibs.graphics.gl.*
import korlibs.kgl.*
import korlibs.korge.view.*
import korlibs.math.geom.Rectangle
import korlibs.platform.*
import korlibs.render.*
import korlibs.render.osx.*
import korlibs.render.osx.metal.*
import korlibs.render.platform.*
import korlibs.time.*
import java.awt.*
import java.awt.Graphics
import javax.swing.*

// @TODO: Use Metal, OpenGL or whatever required depending on what's AWT is using
// https://stackoverflow.com/questions/52108178/swing-animation-still-stutter-when-i-use-toolkit-getdefaulttoolkit-sync
// https://www.oracle.com/java/technologies/painting.html
// https://docs.oracle.com/javase/tutorial/extra/fullscreen/rendering.html
// https://docs.oracle.com/javase/tutorial/extra/fullscreen/doublebuf.html
//open class AwtAGOpenglCanvas : Canvas(), BoundsProvider by BoundsProvider.Base(), Extra by Extra.Mixin() {
open class AwtAGOpenglCanvas : JPanel(GridLayout(1, 1), false.also { System.setProperty("sun.java2d.opengl", "true") }), BoundsProvider by BoundsProvider.Base(), Extra by Extra.Mixin() {
    val canvas = object : Canvas() {
        private fun renderGraphics(g: Graphics) {
            counter.add()
            //super.paint(g)
            if (ctx == null) {
                contextLost()
                ctx = glContextFromComponent(this, GameWindowConfig.Impl())
                if (ctx!!.supportsSwapInterval()) {
                    ctx?.swapInterval(1)
                }
            }
            //g.fillRect(0, 0, 100, 100)
            val ctx = this@AwtAGOpenglCanvas.ctx
            if (isUsingMetalPipeline) {
                println("!!! ERROR: Using Metal pipeline ${this::class} won't work")
            }
            //println("CTX: $ctx")
            ctx?.useContext(g, ag, paintInContextDelegate)
            if (ctx != null) {
                if (autoRepaint && ctx.supportsSwapInterval()) {
                    //if (true) {
                    SwingUtilities.invokeLater {
                        //vsyncLock { vsyncLock.wait(0.5.seconds) }
                        requestFrame()
                    }
                }
            }
        }

        override fun paint(g: Graphics) {
            //if (!createdBufferStrategy) {
            //    createdBufferStrategy = true
            //    val buf = createBufferStrategy(2)
            //}
            renderGraphics(g)
        }

        //var createdBufferStrategy = false

        private val dl: OSXDisplayLink? = if (!Platform.isMac) null else OSXDisplayLink {
            //private val dl: OSXDisplayLink? = if (true) null else OSXDisplayLink {
            //if (autoRepaint && ctx?.supportsSwapInterval() != true && createdBufferStrategy) {
            if (autoRepaint && ctx?.supportsSwapInterval() != true) {
                //val bufferStrat = bufferStrategy
                //val g = bufferStrat.drawGraphics
                //renderGraphics(g)
                //bufferStrategy.show()

                //requestFrame()
                SwingUtilities.invokeLater { requestFrame() }
                //vsyncLock { vsyncLock.notify() }
            }
            //println("FRAME!")
        }

        init {
            addHierarchyListener {
                if (dl == null) return@addHierarchyListener
                val added = getContainerFrame()?.isVisible == true
                if (dl.running != added) {
                    if (added) dl.start() else dl.stop()
                    //println("!!! processHierarchyEvent: added=$added")
                }
            }
        }
    }.also { layout = GridLayout(1, 1) }.also { add(it) }
    //override val ag: AGOpengl = AGOpenglAWT(checkGl = true, logGl = true)
    val ag: AG = AGOpenglAWT()

    var ctx: BaseOpenglContext? = null

    var autoRepaint = true

    val vsyncLock = Lock()

    private val counter = TimeSampler(1.seconds)
    val renderFps: Int get() = counter.count



    private var contextLost: Boolean = false

    fun contextLost() {
        contextLost = true
    }

    fun requestFrame() {
        canvas.repaint()
        //repaint()
    }

    val state: KmlGlState by lazy { (ag as AGOpengl).createGlState() }

    override var actualVirtualBounds: Rectangle = Rectangle(0, 0, 200, 200)
    //val renderContext = RenderContext(ag, this)

    var doRender: (AG) -> Unit = { ag -> }

    val frameBuffer = AGFrameBuffer()

    val paintInContextDelegate: (Graphics, BaseOpenglContext.ContextInfo) -> Unit = { g, info ->
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
                //val factor = 1.0

                val scaledWidth = (frameOrComponent.width * factor).toInt()
                val scaledHeight = (frameOrComponent.height * factor).toInt()
                val viewportScale = 1.0
                //println("factor=$factor, viewportScale=$viewportScale, viewport=$viewport")

                if (contextLost) {
                    contextLost = false
                    ag.contextLost()
                }

                //println("factor=$factor")

                if (viewport != null) {
                //if (false) {
                    viewport!!
                    mainFrameBuffer.setSize(
                        (viewport.x * viewportScale).toInt(),
                        (viewport.y * viewportScale).toInt(),
                        (viewport.width * viewportScale).toInt(),
                        (viewport.height * viewportScale).toInt(),
                        scaledWidth,
                        scaledHeight,
                    )
                    frameBuffer.setSize(viewport.width, viewport.height)
                    //frameBuffer.scissor = RectangleInt(0, 0, 50, 50)

                    //ag.clear(frameBuffer)

                    ag.setMainFrameBufferTemporarily(frameBuffer) {
                        doRender(ag)
                    }
                    ag.textureDrawer.draw(mainFrameBuffer, frameBuffer.tex, -1f, -1f, +1f, +1f)
                } else {
                    mainFrameBuffer.setSize(scaledWidth, scaledHeight)
                    doRender(ag)
                }
            }
        }
    }

}

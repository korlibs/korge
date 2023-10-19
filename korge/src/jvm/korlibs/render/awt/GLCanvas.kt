package korlibs.render.awt

import korlibs.graphics.*
import korlibs.kgl.*
import korlibs.graphics.gl.*
import korlibs.kgl.*
import korlibs.korge.view.*
import korlibs.render.*
import korlibs.render.platform.*
import java.awt.*
import java.awt.Graphics
import java.io.*

open class GLCanvas constructor(checkGl: Boolean = true, val logGl: Boolean = false, cacheGl: Boolean = false) : Canvas(), GameWindowConfig, Closeable {
    val ag: AGOpengl = AGOpenglAWT(checkGl, logGl, cacheGl)
    var ctx: BaseOpenglContext? = null
    val gl = ag.gl
    private var doContextLost = false

    override fun getGraphicsConfiguration(): GraphicsConfiguration? {
        return super.getGraphicsConfiguration()
    }

    override fun addNotify() {
        super.addNotify()
        close()
    }

    override fun removeNotify() {
        super.removeNotify()
        close()
    }

    override fun reshape(x: Int, y: Int, width: Int, height: Int) {
        super.reshape(x, y, width, height)
        repaint()
    }

    override fun update(g: Graphics) {
        paint(g)
    }

    override fun paint(g: Graphics) {
        //val componentId = Native.getComponentID(this)
        //if (ctxComponentId != componentId) {
        //    close()
        //}
        if (logGl) {
            //println("+++++++++++++++++++++++++++++")
        }
        if (ctx == null) {
            //println("--------------------------------------")
            //ctxComponentId = componentId
            ctx = glContextFromComponent(this, this)
            doContextLost = true
        }
        //println("--------------")
        ctx?.useContext(g, ag) { g, info ->
            if (doContextLost) {
                doContextLost = false
                ag.contextLost()
            }
            render(gl, g, info)
        }
    }


    override fun close() {
        ctx?.dispose()
        ctx = null
    }

    var defaultRendererAG: (ag: AG) -> Unit = {
    }

    var defaultRenderer: (gl: KmlGl, g: Graphics) -> Unit = { gl, g ->
        /*
        ctx?.useContext(g, ag) {
            gl.clearColor(0f, 0f, 0f, 1f)
            gl.clear(gl.COLOR_BUFFER_BIT)
        }
         */
    }

    val getCurrent: () -> Any? = { ctx?.getCurrent() }

    open fun render(gl: KmlGl, g: Graphics, info: BaseOpenglContext.ContextInfo) {
        //ctx?.makeCurrent()
        gl.info.current = getCurrent
        ag.startEndFrame {
            val viewport = info.viewport
            if (viewport != null) {
                gl.viewport(viewport.x, viewport.y, viewport.width, viewport.height)
                gl.scissor(viewport.x, viewport.y, viewport.width, viewport.height)
                gl.enable(KmlGl.SCISSOR_TEST)
                //println("viewport=$viewport")
            }
            defaultRenderer(gl, g)
            defaultRendererAG(ag)
        }
    }

    override var quality: GameWindow.Quality = GameWindow.Quality.AUTOMATIC
}

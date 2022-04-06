package com.soywiz.korgw.awt

import com.soywiz.kgl.*
import com.soywiz.korgw.*
import com.soywiz.korgw.platform.*
import com.sun.jna.*
import java.awt.*
import java.io.*

open class GLCanvas constructor(checkGl: Boolean = true, logGl: Boolean = false, cacheGl: Boolean = false) : Canvas(), GameWindowConfig, Closeable {
    val ag: AwtAg = AwtAg(this, checkGl, logGl, cacheGl)
    private var ctxComponentId: Long = -1L
    var ctx: BaseOpenglContext? = null
    val gl = ag.gl

    var logGl: Boolean by ag::logGl

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
            println("+++++++++++++++++++++++++++++")
        }
        if (ctx == null) {
            println("--------------------------------------")
            //ctxComponentId = componentId
            ctx = glContextFromComponent(this, this)
            ag.contextLost()
        }
        //println("--------------")
        render(gl, g)
    }


    override fun close() {
        ctx?.dispose()
        ctx = null
    }

    var defaultRenderer: (gl: KmlGl, g: Graphics) -> Unit = { gl, g ->
        /*
        ctx?.useContext(g, ag) {
            gl.clearColor(0f, 0f, 0f, 1f)
            gl.clear(gl.COLOR_BUFFER_BIT)
        }
         */
    }

    val isCurrent: () -> Any? = { ctx?.getCurrent() }

    open fun render(gl: KmlGl, g: Graphics) {
        //ctx?.makeCurrent()
        gl.info.current = isCurrent
        defaultRenderer(gl, g)
    }

    override var quality: GameWindow.Quality = GameWindow.Quality.AUTOMATIC
}

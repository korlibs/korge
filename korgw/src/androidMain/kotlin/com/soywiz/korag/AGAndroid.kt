package com.soywiz.korag

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import com.soywiz.kgl.KmlGlAndroid
import com.soywiz.korio.util.Once
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

object AGFactoryAndroid : AGFactory {
    override val supportsNativeFrame: Boolean = false

    //override fun create(nativeControl: Any?, config: AGConfig): AG = AGAndroid(nativeControl as Context)
    override fun create(nativeControl: Any?, config: AGConfig): AG = TODO()

    override fun createFastWindow(title: String, width: Int, height: Int): AGWindow {
        TODO()
    }
}

/*
private typealias GL = GLES20
private typealias gl = GLES20

class AGAndroid(val context: Context) : AGOpengl() {
    override val gl = KmlGlAndroid()
    val ag = this
    val glv = GLSurfaceView(context)
    override val nativeComponent: Any = glv
    override val android: Boolean = true

    private fun onRender(ag: AG) {

    }

    init {
        //glv.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        glv.setEGLContextClientVersion(2)
        glv.setRenderer(object : GLSurfaceView.Renderer {
            val onReadyOnce = Once()

            private fun initializeOnce() {
                onReadyOnce {
                    ready()
                }
            }

            override fun onDrawFrame(gl1: GL10) {
                //println("Android.onDrawFrame")
                initializeOnce()
                //if (DEBUG_AGANDROID) println("Android.onDrawFrame... " + Thread.currentThread())
                onRender(ag)
                //gl = gl1 as GLES20
            }

            override fun onSurfaceChanged(gl1: GL10, width: Int, height: Int) {
                //setViewport(0, 0, width, height)
                initializeOnce()
                //resized()
                onRender(ag)
            }

            override fun onSurfaceCreated(gl1: GL10, p1: EGLConfig) {
                initializeOnce()
                //gl = gl1 as GLES20
                onRender(ag)
            }
        })
        glv.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

    override fun repaint() {
        glv.requestRender()
    }
}
*/

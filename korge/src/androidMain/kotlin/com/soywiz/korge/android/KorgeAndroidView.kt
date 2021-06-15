package com.soywiz.korge.android

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.widget.RelativeLayout
import com.soywiz.kds.Pool
import com.soywiz.kgl.KmlGl
import com.soywiz.kgl.KmlGlAndroid
import com.soywiz.klock.*
import com.soywiz.korag.AGOpengl
import com.soywiz.korev.InitEvent
import com.soywiz.korev.RenderEvent
import com.soywiz.korev.TouchEvent
import com.soywiz.korev.dispatch
import com.soywiz.korge.Korge
import com.soywiz.korge.scene.Module
import com.soywiz.korgw.AndroidGameWindowNoActivity
import com.soywiz.korgw.TouchEventHandler
import com.soywiz.korio.Korio
import com.soywiz.korio.android.withAndroidContext
import com.soywiz.korio.file.std.cleanUpResourcesVfs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class KorgeAndroidView(context: Context) : RelativeLayout(context, null) {
    var mGLView: com.soywiz.korgw.KorgwSurfaceView? = null
    private var agOpenGl: AGOpengl? = null
    private var gameWindow: AndroidGameWindowNoActivity? = null

    private val renderEvent = RenderEvent()
    private val initEvent = InitEvent()

    private var moduleLoaded = false

    inner class KorgeViewAGOpenGL : AGOpengl() {

        override val gl: KmlGl = KmlGlAndroid({ mGLView?.clientVersion ?: -1 })
        override val nativeComponent: Any get() = this@KorgeAndroidView
        override val gles: Boolean = true

        // @TODO: Cache somehow?
        override val pixelsPerInch: Double get() = getResources().getDisplayMetrics().densityDpi.toDouble()

        override fun repaint() {
            mGLView?.invalidate()
        }
    }

    fun unloadModule() {

        if (moduleLoaded) {

            gameWindow?.dispatchDestroyEvent()
            gameWindow?.coroutineContext = null
            gameWindow?.close()
            gameWindow?.exit()
            mGLView = null
            gameWindow = null
            agOpenGl = null
            cleanUpResourcesVfs()

            CoroutineScope(Dispatchers.Main).launch {
                mGLView?.let { removeView(it) }
            }

            moduleLoaded = false
        }
    }

    fun loadModule(module: Module) {

        if (!moduleLoaded) {

            agOpenGl = KorgeViewAGOpenGL()
            gameWindow = AndroidGameWindowNoActivity(module.windowSize.width, module.windowSize.height, agOpenGl!!, context) { mGLView!! }

            mGLView = com.soywiz.korgw.KorgwSurfaceView(this, context, gameWindow!!)

            addView(mGLView)

            gameWindow?.let { gameWindow ->

                Korio(context) {
                    try {
                        withAndroidContext(context) {
                            withContext(coroutineContext + gameWindow) {
                                Korge(Korge.Config(module = module))
                            }
                        }
                    } finally {
                        println("${javaClass::getName} completed!")
                    }
                }
            }

            moduleLoaded = true
        }
    }
}

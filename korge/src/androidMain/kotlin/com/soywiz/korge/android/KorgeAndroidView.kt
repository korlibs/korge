package com.soywiz.korge.android

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.soywiz.kgl.KmlGl
import com.soywiz.kgl.KmlGlAndroid
import com.soywiz.korag.gl.AGOpengl
import com.soywiz.korev.InitEvent
import com.soywiz.korev.RenderEvent
import com.soywiz.korge.Korge
import com.soywiz.korge.scene.Module
import com.soywiz.korgw.AndroidGameWindowNoActivity
import com.soywiz.korio.Korio
import com.soywiz.korio.android.withAndroidContext
import com.soywiz.korio.file.std.cleanUpResourcesVfs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class KorgeAndroidView(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {
    var mGLView: com.soywiz.korgw.KorgwSurfaceView? = null
    private var agOpenGl: AGOpengl? = null
    private var gameWindow: AndroidGameWindowNoActivity? = null

    private val renderEvent = RenderEvent()
    private val initEvent = InitEvent()

    private var moduleLoaded = false

    inner class KorgeViewAGOpenGL : AGOpengl() {

        override val gl: KmlGl = KmlGlAndroid { mGLView?.clientVersion ?: -1 }
        override val nativeComponent: Any get() = this@KorgeAndroidView

        // @TODO: Cache somehow?
        override val pixelsPerInch: Double get() = resources.displayMetrics.densityDpi.toDouble()

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
                        println("${javaClass.name} completed!")
                    }
                }
            }

            moduleLoaded = true
        }
    }

    fun queueEvent(runnable: Runnable) {
        mGLView?.queueEvent(runnable)
    }
}

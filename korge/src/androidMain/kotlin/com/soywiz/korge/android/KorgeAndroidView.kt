package com.soywiz.korge.android

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent
import android.widget.RelativeLayout
import com.soywiz.kds.Pool
import com.soywiz.kgl.KmlGl
import com.soywiz.kgl.KmlGlAndroid
import com.soywiz.klock.milliseconds
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

    private var mGLView: GLSurfaceView? = null
    private var agOpenGl: AGOpengl? = null
    private var gameWindow: AndroidGameWindowNoActivity? = null

    private val renderEvent = RenderEvent()
    private val initEvent = InitEvent()

    private var moduleLoaded = false

    inner class KorgeViewAGOpenGL : AGOpengl() {

        override val gl: KmlGl = KmlGlAndroid()
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
            gameWindow = AndroidGameWindowNoActivity(module.windowSize.width, module.windowSize.height, agOpenGl!!)

            mGLView = object : GLSurfaceView(context) {

                val view = this

                init {

                    var contextLost = false
                    var surfaceChanged = false
                    var initialized = false

                    setEGLContextClientVersion(2)

                    setRenderer(object : Renderer {
                        override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
                            contextLost = true
                        }

                        override fun onDrawFrame(unused: GL10) {

                            if (contextLost) {
                                contextLost = false
                                agOpenGl?.contextLost()
                            }

                            if (!initialized) {
                                initialized = true
                                gameWindow?.dispatch(initEvent)
                            }

                            if (surfaceChanged) {
                                surfaceChanged = false
                                gameWindow?.dispatchReshapeEvent(0, 0, view.width, view.height)
                            }

                            gameWindow?.coroutineDispatcher?.executePending(0.milliseconds)
                            gameWindow?.dispatch(renderEvent)
                        }

                        override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
                            surfaceChanged = true
                        }
                    })
                }

                private val touches = TouchEventHandler()
                private val coords = MotionEvent.PointerCoords()

                @SuppressLint("ClickableViewAccessibility")
                override fun onTouchEvent(ev: MotionEvent): Boolean {

                    val gameWindow = gameWindow ?: return false
                    val gameWindowCoroutineContext = gameWindow.coroutineContext ?: return false

                    touches.handleEvent(gameWindow, gameWindowCoroutineContext, when (ev.action) {
                        MotionEvent.ACTION_DOWN -> TouchEvent.Type.START
                        MotionEvent.ACTION_MOVE -> TouchEvent.Type.MOVE
                        MotionEvent.ACTION_UP -> TouchEvent.Type.END
                        else -> TouchEvent.Type.END
                    }, { currentTouchEvent ->
                        for (n in 0 until ev.pointerCount) {
                            ev.getPointerCoords(n, coords)
                            currentTouchEvent.touch(ev.getPointerId(n), coords.x.toDouble(), coords.y.toDouble())
                        }
                    })
                    return true
                }
            }

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

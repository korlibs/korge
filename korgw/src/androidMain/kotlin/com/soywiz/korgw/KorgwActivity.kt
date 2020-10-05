package com.soywiz.korgw

import android.app.Activity
import android.content.*
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.soywiz.kds.Pool
import com.soywiz.kgl.*
import com.soywiz.korag.AGOpengl
import com.soywiz.korev.*
import com.soywiz.korio.Korio
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import com.soywiz.korio.android.withAndroidContext

abstract class KorgwActivity : Activity() {
    var gameWindow: AndroidGameWindow = AndroidGameWindow(this)
    private var mGLView: GLSurfaceView? = null
    lateinit var ag: AGOpengl
    open val agCheck: Boolean get() = false
    open val agTrace: Boolean get() = false

    var fps: Int
        get() = gameWindow?.fps ?: 60
        set(value) {
            gameWindow?.fps = value
        }

    private var defaultUiVisibility = -1

    inner class KorgwActivityAGOpengl : AGOpengl() {
        //override val gl: KmlGl = CheckErrorsKmlGlProxy(KmlGlAndroid())
        override val gl: KmlGl = KmlGlAndroid().checkedIf(agCheck).logIf(agCheck)
        override val nativeComponent: Any get() = this@KorgwActivity
        override val gles: Boolean = true

        override fun repaint() {
            mGLView?.invalidate()
        }

        init {
            println("KorgwActivityAGOpengl: Created ag $this for ${this@KorgwActivity} with gl=$gl")
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        println("---------------- KorgwActivity.onCreate(savedInstanceState=$savedInstanceState) --------------")
        Log.e("KorgwActivity", "onCreate")
        //println("KorgwActivity.onCreate")

        //ag = AGOpenglFactory.create(this).create(this, AGConfig())
        ag = KorgwActivityAGOpengl()

        mGLView = object : GLSurfaceView(this) {
            val view = this

            init {
                println("KorgwActivity: Created GLSurfaceView $this for ${this@KorgwActivity}")

                setEGLContextClientVersion(2)
                setRenderer(object : GLSurfaceView.Renderer {
                    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
                        //GLES20.glClearColor(0.0f, 0.4f, 0.7f, 1.0f)
                        gameWindow.handleContextLost()
                    }

                    override fun onDrawFrame(unused: GL10) {
                        gameWindow.handleInitEventIfRequired()
                        gameWindow.handleReshapeEventIfRequired(0, 0, view.width, view.height)
                        gameWindow.frame()
                    }

                    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
                        println("---------------- GLSurfaceView.onSurfaceChanged($width, $height) --------------")
                        //ag.contextVersion++
                        //GLES20.glViewport(0, 0, width, height)
                        //surfaceChanged = true
                    }
                })
            }

            private val touches = TouchEventHandler()
            private val coords = MotionEvent.PointerCoords()

            override fun onTouchEvent(ev: MotionEvent): Boolean {
                val gameWindow = gameWindow ?: return false

                touches.handleEvent(gameWindow, gameWindow.coroutineContext, when (ev.action) {
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

        gameWindow.initializeAndroid()
        setContentView(mGLView)

        val androidContext = this
        Korio(androidContext) {
            try {
                kotlinx.coroutines.withContext(coroutineContext + gameWindow) {
                    withAndroidContext(androidContext) {
                        activityMain()
                    }
                }
            } finally {
                println("KorgwActivity.activityMain completed!")
            }
        }
    }

    override fun onResume() {
        //Looper.getMainLooper().
        println("---------------- KorgwActivity.onResume --------------")
        super.onResume()
        mGLView?.onResume()
        gameWindow?.dispatchResumeEvent()
    }

    override fun onPause() {
        println("---------------- KorgwActivity.onPause --------------")
        super.onPause()
        mGLView?.onPause()
        gameWindow?.dispatchPauseEvent()
    }

    override fun onStop() {
        println("---------------- KorgwActivity.onStop --------------")
        super.onStop()
        gameWindow?.dispatchStopEvent()
    }

    override fun onDestroy() {
        println("---------------- KorgwActivity.onDestroy --------------")
        super.onDestroy()
        mGLView?.onPause()
        //mGLView?.requestExitAndWait()
        //mGLView?.
        mGLView = null
        setContentView(android.view.View(this))
        gameWindow.dispatchDestroyEvent()
        //gameWindow?.close() // Do not close, since it will be automatically closed by the destroy event
    }

    data class ResultHandler(val request: Int) {
        var handler: (result: Int, data: Intent?) -> Unit = { result, data -> }
    }

    val resultHandlers = Pool { ResultHandler(it) }
    val handlers = LinkedHashMap<Int, ResultHandler>()

    fun registerActivityResult(handler: (result: Int, data: Intent?) -> Unit): Int {
        return resultHandlers.alloc().also {
            it.handler = handler
        }.request
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val handler = handlers.remove(requestCode)
        if (handler != null) {
            val callback = handler.handler
            resultHandlers.free(handler)
            callback(resultCode, data)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    abstract suspend fun activityMain(): Unit

    fun makeFullscreen(value: Boolean) {
        if (value) window.decorView.run {
            if (defaultUiVisibility == -1)
                defaultUiVisibility = systemUiVisibility
            val flags = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            systemUiVisibility = flags
            setOnSystemUiVisibilityChangeListener { visibility ->
                if ((visibility and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    systemUiVisibility = flags
                }
            }
        } else window.decorView.run {
            setOnSystemUiVisibilityChangeListener(null)
            systemUiVisibility = defaultUiVisibility
        }
    }
}


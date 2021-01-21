package com.soywiz.korgw

import android.app.Activity
import android.content.*
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.KeyEvent
import com.soywiz.kds.Pool
import com.soywiz.kgl.*
import com.soywiz.korag.AGOpengl
import com.soywiz.korev.*
import com.soywiz.korio.Korio
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import com.soywiz.korio.android.withAndroidContext
import com.soywiz.kds.toIntMap
import com.soywiz.korio.file.VfsFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import androidx.core.app.ActivityCompat.startActivityForResult

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

        // @TODO: Cache somehow?
        override val pixelsPerInch: Double get() = getResources().getDisplayMetrics().densityDpi.toDouble()

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

    suspend fun startActivityWithResult(intent: Intent, options: Bundle? = null): Intent? {
        val deferred = CompletableDeferred<Intent?>()
        val requestCode = registerActivityResult { result, data ->
            if (result == Activity.RESULT_OK) {
                deferred.complete(data)
            } else {
                deferred.completeExceptionally(CancellationException())
            }
        }
        startActivityForResult(this, intent, requestCode, options)
        return deferred.await()
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

    companion object {
        val KEY_MAP = mapOf(
            KeyEvent.KEYCODE_A to Key.A,
            KeyEvent.KEYCODE_B to Key.B,
            KeyEvent.KEYCODE_C to Key.C,
            KeyEvent.KEYCODE_D to Key.D,
            KeyEvent.KEYCODE_E to Key.E,
            KeyEvent.KEYCODE_F to Key.F,
            KeyEvent.KEYCODE_G to Key.G,
            KeyEvent.KEYCODE_H to Key.H,
            KeyEvent.KEYCODE_I to Key.I,
            KeyEvent.KEYCODE_J to Key.J,
            KeyEvent.KEYCODE_K to Key.K,
            KeyEvent.KEYCODE_L to Key.L,
            KeyEvent.KEYCODE_M to Key.M,
            KeyEvent.KEYCODE_N to Key.N,
            KeyEvent.KEYCODE_O to Key.O,
            KeyEvent.KEYCODE_P to Key.P,
            KeyEvent.KEYCODE_Q to Key.Q,
            KeyEvent.KEYCODE_R to Key.R,
            KeyEvent.KEYCODE_S to Key.S,
            KeyEvent.KEYCODE_T to Key.T,
            KeyEvent.KEYCODE_U to Key.U,
            KeyEvent.KEYCODE_V to Key.V,
            KeyEvent.KEYCODE_W to Key.W,
            KeyEvent.KEYCODE_X to Key.X,
            KeyEvent.KEYCODE_Y to Key.Y,
            KeyEvent.KEYCODE_Z to Key.Z,
            KeyEvent.KEYCODE_ENTER to Key.ENTER,
            KeyEvent.KEYCODE_ESCAPE to Key.ESCAPE,
            KeyEvent.KEYCODE_DPAD_LEFT to Key.LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT to Key.RIGHT,
            KeyEvent.KEYCODE_DPAD_UP to Key.UP,
            KeyEvent.KEYCODE_DPAD_DOWN to Key.DOWN,
            KeyEvent.KEYCODE_DPAD_CENTER to Key.RETURN,
            KeyEvent.KEYCODE_BACK to Key.BACKSPACE,
            KeyEvent.KEYCODE_DEL to Key.BACKSPACE,
            KeyEvent.KEYCODE_FORWARD_DEL to Key.DELETE,
            KeyEvent.KEYCODE_BUTTON_1 to Key.F1,
            KeyEvent.KEYCODE_BUTTON_2 to Key.F2,
            KeyEvent.KEYCODE_BUTTON_3 to Key.F3,
            KeyEvent.KEYCODE_BUTTON_4 to Key.F4,
            KeyEvent.KEYCODE_BUTTON_5 to Key.F5,
            KeyEvent.KEYCODE_BUTTON_6 to Key.F6,
            KeyEvent.KEYCODE_BUTTON_7 to Key.F7,
            KeyEvent.KEYCODE_BUTTON_8 to Key.F8,
            KeyEvent.KEYCODE_BUTTON_9 to Key.F9,
            KeyEvent.KEYCODE_BUTTON_10 to Key.F10,
            KeyEvent.KEYCODE_BUTTON_11 to Key.F11,
            KeyEvent.KEYCODE_BUTTON_12 to Key.F12,
            KeyEvent.KEYCODE_F1 to Key.F1,
            KeyEvent.KEYCODE_F2 to Key.F2,
            KeyEvent.KEYCODE_F3 to Key.F3,
            KeyEvent.KEYCODE_F4 to Key.F4,
            KeyEvent.KEYCODE_F5 to Key.F5,
            KeyEvent.KEYCODE_F6 to Key.F6,
            KeyEvent.KEYCODE_F7 to Key.F7,
            KeyEvent.KEYCODE_F8 to Key.F8,
            KeyEvent.KEYCODE_F9 to Key.F9,
            KeyEvent.KEYCODE_F10 to Key.F10,
            KeyEvent.KEYCODE_F11 to Key.F11,
            KeyEvent.KEYCODE_F12 to Key.F12,
        ).toIntMap()
    }

    fun onKey(keyCode: Int, event: KeyEvent, down: Boolean, long: Boolean): Boolean {
        gameWindow.dispatchKeyEventEx(
            when {
                down -> com.soywiz.korev.KeyEvent.Type.DOWN
                else -> com.soywiz.korev.KeyEvent.Type.UP
            }, 0,
            keyCode.toChar(),
            KEY_MAP[keyCode] ?: Key.UNKNOWN,
            keyCode,
            shift = false,
            ctrl = false,
            alt = false,
            meta = false
        )
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return onKey(keyCode, event, down = true, long = false)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return onKey(keyCode, event, down = false, long = false)
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        return onKey(keyCode, event, down = true, long = true)
    }

    override fun onKeyMultiple(keyCode: Int, repeatCount: Int, event: KeyEvent): Boolean {
        return super.onKeyMultiple(keyCode, repeatCount, event)
    }

    override fun onKeyShortcut(keyCode: Int, event: KeyEvent): Boolean {
        return super.onKeyShortcut(keyCode, event)
    }
}


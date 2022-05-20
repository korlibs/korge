package com.soywiz.korgw

import android.app.*
import android.content.*
import android.os.*
import android.util.*
import android.view.*
import android.view.KeyEvent
import com.soywiz.kds.*
import com.soywiz.kgl.*
import com.soywiz.kmem.hasBits
import com.soywiz.korag.gl.*
import com.soywiz.korev.*
import kotlin.coroutines.*

abstract class KorgwActivity(
    private val activityWithResult: ActivityWithResult.Mixin = ActivityWithResult.Mixin(),
    val config: GameWindowCreationConfig = GameWindowCreationConfig(),
) : Activity(), ActivityWithResult by activityWithResult
//, DialogInterface.OnKeyListener
{
    init {
        activityWithResult.activity = this
    }

    var gameWindow: AndroidGameWindow = AndroidGameWindow(this)
    var mGLView: KorgwSurfaceView? = null
    lateinit var ag: AGOpengl
    open val agCheck: Boolean get() = false
    open val agTrace: Boolean get() = false

    //init { setOnKeyListener(this) }
    //override fun onKey(dialog: DialogInterface, keyCode: Int, event: KeyEvent): Boolean = false

    var fps: Int
        get() = gameWindow?.fps ?: 60
        set(value) {
            gameWindow?.fps = value
        }

    private var defaultUiVisibility = -1

    inner class KorgwActivityAGOpengl : AGOpengl() {
        //override val gl: KmlGl = CheckErrorsKmlGlProxy(KmlGlAndroid())
        override val gl: KmlGl = KmlGlAndroid({ mGLView?.clientVersion ?: -1 }).checkedIf(agCheck).logIf(agCheck)
        override val nativeComponent: Any get() = this@KorgwActivity

        override fun repaint() {
            mGLView?.invalidate()
        }

        // @TODO: Cache somehow?
        override val pixelsPerInch: Double get() = resources.displayMetrics.densityDpi.toDouble()

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

        mGLView = KorgwSurfaceView(this, this, gameWindow)

        gameWindow.initializeAndroid()
        setContentView(mGLView)

        mGLView!!.onDraw.once {
            suspend {
                activityMain()
            }.startCoroutine(object : Continuation<Unit> {
                override val context: CoroutineContext get() = com.soywiz.korio.android.AndroidCoroutineContext(this@KorgwActivity) + gameWindow

                override fun resumeWith(result: Result<Unit>) {
                    println("KorgwActivity.activityMain completed! result=$result")
                }
            })
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        println("---------------- KorgwActivity.onSaveInstanceState(outState=$outState) --------------")
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        println("---------------- KorgwActivity.onRestoreInstanceState(savedInstanceState=$savedInstanceState) --------------")
        super.onRestoreInstanceState(savedInstanceState)
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
        gameWindow.queue {
            gameWindow.dispatchDestroyEvent()
        }
        //gameWindow?.close() // Do not close, since it will be automatically closed by the destroy event
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!activityWithResult.tryHandleActivityResult(requestCode, resultCode, data)) {
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


    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
        mGLView?.dispatchKeyEvent(event) ?: super.onKeyDown(keyCode, event)

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean =
        mGLView?.dispatchKeyEvent(event) ?: super.onKeyUp(keyCode, event)

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean =
        mGLView?.dispatchKeyEvent(event) ?: super.onKeyLongPress(keyCode, event)

    override fun onKeyMultiple(keyCode: Int, repeatCount: Int, event: KeyEvent): Boolean =
        mGLView?.dispatchKeyEvent(event) ?: super.onKeyMultiple(keyCode, repeatCount, event)

    override fun onKeyShortcut(keyCode: Int, event: KeyEvent): Boolean =
        mGLView?.dispatchKeyShortcutEvent(event) ?: super.onKeyShortcut(keyCode, event)

    override fun onTrackballEvent(event: MotionEvent): Boolean =
        mGLView?.dispatchTrackballEvent(event) ?: super.onTrackballEvent(event)

    override fun onGenericMotionEvent(event: MotionEvent): Boolean =
        mGLView?.dispatchGenericMotionEvent(event) ?: super.onGenericMotionEvent(event)

    override fun onBackPressed() {
        gameWindow.queue {
            if (!gameWindow.dispatchKeyEventEx(com.soywiz.korev.KeyEvent.Type.DOWN, 0, '\u0008', Key.BACKSPACE, KeyEvent.KEYCODE_BACK)) {
                runOnUiThread {
                    super.onBackPressed()
                }
            }
        }
    }
}

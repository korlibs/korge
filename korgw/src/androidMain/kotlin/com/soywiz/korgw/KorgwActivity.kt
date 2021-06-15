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
import android.content.DialogInterface
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import com.soywiz.korio.android.withAndroidContext
import com.soywiz.kds.toIntMap
import com.soywiz.korio.file.VfsFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import androidx.core.app.ActivityCompat.startActivityForResult
import com.soywiz.klock.*
import kotlin.coroutines.*

abstract class KorgwActivity : Activity()
    //, DialogInterface.OnKeyListener
{
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

            KeyEvent.KEYCODE_0 to Key.N0,
            KeyEvent.KEYCODE_1 to Key.N1,
            KeyEvent.KEYCODE_2 to Key.N2,
            KeyEvent.KEYCODE_3 to Key.N3,
            KeyEvent.KEYCODE_4 to Key.N4,
            KeyEvent.KEYCODE_5 to Key.N5,
            KeyEvent.KEYCODE_6 to Key.N6,
            KeyEvent.KEYCODE_7 to Key.N7,
            KeyEvent.KEYCODE_8 to Key.N8,
            KeyEvent.KEYCODE_9 to Key.N9,
            KeyEvent.KEYCODE_11 to Key.N11,
            KeyEvent.KEYCODE_12 to Key.N12,

            KeyEvent.KEYCODE_3D_MODE to Key.N3D_MODE,

            KeyEvent.KEYCODE_ALT_LEFT to Key.LEFT_ALT,
            KeyEvent.KEYCODE_ALT_RIGHT to Key.RIGHT_ALT,
            KeyEvent.KEYCODE_CTRL_LEFT to Key.LEFT_CONTROL,
            KeyEvent.KEYCODE_CTRL_RIGHT to Key.RIGHT_CONTROL,
            KeyEvent.KEYCODE_META_LEFT to Key.LEFT_SUPER,
            KeyEvent.KEYCODE_META_RIGHT to Key.RIGHT_SUPER,
            KeyEvent.KEYCODE_SHIFT_LEFT to Key.LEFT_SHIFT,
            KeyEvent.KEYCODE_SHIFT_RIGHT to Key.RIGHT_SHIFT,
            KeyEvent.KEYCODE_SOFT_LEFT to Key.SOFT_LEFT,
            KeyEvent.KEYCODE_SOFT_RIGHT to Key.SOFT_RIGHT,

            KeyEvent.KEYCODE_APOSTROPHE to Key.APOSTROPHE,
            KeyEvent.KEYCODE_APP_SWITCH to Key.APP_SWITCH,
            KeyEvent.KEYCODE_ASSIST to Key.ASSIST,
            KeyEvent.KEYCODE_AT to Key.AT,
            KeyEvent.KEYCODE_AVR_INPUT to Key.AVR_INPUT,
            KeyEvent.KEYCODE_AVR_POWER to Key.AVR_POWER,

            KeyEvent.KEYCODE_DEL to Key.BACKSPACE,
            KeyEvent.KEYCODE_FORWARD_DEL to Key.DELETE,

            KeyEvent.KEYCODE_BACK to Key.BACK,
            KeyEvent.KEYCODE_BACKSLASH to Key.BACKSLASH,
            KeyEvent.KEYCODE_BOOKMARK to Key.BOOKMARK,
            KeyEvent.KEYCODE_BREAK to Key.BREAK,

            KeyEvent.KEYCODE_BRIGHTNESS_DOWN to Key.BRIGHTNESS_DOWN,
            KeyEvent.KEYCODE_BRIGHTNESS_UP to Key.BRIGHTNESS_UP,

            KeyEvent.KEYCODE_BUTTON_1 to Key.XBUTTON1,
            KeyEvent.KEYCODE_BUTTON_2 to Key.XBUTTON2,
            KeyEvent.KEYCODE_BUTTON_3 to Key.XBUTTON3,
            KeyEvent.KEYCODE_BUTTON_4 to Key.XBUTTON4,
            KeyEvent.KEYCODE_BUTTON_5 to Key.XBUTTON5,
            KeyEvent.KEYCODE_BUTTON_6 to Key.XBUTTON6,
            KeyEvent.KEYCODE_BUTTON_7 to Key.XBUTTON7,
            KeyEvent.KEYCODE_BUTTON_8 to Key.XBUTTON8,
            KeyEvent.KEYCODE_BUTTON_9 to Key.XBUTTON9,
            KeyEvent.KEYCODE_BUTTON_10 to Key.XBUTTON10,
            KeyEvent.KEYCODE_BUTTON_11 to Key.XBUTTON11,
            KeyEvent.KEYCODE_BUTTON_12 to Key.XBUTTON12,
            KeyEvent.KEYCODE_BUTTON_13 to Key.XBUTTON13,
            KeyEvent.KEYCODE_BUTTON_14 to Key.XBUTTON14,
            KeyEvent.KEYCODE_BUTTON_15 to Key.XBUTTON15,
            KeyEvent.KEYCODE_BUTTON_16 to Key.XBUTTON16,
            KeyEvent.KEYCODE_BUTTON_A to Key.XBUTTON_A,
            KeyEvent.KEYCODE_BUTTON_B to Key.XBUTTON_B,
            KeyEvent.KEYCODE_BUTTON_C to Key.XBUTTON_C,
            KeyEvent.KEYCODE_BUTTON_L1 to Key.XBUTTON_L1,
            KeyEvent.KEYCODE_BUTTON_L2 to Key.XBUTTON_L2,
            KeyEvent.KEYCODE_BUTTON_MODE to Key.XBUTTON_MODE,
            KeyEvent.KEYCODE_BUTTON_R1 to Key.XBUTTON_R1,
            KeyEvent.KEYCODE_BUTTON_R2 to Key.XBUTTON_R2,
            KeyEvent.KEYCODE_BUTTON_SELECT to Key.XBUTTON_SELECT,
            KeyEvent.KEYCODE_BUTTON_START to Key.XBUTTON_START,
            KeyEvent.KEYCODE_BUTTON_THUMBL to Key.XBUTTON_THUMBL,
            KeyEvent.KEYCODE_BUTTON_THUMBR to Key.XBUTTON_THUMBR,
            KeyEvent.KEYCODE_BUTTON_X to Key.XBUTTON_X,
            KeyEvent.KEYCODE_BUTTON_Y to Key.XBUTTON_Y,
            KeyEvent.KEYCODE_BUTTON_Z to Key.XBUTTON_Z,

            KeyEvent.KEYCODE_CALCULATOR to Key.CALCULATOR,
            KeyEvent.KEYCODE_CALENDAR to Key.CALENDAR,
            KeyEvent.KEYCODE_CALL to Key.CALL,
            KeyEvent.KEYCODE_CAMERA to Key.CAMERA,
            KeyEvent.KEYCODE_CAPS_LOCK to Key.CAPS_LOCK,
            KeyEvent.KEYCODE_CAPTIONS to Key.CAPTIONS,
            KeyEvent.KEYCODE_CHANNEL_DOWN to Key.CHANNEL_DOWN,
            KeyEvent.KEYCODE_CHANNEL_UP to Key.CHANNEL_UP,
            KeyEvent.KEYCODE_CLEAR to Key.CLEAR,
            KeyEvent.KEYCODE_COMMA to Key.COMMA,
            KeyEvent.KEYCODE_CONTACTS to Key.CONTACTS,
            KeyEvent.KEYCODE_COPY to Key.COPY,
            KeyEvent.KEYCODE_CUT to Key.CUT,

            KeyEvent.KEYCODE_DPAD_LEFT to Key.LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT to Key.RIGHT,
            KeyEvent.KEYCODE_DPAD_UP to Key.UP,
            KeyEvent.KEYCODE_DPAD_DOWN to Key.DOWN,
            KeyEvent.KEYCODE_DPAD_CENTER to Key.RETURN,

            KeyEvent.KEYCODE_DPAD_DOWN_LEFT to Key.DPAD_DOWN_LEFT,
            KeyEvent.KEYCODE_DPAD_DOWN_RIGHT to Key.DPAD_DOWN_RIGHT,
            KeyEvent.KEYCODE_DPAD_UP_LEFT to Key.DPAD_UP_LEFT,
            KeyEvent.KEYCODE_DPAD_UP_RIGHT to Key.DPAD_UP_RIGHT,

            KeyEvent.KEYCODE_DVR to Key.DVR,
            KeyEvent.KEYCODE_EISU to Key.EISU,
            KeyEvent.KEYCODE_ENDCALL to Key.ENDCALL,
            KeyEvent.KEYCODE_ENTER to Key.ENTER,
            KeyEvent.KEYCODE_ENVELOPE to Key.ENVELOPE,
            KeyEvent.KEYCODE_EQUALS to Key.KP_EQUAL,
            KeyEvent.KEYCODE_ESCAPE to Key.ESCAPE,
            KeyEvent.KEYCODE_EXPLORER to Key.EXPLORER,

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

            KeyEvent.KEYCODE_FOCUS to Key.FOCUS,
            KeyEvent.KEYCODE_FORWARD to Key.FORWARD,
            KeyEvent.KEYCODE_FUNCTION to Key.FUNCTION,
            KeyEvent.KEYCODE_GRAVE to Key.GRAVE,
            KeyEvent.KEYCODE_GUIDE to Key.GUIDE,
            KeyEvent.KEYCODE_HEADSETHOOK to Key.HEADSETHOOK,
            KeyEvent.KEYCODE_HELP to Key.HELP,
            KeyEvent.KEYCODE_HENKAN to Key.HENKAN,
            KeyEvent.KEYCODE_HOME to Key.HOME,
            KeyEvent.KEYCODE_INFO to Key.INFO,
            KeyEvent.KEYCODE_INSERT to Key.INSERT,
            KeyEvent.KEYCODE_KANA to Key.KANA,
            KeyEvent.KEYCODE_KATAKANA_HIRAGANA to Key.KATAKANA_HIRAGANA,
            KeyEvent.KEYCODE_LANGUAGE_SWITCH to Key.LANGUAGE_SWITCH,
            KeyEvent.KEYCODE_LAST_CHANNEL to Key.LAST_CHANNEL,
            KeyEvent.KEYCODE_LEFT_BRACKET to Key.LEFT_BRACKET,
            KeyEvent.KEYCODE_MANNER_MODE to Key.MANNER_MODE,
            KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK to Key.MEDIA_AUDIO_TRACK,
            KeyEvent.KEYCODE_MEDIA_CLOSE to Key.MEDIA_CLOSE,
            KeyEvent.KEYCODE_MEDIA_EJECT to Key.MEDIA_EJECT,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD to Key.MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_MEDIA_NEXT to Key.MEDIA_NEXT_TRACK,
            KeyEvent.KEYCODE_MEDIA_PAUSE to Key.MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY to Key.MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE to Key.MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS to Key.MEDIA_PREV_TRACK,
            KeyEvent.KEYCODE_MEDIA_RECORD to Key.MEDIA_RECORD,
            KeyEvent.KEYCODE_MEDIA_REWIND to Key.MEDIA_REWIND,
            KeyEvent.KEYCODE_MEDIA_SKIP_BACKWARD to Key.MEDIA_SKIP_BACKWARD,
            KeyEvent.KEYCODE_MEDIA_SKIP_FORWARD to Key.MEDIA_SKIP_FORWARD,
            KeyEvent.KEYCODE_MEDIA_STEP_BACKWARD to Key.MEDIA_STEP_BACKWARD,
            KeyEvent.KEYCODE_MEDIA_STEP_FORWARD to Key.MEDIA_STEP_FORWARD,
            KeyEvent.KEYCODE_MEDIA_STOP to Key.MEDIA_STOP,
            KeyEvent.KEYCODE_MEDIA_TOP_MENU to Key.MEDIA_TOP_MENU,
            KeyEvent.KEYCODE_MENU to Key.MENU,
            KeyEvent.KEYCODE_MINUS to Key.MINUS,
            KeyEvent.KEYCODE_MOVE_END to Key.END,
            KeyEvent.KEYCODE_MOVE_HOME to Key.HOME,
            KeyEvent.KEYCODE_MUHENKAN to Key.MUHENKAN,
            KeyEvent.KEYCODE_MUSIC to Key.MUSIC,
            KeyEvent.KEYCODE_MUTE to Key.MUTE,
            KeyEvent.KEYCODE_NAVIGATE_IN to Key.NAVIGATE_IN,
            KeyEvent.KEYCODE_NAVIGATE_NEXT to Key.NAVIGATE_NEXT,
            KeyEvent.KEYCODE_NAVIGATE_OUT to Key.NAVIGATE_OUT,
            KeyEvent.KEYCODE_NAVIGATE_PREVIOUS to Key.NAVIGATE_PREVIOUS,
            KeyEvent.KEYCODE_NOTIFICATION to Key.NOTIFICATION,
            KeyEvent.KEYCODE_NUM to Key.NUM,
            KeyEvent.KEYCODE_NUMPAD_0 to Key.NUMPAD0,
            KeyEvent.KEYCODE_NUMPAD_1 to Key.NUMPAD1,
            KeyEvent.KEYCODE_NUMPAD_2 to Key.NUMPAD2,
            KeyEvent.KEYCODE_NUMPAD_3 to Key.NUMPAD3,
            KeyEvent.KEYCODE_NUMPAD_4 to Key.NUMPAD4,
            KeyEvent.KEYCODE_NUMPAD_5 to Key.NUMPAD5,
            KeyEvent.KEYCODE_NUMPAD_6 to Key.NUMPAD6,
            KeyEvent.KEYCODE_NUMPAD_7 to Key.NUMPAD7,
            KeyEvent.KEYCODE_NUMPAD_8 to Key.NUMPAD8,
            KeyEvent.KEYCODE_NUMPAD_9 to Key.NUMPAD9,
            KeyEvent.KEYCODE_NUMPAD_ADD to Key.KP_ADD,
            KeyEvent.KEYCODE_NUMPAD_COMMA to Key.KP_COMMA,
            KeyEvent.KEYCODE_NUMPAD_DIVIDE to Key.KP_DIVIDE,
            KeyEvent.KEYCODE_NUMPAD_DOT to Key.KP_DOT,
            KeyEvent.KEYCODE_NUMPAD_ENTER to Key.KP_DIVIDE,
            KeyEvent.KEYCODE_NUMPAD_EQUALS to Key.KP_EQUAL,
            KeyEvent.KEYCODE_NUMPAD_LEFT_PAREN to Key.KP_LEFT_PAREN,
            KeyEvent.KEYCODE_NUMPAD_MULTIPLY to Key.KP_MULTIPLY,
            KeyEvent.KEYCODE_NUMPAD_RIGHT_PAREN to Key.KP_RIGHT_PAREN,
            KeyEvent.KEYCODE_NUMPAD_SUBTRACT to Key.KP_SUBTRACT,
            KeyEvent.KEYCODE_NUM_LOCK to Key.NUM_LOCK,
            KeyEvent.KEYCODE_PAGE_DOWN to Key.PAGE_DOWN,
            KeyEvent.KEYCODE_PAGE_UP to Key.PAGE_UP,
            KeyEvent.KEYCODE_PAIRING to Key.PAIRING,
            KeyEvent.KEYCODE_PASTE to Key.PASTE,
            KeyEvent.KEYCODE_PERIOD to Key.PERIOD,
            KeyEvent.KEYCODE_PICTSYMBOLS to Key.PICTSYMBOLS,
            KeyEvent.KEYCODE_PLUS to Key.PLUS,
            KeyEvent.KEYCODE_POUND to Key.POUND,
            KeyEvent.KEYCODE_POWER to Key.POWER,
            KeyEvent.KEYCODE_PROG_BLUE to Key.PROG_BLUE,
            KeyEvent.KEYCODE_PROG_GREEN to Key.PROG_GREEN,
            KeyEvent.KEYCODE_PROG_RED to Key.PROG_RED,
            KeyEvent.KEYCODE_PROG_YELLOW to Key.PROG_YELLOW,
            KeyEvent.KEYCODE_RIGHT_BRACKET to Key.RIGHT_BRACKET,
            KeyEvent.KEYCODE_RO to Key.RO,
            KeyEvent.KEYCODE_SCROLL_LOCK to Key.SCROLL_LOCK,
            KeyEvent.KEYCODE_SEARCH to Key.SEARCH,
            KeyEvent.KEYCODE_SEMICOLON to Key.SEMICOLON,
            KeyEvent.KEYCODE_SETTINGS to Key.SETTINGS,
            KeyEvent.KEYCODE_SLASH to Key.SLASH,
            KeyEvent.KEYCODE_SLEEP to Key.SLEEP,
            KeyEvent.KEYCODE_SOFT_SLEEP to Key.SOFT_SLEEP,
            KeyEvent.KEYCODE_SPACE to Key.SPACE,
            KeyEvent.KEYCODE_STAR to Key.STAR,
            KeyEvent.KEYCODE_STB_INPUT to Key.STB_INPUT,
            KeyEvent.KEYCODE_STB_POWER to Key.STB_POWER,
            KeyEvent.KEYCODE_STEM_1 to Key.STEM_1,
            KeyEvent.KEYCODE_STEM_2 to Key.STEM_2,
            KeyEvent.KEYCODE_STEM_3 to Key.STEM_3,
            KeyEvent.KEYCODE_STEM_PRIMARY to Key.STEM_PRIMARY,
            KeyEvent.KEYCODE_SWITCH_CHARSET to Key.SWITCH_CHARSET,
            KeyEvent.KEYCODE_SYM to Key.SYM,
            KeyEvent.KEYCODE_SYSRQ to Key.SYSRQ,
            KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN to Key.SYSTEM_NAVIGATION_DOWN,
            KeyEvent.KEYCODE_SYSTEM_NAVIGATION_LEFT to Key.SYSTEM_NAVIGATION_LEFT,
            KeyEvent.KEYCODE_SYSTEM_NAVIGATION_RIGHT to Key.SYSTEM_NAVIGATION_RIGHT,
            KeyEvent.KEYCODE_SYSTEM_NAVIGATION_UP to Key.SYSTEM_NAVIGATION_UP,
            KeyEvent.KEYCODE_TAB to Key.TAB,
            KeyEvent.KEYCODE_TV to Key.TV,
            KeyEvent.KEYCODE_TV_ANTENNA_CABLE to Key.TV_ANTENNA_CABLE,
            KeyEvent.KEYCODE_TV_AUDIO_DESCRIPTION to Key.TV_AUDIO_DESCRIPTION,
            KeyEvent.KEYCODE_TV_AUDIO_DESCRIPTION_MIX_DOWN to Key.TV_AUDIO_DESCRIPTION_MIX_DOWN,
            KeyEvent.KEYCODE_TV_AUDIO_DESCRIPTION_MIX_UP to Key.TV_AUDIO_DESCRIPTION_MIX_UP,
            KeyEvent.KEYCODE_TV_CONTENTS_MENU to Key.TV_CONTENTS_MENU,
            KeyEvent.KEYCODE_TV_DATA_SERVICE to Key.TV_DATA_SERVICE,
            KeyEvent.KEYCODE_TV_INPUT to Key.TV_INPUT,
            KeyEvent.KEYCODE_TV_INPUT_COMPONENT_1 to Key.TV_INPUT_COMPONENT_1,
            KeyEvent.KEYCODE_TV_INPUT_COMPONENT_2 to Key.TV_INPUT_COMPONENT_2,
            KeyEvent.KEYCODE_TV_INPUT_COMPOSITE_1 to Key.TV_INPUT_COMPOSITE_1,
            KeyEvent.KEYCODE_TV_INPUT_COMPOSITE_2 to Key.TV_INPUT_COMPOSITE_2,
            KeyEvent.KEYCODE_TV_INPUT_HDMI_1 to Key.TV_INPUT_HDMI_1,
            KeyEvent.KEYCODE_TV_INPUT_HDMI_2 to Key.TV_INPUT_HDMI_2,
            KeyEvent.KEYCODE_TV_INPUT_HDMI_3 to Key.TV_INPUT_HDMI_3,
            KeyEvent.KEYCODE_TV_INPUT_HDMI_4 to Key.TV_INPUT_HDMI_4,
            KeyEvent.KEYCODE_TV_INPUT_VGA_1 to Key.TV_INPUT_VGA_1,
            KeyEvent.KEYCODE_TV_MEDIA_CONTEXT_MENU to Key.TV_MEDIA_CONTEXT_MENU,
            KeyEvent.KEYCODE_TV_NETWORK to Key.TV_NETWORK,
            KeyEvent.KEYCODE_TV_NUMBER_ENTRY to Key.TV_NUMBER_ENTRY,
            KeyEvent.KEYCODE_TV_POWER to Key.TV_POWER,
            KeyEvent.KEYCODE_TV_RADIO_SERVICE to Key.TV_RADIO_SERVICE,
            KeyEvent.KEYCODE_TV_SATELLITE to Key.TV_SATELLITE,
            KeyEvent.KEYCODE_TV_SATELLITE_BS to Key.TV_SATELLITE_BS,
            KeyEvent.KEYCODE_TV_SATELLITE_CS to Key.TV_SATELLITE_CS,
            KeyEvent.KEYCODE_TV_SATELLITE_SERVICE to Key.TV_SATELLITE_SERVICE,
            KeyEvent.KEYCODE_TV_TELETEXT to Key.TV_TELETEXT,
            KeyEvent.KEYCODE_TV_TERRESTRIAL_ANALOG to Key.TV_TERRESTRIAL_ANALOG,
            KeyEvent.KEYCODE_TV_TERRESTRIAL_DIGITAL to Key.TV_TERRESTRIAL_DIGITAL,
            KeyEvent.KEYCODE_TV_TIMER_PROGRAMMING to Key.TV_TIMER_PROGRAMMING,
            KeyEvent.KEYCODE_TV_ZOOM_MODE to Key.TV_ZOOM_MODE,
            KeyEvent.KEYCODE_UNKNOWN to Key.UNKNOWN,
            KeyEvent.KEYCODE_VOICE_ASSIST to Key.VOICE_ASSIST,
            KeyEvent.KEYCODE_VOLUME_DOWN to Key.VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE to Key.VOLUME_MUTE,
            KeyEvent.KEYCODE_VOLUME_UP to Key.VOLUME_UP,
            KeyEvent.KEYCODE_WAKEUP to Key.WAKEUP,
            KeyEvent.KEYCODE_WINDOW to Key.WINDOW,
            KeyEvent.KEYCODE_YEN to Key.YEN,
            KeyEvent.KEYCODE_ZENKAKU_HANKAKU to Key.ZENKAKU_HANKAKU,
            KeyEvent.KEYCODE_ZOOM_IN to Key.ZOOM_IN,
            KeyEvent.KEYCODE_ZOOM_OUT to Key.ZOOM_OUT,
        ).toIntMap()
    }

    fun onKey(keyCode: Int, event: KeyEvent, type: com.soywiz.korev.KeyEvent.Type, long: Boolean): Boolean {
        gameWindow.dispatchKeyEventEx(
            type, 0,
            keyCode.toChar(),
            KEY_MAP[keyCode] ?: Key.UNKNOWN,
            keyCode,
            shift = event.isShiftPressed(),
            ctrl = event.isCtrlPressed(),
            alt = event.isAltPressed(),
            meta = event.isMetaPressed()
        )
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        //println("Android.onKeyDown:$keyCode,${event.getUnicodeChar()}")
        return onKey(keyCode, event, type = com.soywiz.korev.KeyEvent.Type.DOWN, long = false)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        //println("Android.onKeyUp:$keyCode,${event.getUnicodeChar()}")
        onKey(keyCode, event, type = com.soywiz.korev.KeyEvent.Type.UP, long = false)
        val unicodeChar = event.getUnicodeChar()
        if (unicodeChar != 0) {
            onKey(unicodeChar, event, type = com.soywiz.korev.KeyEvent.Type.TYPE, long = false)
        }
        return true
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean {
        //println("Android.onKeyLongPress:$keyCode,${event.getUnicodeChar()}")
        return onKey(keyCode, event, type = com.soywiz.korev.KeyEvent.Type.DOWN, long = true)
    }

    override fun onKeyMultiple(keyCode: Int, repeatCount: Int, event: KeyEvent): Boolean {
        //println("Android.onKeyMultiple:$keyCode,$repeatCount,${event.getUnicodeChar()},$event")
        for (char in event.characters) {
            onKey(char.toInt(), event, type = com.soywiz.korev.KeyEvent.Type.TYPE, long = false)
        }
        return true
    }

    override fun onKeyShortcut(keyCode: Int, event: KeyEvent): Boolean {
        println("Android.onKeyShortcut:$keyCode")
        return super.onKeyShortcut(keyCode, event)
    }
}


package com.soywiz.korgw

import android.app.Activity
import android.content.*
import android.view.*
import android.view.inputmethod.*
import com.soywiz.korag.*
import com.soywiz.korev.ISoftKeyboardConfig
import com.soywiz.korev.SoftKeyboardConfig
import com.soywiz.korim.bitmap.*
import com.soywiz.korio.async.Signal
import kotlin.coroutines.*
import kotlin.properties.Delegates

actual fun CreateDefaultGameWindow(config: GameWindowCreationConfig): GameWindow = TODO()

abstract class BaseAndroidGameWindow(
    val config: GameWindowCreationConfig = GameWindowCreationConfig(),
) : GameWindow() {
    abstract val androidContext: Context
    abstract val androidView: View

    // @TODO: Cache somehow?
    override val pixelsPerInch: Double get() = androidContext.resources.displayMetrics.xdpi.toDouble()

    val context get() = androidContext
    var coroutineContext: CoroutineContext? = null
    val Context?.activity: Activity? get() = when (this) {
        is Activity -> this
        is ContextWrapper -> this.baseContext.activity
        else -> null
    }
    val _activity: Activity? get() = context.activity

    val inputMethodManager get() = androidContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    override val dialogInterface = DialogInterfaceAndroid { androidContext }
    override var isSoftKeyboardVisible: Boolean = false

    override var keepScreenOn: Boolean
        set(value) {
            val flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            _activity?.window?.setFlags(if (value) -1 else 0, flags)
        }
        get() = ((_activity?.window?.attributes?.flags ?: 0) and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0

    override fun showSoftKeyboard(force: Boolean, config: ISoftKeyboardConfig?) {
        isSoftKeyboardVisible = true
        println("Korgw.BaseAndroidGameWindow.showSoftKeyboard:force=$force")
        try {
            //inputMethodManager.showSoftInput(androidView, if (force) InputMethodManager.SHOW_FORCED else 0)
            if (force) {
                inputMethodManager.toggleSoftInput(
                    InputMethodManager.SHOW_FORCED,
                    InputMethodManager.HIDE_IMPLICIT_ONLY
                )
            }

            //val hasHardKeyboard = context.resources.configuration.hasHardKeyboard()
            //val hardKeyboardInUse = context.resources.configuration.isHardKeyboardInUse()
            //if (hasHardKeyboard && !hardKeyboardInUse) {
            //    inputMethodManager.showSoftInput(androidView, InputMethodManager.SHOW_FORCED)
            //} else if (!hasHardKeyboard) {
            //    inputMethodManager.showSoftInput(androidView, InputMethodManager.SHOW_IMPLICIT)
            //}

            //inputMethodManager.showSoftInput(androidView, InputMethodManager.SHOW_FORCED)
            inputMethodManager.showSoftInput(androidView, InputMethodManager.SHOW_IMPLICIT)

        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun hideSoftKeyboard() {
        println("Korgw.BaseAndroidGameWindow.hideSoftKeyboard")
        isSoftKeyboardVisible = false
        try {
            inputMethodManager.hideSoftInputFromWindow(androidView.getWindowToken(), 0)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    //fun <T : Any> queueAndWait(callback: () -> T): T {
    //    var result: Result<T>? = null
    //    val semaphore = java.util.concurrent.Semaphore(0)
    //    coroutineDispatcher.queue {
    //        result = kotlin.runCatching { callback() }
    //        semaphore.release()
    //    }
    //    semaphore.acquire()
    //    return result!!.getOrThrow()
    //}
}

class AndroidGameWindow(val activity: KorgwActivity, config: GameWindowCreationConfig = activity.config) : BaseAndroidGameWindow(config) {
    override val androidContext get() = activity
    override val androidView: View get() = activity.mGLView ?: error("Can't find mGLView")

    val mainHandler by lazy { android.os.Handler(androidContext.mainLooper) }

    override val ag: AG get() = activity.ag

    private var _setTitle: String? = null
    override var title: String; get() = _setTitle ?: activity.title.toString(); set(value) { _setTitle = value; mainHandler.post { activity.title = value } }
    override val width: Int get() = activity.window.decorView.width
    override val height: Int get() = activity.window.decorView.height
    override var icon: Bitmap?
        get() = super.icon
        set(value) {}
    override var fullscreen: Boolean = true
        set(value) {
            field = value
            activity.makeFullscreen(value)
        }
    override var visible: Boolean
        get() = super.visible
        set(value) {}
    override var quality: Quality
        get() = super.quality
        set(value) {}

    fun initializeAndroid() {
        fullscreen = true
    }

    override fun setSize(width: Int, height: Int) {
    }


    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        this.coroutineContext = kotlin.coroutines.coroutineContext
        //println("CONTEXT: ${kotlin.coroutines.coroutineContext[AndroidCoroutineContext.Key]?.context}")
        entry(this)
    }
}

class AndroidGameWindowNoActivity(
    override val width: Int,
    override val height: Int,
    override val ag: AG,
    override val androidContext: Context,
    config: GameWindowCreationConfig = GameWindowCreationConfig(),
    val getView: () -> View
) : BaseAndroidGameWindow(config) {
    override val dialogInterface = DialogInterfaceAndroid { androidContext }

    override val androidView: View get() = getView()
    override var title: String = "Korge"

    override var icon: Bitmap?
        get() = super.icon
        set(value) {}

    override var fullscreen: Boolean
        get() = true
        set(value) {}

    override var visible: Boolean
        get() = super.visible
        set(value) {}

    override var quality: Quality
        get() = super.quality
        set(value) {}

    override suspend fun loop(entry: suspend GameWindow.() -> Unit) {
        this.coroutineContext = kotlin.coroutines.coroutineContext
        entry(this)
    }
}

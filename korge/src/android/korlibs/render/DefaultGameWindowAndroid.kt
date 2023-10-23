package korlibs.render

import android.app.*
import android.content.*
import android.os.*
import android.view.*
import android.view.inputmethod.*
import korlibs.event.*
import korlibs.graphics.*
import korlibs.image.bitmap.*
import korlibs.io.android.*
import korlibs.io.lang.TimedCache
import korlibs.time.*
import kotlinx.coroutines.*


actual fun CreateDefaultGameWindow(config: GameWindowCreationConfig): GameWindow = TODO()

interface AndroidContextHolder {
    val androidContext: Context
}

abstract class BaseAndroidGameWindow(
    final override val androidContext: Context,
    val config: GameWindowCreationConfig = GameWindowCreationConfig(),
) : GameWindow(), AndroidContextHolder {
    final override val dialogInterface = DialogInterfaceAndroid { androidContext }
    override val androidContextAny: Any? = androidContext
    abstract val androidView: View
    override val pixelsPerInch: Double by TimedCache(1.seconds) { androidContext.resources.displayMetrics.xdpi.toDouble() }
    val _activity: Activity? get() = androidContext.activity

    init {
        coroutineContext += AndroidCoroutineContext(androidContext)
    }

    val Context?.activity: Activity? get() = when (this) {
        is Activity -> this
        is ContextWrapper -> this.baseContext.activity
        else -> null
    }

    val inputMethodManager get() = androidContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
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

val GameWindow.gameWindowAndroidContext: Context get() = androidContextAny as Context
val GameWindow.gameWindowAndroidContextOrNull: Context? get() = androidContextAny as? Context?

fun runAndroidOnUiThread(context: Context? = null, block: () -> Unit) {
    //Looper.prepare()
    Handler(context?.mainLooper ?: Looper.getMainLooper()).post(block)
}

suspend fun <T> runAndroidOnUiThreadSuspend(context: Context? = null, block: () -> T): T {
    val deferred = CompletableDeferred<T>()
    runAndroidOnUiThread(context) {
        deferred.completeWith(runCatching { block() })
    }
    return deferred.await()
}

class AndroidGameWindow(val activity: KorgwActivity, config: GameWindowCreationConfig = activity.config) : BaseAndroidGameWindow(activity, config) {
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

    fun initializeAndroid() {
        fullscreen = true
    }

    override fun setSize(width: Int, height: Int) {
    }
}

class AndroidGameWindowNoActivity(
    override val width: Int,
    override val height: Int,
    override val ag: AG,
    androidContext: Context,
    config: GameWindowCreationConfig = GameWindowCreationConfig(),
    val getView: () -> View
) : BaseAndroidGameWindow(androidContext, config) {
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
}

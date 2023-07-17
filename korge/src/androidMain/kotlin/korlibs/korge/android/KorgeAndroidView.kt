package korlibs.korge.android

import android.content.*
import android.util.*
import android.widget.*
import korlibs.event.*
import korlibs.graphics.gl.*
import korlibs.io.*
import korlibs.io.android.*
import korlibs.kgl.*
import korlibs.korge.*
import korlibs.render.*
import kotlinx.coroutines.*

@Suppress("unused")
open class KorgeAndroidView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    val config: GameWindowCreationConfig = GameWindowCreationConfig(),
) : RelativeLayout(context, attrs, defStyleAttr) {
    var mGLView: korlibs.render.KorgwSurfaceView? = null
    private var agOpenGl: AGOpengl? = null
    private var gameWindow: AndroidGameWindowNoActivity? = null

    private val renderEvent = RenderEvent()
    private val initEvent = InitEvent()

    var moduleLoaded = false ; private set

    fun unloadModule() {
        if (!moduleLoaded) return

        gameWindow?.dispatchDestroyEvent()
        gameWindow?.coroutineContext = null
        gameWindow?.close()
        gameWindow?.exit()
        mGLView = null
        gameWindow = null
        agOpenGl = null

        //findViewTreeLifecycleOwner()?.lifecycleScope?.launch { // @TODO: Not available in dependencies. Check if we can somehow get this other way.
        CoroutineScope(Dispatchers.Main).launch {
            mGLView?.let { removeView(it) }
        }

        moduleLoaded = false
    }

    fun loadModule(config: KorgeConfig) {
        println("KorgeAndroidView.loadModule[a]")
        unloadModule() // Unload module if already loaded

        agOpenGl = AGOpengl(KmlGlAndroid { mGLView?.clientVersion ?: -1 }.checkedIf(checked = false).logIf(log = false))
        gameWindow = AndroidGameWindowNoActivity(config.windowSize.width.toInt(), config.windowSize.height.toInt(), agOpenGl!!, context, this.config) { mGLView!! }
        mGLView = korlibs.render.KorgwSurfaceView(this, context, gameWindow!!, this.config)

        println("KorgeAndroidView.loadModule[b]")

        addView(mGLView)

        println("KorgeAndroidView.loadModule[b]")

        gameWindow?.let { gameWindow ->
            Korio(context) {
                println("KorgeAndroidView.loadModule[d]")

                try {
                    withAndroidContext(context) {
                        withContext(coroutineContext + gameWindow) {
                            println("KorgeAndroidView.loadModule[e]")
                            moduleLoaded = true
                            config.start()
                            println("KorgeAndroidView.loadModule[f]")
                        }
                    }
                } finally {
                    println("${javaClass.name} completed!")
                }
            }
        }
    }

    fun queueEvent(runnable: Runnable) {
        mGLView?.queueEvent(runnable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        unloadModule()
    }
}

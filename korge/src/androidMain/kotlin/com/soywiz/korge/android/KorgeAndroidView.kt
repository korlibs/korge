package com.soywiz.korge.android

import android.content.*
import android.util.*
import android.widget.*
import com.soywiz.kgl.*
import com.soywiz.korag.gl.*
import com.soywiz.korev.*
import com.soywiz.korge.*
import com.soywiz.korge.scene.*
import com.soywiz.korgw.*
import com.soywiz.korio.*
import com.soywiz.korio.android.*
import kotlinx.coroutines.*

@Suppress("unused")
open class KorgeAndroidView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    val config: GameWindowCreationConfig = GameWindowCreationConfig(),
) : RelativeLayout(context, attrs, defStyleAttr) {
    var mGLView: com.soywiz.korgw.KorgwSurfaceView? = null
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

    fun loadModule(module: Module) {
        loadModule(KorgeConfig(module))
    }

    fun loadModule(config: KorgeConfig) {
        unloadModule() // Unload module if already loaded

        agOpenGl = AGOpengl(KmlGlAndroid { mGLView?.clientVersion ?: -1 }.checkedIf(checked = false).logIf(log = false))
        gameWindow = AndroidGameWindowNoActivity(config.windowSize?.width ?: config.finalWindowSize.width,
            config.finalWindowSize.height, agOpenGl!!, context, this.config) { mGLView!! }
        mGLView = com.soywiz.korgw.KorgwSurfaceView(this, context, gameWindow!!, this.config)

        addView(mGLView)

        gameWindow?.let { gameWindow ->
            Korio(context) {
                try {
                    withAndroidContext(context) {
                        withContext(coroutineContext + gameWindow) {
                            config.start()
                        }
                    }
                } finally {
                    println("${javaClass.name} completed!")
                }
            }
        }

        moduleLoaded = true
    }

    fun queueEvent(runnable: Runnable) {
        mGLView?.queueEvent(runnable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        unloadModule()
    }
}

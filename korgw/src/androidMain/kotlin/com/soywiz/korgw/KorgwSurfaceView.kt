package com.soywiz.korgw

import android.app.*
import android.content.*
import android.content.pm.*
import android.opengl.*
import android.os.*
import android.util.*
import android.view.*
import android.opengl.EGL14.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import com.soywiz.klock.*
import com.soywiz.korev.*
import com.soywiz.korio.async.*

// https://github.com/aosp-mirror/platform_frameworks_base/blob/e4df5d375df945b0f53a9c7cca83d37970b7ce64/opengl/java/android/opengl/GLSurfaceView.java
class KorgwSurfaceView(val viewOrActivity: Any?, context: Context, val gameWindow: BaseAndroidGameWindow) : GLSurfaceView(context) {
    val view = this

    val onDraw = Signal<Unit>()
    val requestedClientVersion by lazy { getVersionFromPackageManager(context) }
    var clientVersion = -1

    init {
        println("KorgwActivity: Created GLSurfaceView $this for ${viewOrActivity}")

        println("OpenGL ES Version (requested): $requestedClientVersion")
        setEGLContextClientVersion(getVersionFromPackageManager(context))
        setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
                //GLES20.glClearColor(0.0f, 0.4f, 0.7f, 1.0f)
                gameWindow.handleContextLost()
                val out = IntArray(1)
                eglQueryContext(eglGetCurrentDisplay(), eglGetCurrentContext(), EGL_CONTEXT_CLIENT_VERSION, out, 0)
                clientVersion = out[0]
                println("OpenGL ES Version (actual): $clientVersion")
            }

            override fun onDrawFrame(unused: GL10) {
                gameWindow.handleInitEventIfRequired()
                gameWindow.handleReshapeEventIfRequired(0, 0, view.width, view.height)
                gameWindow.frame()
                onDraw(Unit)
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
        val gameWindow = gameWindow

        val actionMasked = ev.actionMasked
        val actionPointerIndex = ev.actionIndex

        if (ev.action != MotionEvent.ACTION_MOVE) {
            //println("[${DateTime.nowUnixLong()}]onTouchEvent: ${ev.action}, ${MotionEvent.actionToString(ev.action)}, actionMasked=$actionMasked, actionPointerId=$actionPointerId, ev.pointerCount=${ev.pointerCount}")
        }

        val type = when (actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> TouchEvent.Type.START
            MotionEvent.ACTION_HOVER_MOVE -> TouchEvent.Type.HOVER
            MotionEvent.ACTION_MOVE -> TouchEvent.Type.MOVE
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> TouchEvent.Type.END
            else -> TouchEvent.Type.UNKNOWN
        }
        touches.handleEvent(gameWindow, gameWindow.coroutineContext!!, type) { currentTouchEvent ->
            for (n in 0 until ev.pointerCount) {
                ev.getPointerCoords(n, coords)
                val id = ev.getPointerId(n)
                val status = when {
                    type == TouchEvent.Type.START && actionPointerIndex == n -> Touch.Status.ADD
                    type == TouchEvent.Type.END && actionPointerIndex == n -> Touch.Status.REMOVE
                    else -> Touch.Status.KEEP
                }
                currentTouchEvent.touch(id, coords.x.toDouble(), coords.y.toDouble(), status)
            }
        }
        return true
    }
}

private fun getVersionFromPackageManager(context: Context): Int {
    val packageManager = context.packageManager
    val featureInfos = packageManager.systemAvailableFeatures
    if (featureInfos != null && featureInfos.isNotEmpty()) {
        for (featureInfo in featureInfos) {
            // Null feature name means this feature is the open gl es version feature.
            if (featureInfo.name == null) {
                return when {
                    featureInfo.reqGlEsVersion != FeatureInfo.GL_ES_VERSION_UNDEFINED -> {
                        (featureInfo.reqGlEsVersion ushr 16) and 0xFF
                    }
                    else -> {
                        1 // Lack of property means OpenGL ES version 1
                    }
                }
            }
        }
    }
    return 1
}

package com.soywiz.korgw

import android.app.*
import android.content.*
import android.opengl.*
import android.os.*
import android.util.*
import android.view.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import com.soywiz.klock.*
import com.soywiz.korev.*

class KorgwSurfaceView(val viewOrActivity: Any?, context: Context, val gameWindow: BaseAndroidGameWindow) : GLSurfaceView(context) {
    val view = this

    init {
        println("KorgwActivity: Created GLSurfaceView $this for ${viewOrActivity}")

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
        val gameWindow = gameWindow

        val actionMasked = ev.actionMasked
        val actionPointerId = ev.actionIndex

        if (ev.action != MotionEvent.ACTION_MOVE) {
            //println("[${DateTime.nowUnixLong()}]onTouchEvent: ${ev.action}, ${MotionEvent.actionToString(ev.action)}, actionMasked=$actionMasked, actionPointerId=$actionPointerId, ev.pointerCount=${ev.pointerCount}")
        }

        touches.handleEvent(gameWindow, gameWindow.coroutineContext!!, when (actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> TouchEvent.Type.START
            MotionEvent.ACTION_HOVER_MOVE -> TouchEvent.Type.HOVER
            MotionEvent.ACTION_MOVE -> TouchEvent.Type.MOVE
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> TouchEvent.Type.END
            else -> TouchEvent.Type.UNKNOWN
        }, { currentTouchEvent ->
            for (n in 0 until ev.pointerCount) {
                ev.getPointerCoords(n, coords)
                currentTouchEvent.touch(ev.getPointerId(n), coords.x.toDouble(), coords.y.toDouble())
            }
        })
        return true
    }
}

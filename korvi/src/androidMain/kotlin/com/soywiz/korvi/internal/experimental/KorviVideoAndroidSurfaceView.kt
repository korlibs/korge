package com.soywiz.korvi.internal.experimental

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.media.MediaPlayer
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.soywiz.klock.*
import com.soywiz.klock.hr.*
import com.soywiz.korim.format.AndroidNativeImage
import com.soywiz.korio.file.VfsFile
import com.soywiz.korvi.KorviVideo
import com.soywiz.korvi.internal.createMediaPlayerFromSource
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.coroutines.CoroutineContext

class KorviVideoAndroidSurfaceView(val file: VfsFile, val androidContext: Context, val coroutineContext: CoroutineContext) : KorviVideo() {
    val videoSurfaceContainer = VideoSurfaceContainer(
        androidContext,
        file,
        invisible = false,
        callback = object : VideoSurfaceContainerCallback {
            override fun textureIdGenerated(texName: Int) {
                println("textureIdGenerated")
            }

            override fun surfaceTextureReady(surfaceTexture: SurfaceTexture) {
                println("surfaceTextureReady")
            }

            override fun onFrameAvailable(surface: SurfaceTexture) {
                println("onFrameAvailable")
            }

            override fun onFrameCaptured(bitmap: Bitmap) {
                println("onFrameCaptured")
                onVideoFrame(Frame(AndroidNativeImage(bitmap), 0.milliseconds.hr, 40.milliseconds.hr))
            }
        })
    init {
        //(androidContext as Activity).addContentView(videoSurfaceContainer, ViewGroup.LayoutParams(1, 1))
        (androidContext as Activity).addContentView(videoSurfaceContainer, ViewGroup.LayoutParams(100, 100))
        (videoSurfaceContainer as ViewGroup).removeView(videoSurfaceContainer)
    }

    override val running: Boolean
        get() = super.running
    override val elapsedTimeHr: HRTimeSpan
        get() = super.elapsedTimeHr

    override suspend fun getTotalFrames(): Long? {
        return super.getTotalFrames()
    }

    override suspend fun getDuration(): HRTimeSpan? {
        return super.getDuration()
    }

    override suspend fun play() {
        println("KorviVideoAndroidSurfaceView.play")
    }

    override suspend fun seek(frame: Long) {
        println("KorviVideoAndroidSurfaceView.seek")
    }

    override suspend fun seek(time: HRTimeSpan) {
        println("KorviVideoAndroidSurfaceView.seek")
    }

    override suspend fun stop() {
        println("KorviVideoAndroidSurfaceView.stop")
    }

    override suspend fun close() {
        println("KorviVideoAndroidSurfaceView.close")
    }
}

interface VideoSurfaceContainerCallback {

    fun textureIdGenerated(texName: Int)
    fun surfaceTextureReady(surfaceTexture: SurfaceTexture)
    fun onFrameAvailable(surface: SurfaceTexture)
    fun onFrameCaptured(bitmap: Bitmap)

}

@SuppressLint("ViewConstructor")
class VideoSurfaceContainer(
    context: Context,
    file: VfsFile,
    invisible: Boolean,
    callback: VideoSurfaceContainerCallback? = null
) : FrameLayout(context) {

    private var videoSurface: VideoSurfaceView

    init {
        setBackgroundColor(Color.BLUE)
        visibility = if (invisible) View.INVISIBLE else View.VISIBLE
        videoSurface =
            VideoSurfaceView(context, file, callback)
        addView(videoSurface)
    }

    fun captureFrame() {
        videoSurface.renderer.saveFrame = true
    }
}

@SuppressLint("ViewConstructor")
class VideoSurfaceView(
    context: Context,
    file: VfsFile,
    callback: VideoSurfaceContainerCallback? = null
) : GLSurfaceView(context) {

    companion object {
        private val TAG = VideoSurfaceView::class.java.toString()
        private const val FLOAT_SIZE_BYTES = 4
        private const val TRIANGLE_VERTICES_DATA_STRIDE_BYTES =
            5 * FLOAT_SIZE_BYTES
        private const val TRIANGLE_VERTICES_DATA_POS_OFFSET = 0
        private const val TRIANGLE_VERTICES_DATA_UV_OFFSET = 3
        private const val GL_TEXTURE_EXTERNAL_OES = 0x8D65
    }

    var renderer: VideoRenderer

    init {
        setEGLContextClientVersion(2)
        val mediaPlayer = runBlocking { createMediaPlayerFromSource(file, context) }
        renderer = VideoRenderer(callback, mediaPlayer)
        setRenderer(renderer)
    }

    @Suppress("SameParameterValue")
    inner class VideoRenderer(private val callback: VideoSurfaceContainerCallback? = null, private val mediaPlayer: MediaPlayer) : Renderer, OnFrameAvailableListener {

        private val mTriangleVerticesData = floatArrayOf(
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0f, 0f, 0f,
            1.0f, -1.0f, 0f, 1f, 0f,
            -1.0f, 1.0f, 0f, 0f, 1f,
            1.0f, 1.0f, 0f, 1f, 1f
        )

        private val mTriangleVertices: FloatBuffer

        private val mVertexShader = """
uniform mat4 uMVPMatrix;
uniform mat4 uSTMatrix;
attribute vec4 aPosition;
attribute vec4 aTextureCoord;
varying vec2 vTextureCoord;
void main() {
  gl_Position = uMVPMatrix * aPosition;
  vTextureCoord = (uSTMatrix * aTextureCoord).xy;
}
"""

        private val mFragmentShader =
            """
#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 vTextureCoord;
uniform samplerExternalOES sTexture;
void main() {
  gl_FragColor = texture2D(sTexture, vTextureCoord);
}
"""

        private var mPixelBuf: ByteBuffer? = null
        var saveFrame = false
        //var saveFrame = true

        private val mMVPMatrix = FloatArray(16)
        private val mSTMatrix = FloatArray(16)
        private var mProgram = 0
        private var mTextureID = 0
        private var muMVPMatrixHandle = 0
        private var muSTMatrixHandle = 0
        private var maPositionHandle = 0
        private var maTextureHandle = 0
        private var mSurface: SurfaceTexture? = null
        private var updateSurface = false

        init {
            mTriangleVertices =
                ByteBuffer.allocateDirect(mTriangleVerticesData.size * FLOAT_SIZE_BYTES)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer()
            mTriangleVertices.put(mTriangleVerticesData).position(0)
            Matrix.setIdentityM(mSTMatrix, 0)
        }

        override fun onDrawFrame(glUnused: GL10) {

            synchronized(this) {
                if (updateSurface) {
                    mSurface?.updateTexImage()
                    mSurface?.getTransformMatrix(mSTMatrix)
                    updateSurface = false
                }
            }

            GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f)
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glUseProgram(mProgram)
            checkGlError("glUseProgram")
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID)
            mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET)
            GLES20.glVertexAttribPointer(
                maPositionHandle, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices
            )
            checkGlError("glVertexAttribPointer maPosition")
            GLES20.glEnableVertexAttribArray(maPositionHandle)
            checkGlError("glEnableVertexAttribArray maPositionHandle")
            mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET)
            GLES20.glVertexAttribPointer(
                maTextureHandle, 3, GLES20.GL_FLOAT, false,
                TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices
            )
            checkGlError("glVertexAttribPointer maTextureHandle")
            GLES20.glEnableVertexAttribArray(maTextureHandle)
            checkGlError("glEnableVertexAttribArray maTextureHandle")
            Matrix.setIdentityM(mMVPMatrix, 0)
            GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0)
            GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
            checkGlError("glDrawArrays")

            saveFrame()
            /*
            if(saveFrame) {
                saveFrame()
                saveFrame = false
            }
             */

            GLES20.glFinish()
        }

        override fun onSurfaceChanged(
            glUnused: GL10,
            width: Int,
            height: Int
        ) {
        }

        override fun onSurfaceCreated(
            glUnused: GL10,
            config: EGLConfig
        ) {
            mProgram = createProgram(mVertexShader, mFragmentShader)
            if (mProgram == 0) {
                return
            }
            maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition")
            checkGlError("glGetAttribLocation aPosition")
            if (maPositionHandle == -1) {
                throw RuntimeException("Could not get attrib location for aPosition")
            }
            maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord")
            checkGlError("glGetAttribLocation aTextureCoord")
            if (maTextureHandle == -1) {
                throw RuntimeException("Could not get attrib location for aTextureCoord")
            }
            muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")
            checkGlError("glGetUniformLocation uMVPMatrix")
            if (muMVPMatrixHandle == -1) {
                throw RuntimeException("Could not get attrib location for uMVPMatrix")
            }
            muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix")
            checkGlError("glGetUniformLocation uSTMatrix")
            if (muSTMatrixHandle == -1) {
                throw RuntimeException("Could not get attrib location for uSTMatrix")
            }

            mPixelBuf = ByteBuffer.allocateDirect(width * height * 4)
            mPixelBuf?.order(ByteOrder.LITTLE_ENDIAN)

            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            mTextureID = textures[0]
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID)
            checkGlError("glBindTexture mTextureID")
            GLES20.glTexParameterf(
                GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST.toFloat()
            )
            GLES20.glTexParameterf(
                GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR.toFloat()
            )

            callback?.textureIdGenerated(mTextureID)

            /*
             * Create the SurfaceTexture that will feed this textureID,
             * and pass it to the MediaPlayer
             */
            mSurface = SurfaceTexture(mTextureID).apply {
                callback?.surfaceTextureReady(this)
            }

            mSurface?.setOnFrameAvailableListener(this)
            val surface = Surface(mSurface)
            mediaPlayer.setSurface(surface)
            mediaPlayer.setScreenOnWhilePlaying(true)
            surface.release()
            try {
                mediaPlayer.prepare()
            } catch (t: IOException) {
                Log.e(TAG, "media player prepare failed")
            }
            synchronized(this) { updateSurface = false }
            mediaPlayer.start()

        }

        @Synchronized
        override fun onFrameAvailable(surface: SurfaceTexture) {
            updateSurface = true
            callback?.onFrameAvailable(surface)
        }

        /**
         * Saves the current frame to disk as a PNG image.
         */
        @Throws(IOException::class)
        private fun saveFrame() {
            // glReadPixels gives us a ByteBuffer filled with what is essentially big-endian RGBA
            // data (i.e. a byte of red, followed by a byte of green...).  To use the Bitmap
            // constructor that takes an int[] array with pixel data, we need an int[] filled
            // with little-endian ARGB data.
            //
            // If we implement this as a series of buf.get() calls, we can spend 2.5 seconds just
            // copying data around for a 720p frame.  It's better to do a bulk get() and then
            // rearrange the data in memory.  (For comparison, the PNG compress takes about 500ms
            // for a trivial frame.)
            //
            // So... we set the ByteBuffer to little-endian, which should turn the bulk IntBuffer
            // get() into a straight memcpy on most Android devices.  Our ints will hold ABGR data.
            // Swapping B and R gives us ARGB.  We need about 30ms for the bulk get(), and another
            // 270ms for the color swap.
            //
            // We can avoid the costly B/R swap here if we do it in the fragment shader (see
            // http://stackoverflow.com/questions/21634450/ ).
            //
            // Having said all that... it turns out that the Bitmap#copyPixelsFromBuffer()
            // method wants RGBA pixels, not ARGB, so if we create an empty bitmap and then
            // copy pixel data in we can avoid the swap issue entirely, and just copy straight
            // into the Bitmap from the ByteBuffer.
            //
            // Making this even more interesting is the upside-down nature of GL, which means
            // our output will look upside-down relative to what appears on screen if the
            // typical GL conventions are used.  (For ExtractMpegFrameTest, we avoid the issue
            // by inverting the frame when we render it.)
            //
            // Allocating large buffers is expensive, so we really want mPixelBuf to be
            // allocated ahead of time if possible.  We still get some allocations from the
            // Bitmap / PNG creation.
            mPixelBuf?.rewind()
            GLES20.glReadPixels(
                0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                mPixelBuf
            )

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            mPixelBuf?.rewind()
            bitmap.copyPixelsFromBuffer(mPixelBuf)

            println(bitmap)
            callback?.onFrameCaptured(bitmap)

            Log.d(
                TAG,
                "Saved " + width.toString() + "x" + height.toString() + " frame"
            )
        }

        private fun loadShader(shaderType: Int, source: String): Int {
            var shader = GLES20.glCreateShader(shaderType)
            if (shader != 0) {
                GLES20.glShaderSource(shader, source)
                GLES20.glCompileShader(shader)
                val compiled = IntArray(1)
                GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
                if (compiled[0] == 0) {
                    Log.e(
                        TAG,
                        "Could not compile shader $shaderType:"
                    )
                    Log.e(TAG, GLES20.glGetShaderInfoLog(shader))
                    GLES20.glDeleteShader(shader)
                    shader = 0
                }
            }
            return shader
        }

        private fun createProgram(vertexSource: String, fragmentSource: String): Int {

            val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
            if (vertexShader == 0) {
                return 0
            }

            val pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
            if (pixelShader == 0) {
                return 0
            }

            var program = GLES20.glCreateProgram()
            if (program != 0) {
                GLES20.glAttachShader(program, vertexShader)
                checkGlError("glAttachShader")
                GLES20.glAttachShader(program, pixelShader)
                checkGlError("glAttachShader")
                GLES20.glLinkProgram(program)
                val linkStatus = IntArray(1)
                GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
                if (linkStatus[0] != GLES20.GL_TRUE) {
                    Log.e(TAG, "Could not link program: ")
                    Log.e(
                        TAG,
                        GLES20.glGetProgramInfoLog(program)
                    )
                    GLES20.glDeleteProgram(program)
                    program = 0
                }
            }
            return program
        }

        private fun checkGlError(op: String) {
            var error: Int
            while (GLES20.glGetError().also { error = it } != GLES20.GL_NO_ERROR) {
                Log.e(TAG, "$op: glError $error")
                throw RuntimeException("$op: glError $error")
            }
        }
    }
}


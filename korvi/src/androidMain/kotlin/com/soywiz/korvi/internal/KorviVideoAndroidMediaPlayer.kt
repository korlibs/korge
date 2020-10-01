package com.soywiz.korvi.internal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.SurfaceTexture
import android.media.MediaDataSource
import android.media.MediaPlayer
import android.opengl.GLES20
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Surface
import com.soywiz.klock.Frequency
import com.soywiz.klock.hr.HRTimeSpan
import com.soywiz.klock.hr.hr
import com.soywiz.klock.*
import com.soywiz.klock.timesPerSecond
import com.soywiz.korim.bitmap.NativeImage
import com.soywiz.korim.color.Colors
import com.soywiz.korim.color.RgbaArray
import com.soywiz.korio.android.androidContext
import com.soywiz.korio.async.launchImmediately
import com.soywiz.korio.file.VfsFile
import com.soywiz.korio.lang.Disposable
import com.soywiz.korvi.KorviVideo
import java.io.FileDescriptor
import java.lang.reflect.Field
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext


class AndroidKorviVideoAndroidMediaPlayer private constructor(val file: VfsFile, val androidContext: Context, val coroutineContext: CoroutineContext) : KorviVideo() {
    companion object {
        suspend operator fun invoke(file: VfsFile) = AndroidKorviVideoAndroidMediaPlayer(file, androidContext(), coroutineContext).also { it.init() }
    }

    lateinit var player: MediaPlayer
    lateinit var nativeImage: SurfaceNativeImage

    private var lastTimeSpan: HRTimeSpan = HRTimeSpan.ZERO
    private suspend fun init() {
        player = createMediaPlayerFromSource(file)
    }

    @Volatile
    private var frameAvailable = 0

    override fun prepare() {
        //val offsurface = OffscreenSurface(1024, 1024)
        //offsurface.makeCurrentTemporarily {
        //println("CREATING SURFACE")
        val info = SurfaceNativeImage.createSurfacePair()
        //println("SET SURFACE")
        player.setSurface(info.surface)
        //println("PREPARING")
        player.prepare()
        player.setOnCompletionListener {
            launchImmediately(coroutineContext) {
                onComplete(Unit)
            }
        }
        //println("CREATE SURFACE FOR VIDEO: ${player.videoWidth},${player.videoHeight}")
        nativeImage = SurfaceNativeImage(player.videoWidth, player.videoHeight, info)
        nativeImage.surfaceTexture.setOnFrameAvailableListener { frameAvailable++ }
    }

    private var lastUpdatedFrame = -1
    override fun render() {
        if (lastUpdatedFrame == frameAvailable) return
        //println("AndroidKorviVideoAndroidMediaPlayer.render! $frameAvailable")
        lastUpdatedFrame = frameAvailable
        val surfaceTexture = nativeImage.surfaceTexture
        surfaceTexture.updateTexImage()
        lastTimeSpan = surfaceTexture.timestamp.toDouble().nanoseconds.hr
        onVideoFrame(Frame(nativeImage, lastTimeSpan, frameRate.timeSpan.hr))
    }

    override val running: Boolean get() = player.isPlaying
    override val elapsedTimeHr: HRTimeSpan get() = lastTimeSpan

    // @TODO: We should try to get this
    val frameRate: Frequency = 25.timesPerSecond

    override suspend fun getTotalFrames(): Long? =
        getDuration()?.let { duration -> (duration / frameRate.timeSpan.hr).toLong() }

    override suspend fun getDuration(): HRTimeSpan? = player.duration.takeIf { it >= 0 }?.milliseconds?.hr

    override suspend fun play() {
        //println("START")
        player.start()
    }

    override suspend fun seek(frame: Long) {
        seek(frameRate.timeSpan.hr * frame.toDouble())
    }

    override suspend fun seek(time: HRTimeSpan) {
        lastTimeSpan = time
        player.seekTo(time.millisecondsInt)
    }

    override suspend fun stop() {
        player.stop()
    }

    override suspend fun close() {
        stop()
    }
}

data class SurfaceTextureInfo(val surface: Surface, val texture: SurfaceTexture, val texId: Int)

class SurfaceNativeImage(width: Int, height: Int, val info: SurfaceTextureInfo) :
    NativeImage(width, height, info, true), Disposable
{
    val surface get() = info.surface
    val surfaceTexture get() = info.texture

    override val forcedTexId: Int get() = info.texId
    override val forcedTexTarget: Int get() = GL_TEXTURE_EXTERNAL_OES

    companion object {
        const val GL_TEXTURE_EXTERNAL_OES = 0x8D65

        operator fun invoke(width: Int, height: Int): SurfaceNativeImage {
            val info = createSurfacePair()
            return SurfaceNativeImage(width, height, info)
        }

        fun createSurfacePair(): SurfaceTextureInfo {
            val textures = IntArray(1)
            GLES20.glGenTextures(1, textures, 0)
            val surfaceTexture = SurfaceTexture(textures[0])
            val surface = Surface(surfaceTexture)
            return SurfaceTextureInfo(surface, surfaceTexture, textures[0])
        }
    }

    override fun dispose() {
        surface.release()
        surfaceTexture.release()
    }

    // @TODO: Not required just for rendering, since ForcedTexId is set. But we might want to read its pixels at some point.
    override fun readPixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int) {
        TODO("Unsupported")
    }

    // @TODO: Not required just for rendering, since ForcedTexId is set. But we might want to read its pixels at some point.
    override fun writePixelsUnsafe(x: Int, y: Int, width: Int, height: Int, out: RgbaArray, offset: Int) {
        TODO("Unsupported")
    }
}

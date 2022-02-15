package com.soywiz.korvi.internal

import android.content.Context
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.video.VideoSize
import com.soywiz.klock.Frequency
import com.soywiz.klock.hr.HRTimeSpan
import com.soywiz.klock.hr.hr
import com.soywiz.klock.milliseconds
import com.soywiz.klock.nanoseconds
import com.soywiz.klock.timesPerSecond
import com.soywiz.korma.geom.*
import com.soywiz.korvi.KorviVideo
import kotlinx.coroutines.*


class AndroidKorviVideoAndroidExoPlayer(context: Context) : KorviVideo() {

    private val player = SimpleExoPlayer.Builder(context).build()
    lateinit var nativeImage: SurfaceNativeImage

    private var lastTimeSpan: HRTimeSpan = HRTimeSpan.ZERO
    private var transformMat = FloatArray(16) { 0.0f }

    private val mainScope = CoroutineScope(Dispatchers.Main + Job())

    private var currentSeek = -1L
    private var pendingSeek = -1L

    fun setVolume(volume: Float) {
        player.volume = volume
    }

    fun setSpeed(volume: Float) {
        val param = PlaybackParameters(volume)
        player.playbackParameters = param
    }

    fun setMedia(mediaItem: MediaItem) {
        player.clearVideoSurface()
        player.setMediaItem(mediaItem)
    }

    fun clearVideoSurface() = player.clearVideoSurface()

    fun getCurrentMediaCount() = player.mediaItemCount

    @Volatile
    private var frameAvailable = 0

    override fun prepare() {
        val info = SurfaceNativeImage.createSurfacePair()
        // println("SET SURFACE")
        info.texture.setOnFrameAvailableListener {
            // println("frame available: $frameAvailable")
            frameAvailable++
        }

        mainScope.launch {
            //val offsurface = OffscreenSurface(1024, 1024)
            //offsurface.makeCurrentTemporarily {
            player.setVideoSurface(info.surface)
            val param = PlaybackParameters(1f)
            player.playbackParameters = param

            player.prepare()
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    super.onPlaybackStateChanged(state)
                    if(state == ExoPlayer.STATE_READY) {
                        if (pendingSeek != currentSeek && pendingSeek != -1L) {
                            mainScope.launch {
                                currentSeek = -1L
                                seek(
                                    HRTimeSpan.fromMilliseconds(pendingSeek.toDouble())
                                )
                            }
                        }
                        else {
                            currentSeek = -1L
                        }
                    }
                }

                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    super.onVideoSizeChanged(videoSize)
                    println("CREATE SURFACE FOR VIDEO: ${videoSize.width},${videoSize.height}")
                    nativeImage = SurfaceNativeImage(videoSize.width, videoSize.height, info)
                }
            })

        }
    }

    private var lastUpdatedFrame = -1
    private val matrix = Matrix3D()

    override fun render() {
        if (lastUpdatedFrame == frameAvailable) return
        try {
//            println("AndroidKorviVideoAndroidExoPlayer.render! $frameAvailable")
            val surfaceTexture = nativeImage.surfaceTexture
            lastUpdatedFrame = frameAvailable
            surfaceTexture.updateTexImage()
            surfaceTexture.getTransformMatrix(transformMat)

            matrix.setColumns4x4(transformMat, 0)
            matrix.translate(0.0, +0.5, 0.0)
            matrix.scale(1.0, -1.0, 1.0)
            matrix.translate(0.0, -0.5, 0.0)
            matrix.copyToFloat4x4(transformMat, MajorOrder.COLUMN)

            nativeImage.transformMat.setColumns4x4(transformMat, 0)
            lastTimeSpan = surfaceTexture.timestamp.toDouble().nanoseconds.hr
            onVideoFrame(Frame(nativeImage, lastTimeSpan, frameRate.timeSpan.hr))
        } catch (e: Exception) {
            System.err.println(e.message)
        }
    }

    override val running: Boolean get() = player.isPlaying ?: false
    override val elapsedTimeHr: HRTimeSpan get() = lastTimeSpan

    // @TODO: We should try to get this
    val frameRate: Frequency = 25.timesPerSecond

    override suspend fun getTotalFrames(): Long? =
        getDuration()?.let { duration -> (duration / frameRate.timeSpan.hr).toLong() }

    override suspend fun getDuration(): HRTimeSpan? =
        player.duration.takeIf { it != null && it >= 0 }?.milliseconds?.hr

    override suspend fun play() {
        //println("START")
        withContext(Dispatchers.Main) {
            player.play()
            println("Duration:" + getDuration()?.secondsInt)

        }
    }

    override suspend fun pause() {
        super.pause()
        withContext(Dispatchers.Main) {
            player.pause()
            println("Duration:" + getDuration()?.secondsInt)

        }

    }

    override suspend fun seek(frame: Long) {
        println(frameRate.timeSpan.hr)
        seek(frameRate.timeSpan.hr * frame.toDouble())
    }

    override suspend fun seek(time: HRTimeSpan) {
        lastTimeSpan = time
        withContext(Dispatchers.Main) {
            //Todo seek through multiple media files
//            player.seekTo(windowIndex, seekPos.toLong())
            if (currentSeek == -1L) {
                currentSeek = time.millisecondsInt.toLong()
                pendingSeek = -1L
                player.seekTo(currentSeek)
            } else {
                pendingSeek = time.millisecondsInt.toLong()
            }
        }
    }

    override suspend fun stop() {
        close()
    }

    override suspend fun close() = withContext(Dispatchers.Main) {
        nativeImage.dispose()
        player.stop()
        player.release()
        mainScope.cancel()
    }
}

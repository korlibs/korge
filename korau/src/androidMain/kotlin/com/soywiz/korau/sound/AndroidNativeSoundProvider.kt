package com.soywiz.korau.sound

import android.annotation.*
import android.content.*
import android.media.*
import android.media.AudioFormat
import android.os.*
import com.soywiz.kds.*
import com.soywiz.klock.*
import com.soywiz.klogger.*
import com.soywiz.korau.format.*
import com.soywiz.korau.format.mp3.*
import com.soywiz.korio.android.*
import com.soywiz.korio.async.*
import kotlin.coroutines.*

actual val nativeSoundProvider: NativeSoundProvider by lazy { AndroidNativeSoundProvider() }

class AndroidNativeSoundProvider : NativeSoundProvider() {
    companion object {
        val MAX_CHANNELS = 16
    }

	override val target: String = "android"

    override val audioFormats: AudioFormats = AudioFormats(MP3Decoder) + defaultAudioFormats

    private var audioManager: AudioManager? = null
    val audioSessionId by lazy { audioManager!!.generateAudioSessionId() }
    //val audioSessionId get() = audioManager!!.generateAudioSessionId()

    private val threadPool = Pool {
        com.soywiz.klogger.Console.info("Creating AudioThread[$it]")
        AudioThread(this).also { it.isDaemon = true }.also { it.start() }
    }

    class AudioThread(val provider: AndroidNativeSoundProvider, var freq: Int = 44100) : Thread() {
        var props: SoundProps = DummySoundProps
        val deque = AudioSamplesDeque(2)
        @Volatile
        var running = true

        override fun run() {
            val bufferSamples = 4096

            val at = AudioTrack(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    //.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                    .build(),
                AudioFormat.Builder()
                    .setChannelMask(AudioFormat.CHANNEL_IN_STEREO)
                    .setSampleRate(freq)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build(),
                2 * 2 * bufferSamples,
                AudioTrack.MODE_STREAM,
                provider.audioSessionId
            )
            if (at.getState() == AudioTrack.STATE_INITIALIZED) {
                at.play()
            }
            try {
                val temp = AudioSamplesInterleaved(2, bufferSamples)
                while (running) {
                    val readCount = deque.read(temp)
                    if (readCount > 0) {
                        //println("AUDIO CHUNK: $readCount : ${temp.data.toList()}")
                        if (at.getState() == AudioTrack.STATE_INITIALIZED) {
                            at.setPlaybackRate(freq)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                at.playbackParams.speed = props.pitch.toFloat()
                            }
                            at.setVolume(props.volume.toFloat())
                            at.write(temp.data, 0, readCount * 2, AudioTrack.WRITE_BLOCKING)
                        }
                    } else {
                        Thread.sleep(10L)
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            } finally {
                try {
                    at.stop()
                    at.release()
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun ensureAudioManager(coroutineContext: CoroutineContext) {
        if (audioManager == null) {
            val ctx = coroutineContext[AndroidCoroutineContext.Key]?.context ?: error("Can't find the Android Context on the CoroutineContext. Must call withAndroidContext first")
            audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun createAudioStream(coroutineContext: CoroutineContext, freq: Int): PlatformAudioOutput {
        ensureAudioManager(coroutineContext)

		return object : PlatformAudioOutput(coroutineContext, freq) {
            private var started = false
            private var thread: AudioThread? = null
            private val threadDeque get() = thread?.deque

            override val availableSamples: Int get() = threadDeque?.availableRead ?: 0

            override suspend fun add(samples: AudioSamples, offset: Int, size: Int) {
                while (thread == null) delay(10.milliseconds)
                threadDeque!!.write(samples, offset, size)
            }

            override fun start(): Unit {
                if (started) return
                started = true
                launchImmediately(coroutineContext) {
                    while (threadPool.totalItemsInUse >= MAX_CHANNELS) {
                        delay(10.milliseconds)
                    }
                    thread = threadPool.alloc()
                    thread?.props = this
                    thread?.freq = freq
                }
            }
            override fun stop() {
                if (!started) return
                started = false
                if (thread != null) {
                    threadPool.free(thread!!)
                }
                thread = null
            }
        }
	}

    /*
    override suspend fun createNonStreamingSound(data: AudioData, name: String): Sound {
        ensureAudioManager(coroutineContext)
        val audioManager = this.audioManager!!
        val ctx = coroutineContext
        return object : Sound(ctx) {
            val sound = this

            override fun play(coroutineContext: CoroutineContext, params: PlaybackParameters): SoundChannel {
                val samplesData = data.samplesInterleaved.data
                val totalShorts = data.totalSamples * data.channels

                val at = AudioTrack(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN).build(),
                    AudioFormat.Builder().setChannelMask(AudioFormat.CHANNEL_IN_STEREO).setSampleRate(data.rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build(),
                    totalShorts * 2,
                    AudioTrack.MODE_STATIC,
                    audioSessionId
                )

                at.write(samplesData, 0, totalShorts, AudioTrack.WRITE_NON_BLOCKING)
                //at.write(samplesData, 0, totalShorts, AudioTrack.WRITE_BLOCKING)
                //at.write(data.samplesInterleaved.data, 0, data.totalSamples * data.channels)
                //println("AudioTrack.PLAY")

                return object : SoundChannel(sound) {
                    fun setProps() {
                        at.setVolume(volume.toFloat())
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            at.playbackParams.speed = pitch.toFloat()
                        }
                    }

                    init {
                        launchImmediately(coroutineContext) {
                            while (at.getState() != AudioTrack.STATE_INITIALIZED) {
                                delay(1.milliseconds)
                            }
                            setProps()
                            at.play()
                            try {
                                while (at.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
                                    setProps()
                                    delay(64.milliseconds)
                                }
                            } finally {
                                //println("AudioTrack.STOP+RELEASE")
                                at.stop()
                                at.release()
                            }
                        }
                    }

                    override fun resume() {
                        at.play()
                    }

                    override fun pause() {
                        at.pause()
                    }

                    override fun stop() {
                        at.stop()
                    }
                }.apply {
                    this.copySoundPropsFrom(params)
                }
            }

            override suspend fun decode(): AudioData = data
        }
    }
     */
}

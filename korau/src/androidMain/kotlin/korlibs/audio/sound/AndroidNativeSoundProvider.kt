package korlibs.audio.sound

import android.content.*
import android.media.*
import android.os.*
import korlibs.datastructure.*
import korlibs.io.android.*
import korlibs.io.async.*
import korlibs.time.*
import kotlin.coroutines.*
import kotlin.coroutines.cancellation.*

actual val nativeSoundProvider: NativeSoundProvider by lazy { AndroidNativeSoundProvider() }

class AndroidNativeSoundProvider : NativeSoundProvider() {
    companion object {
        val MAX_CHANNELS = 16
    }

    override val target: String = "android"

    private var audioManager: AudioManager? = null
    val audioSessionId: Int by lazy {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
            audioManager!!.generateAudioSessionId() else -1
    }
    //val audioSessionId get() = audioManager!!.generateAudioSessionId()

    //private val activeOutputs = LinkedHashSet<AndroidPlatformAudioOutput>()
    private val threadPool = Pool { id ->
        //Console.info("Creating AudioThread[$id]")
        AudioThread(this, id = id).also { it.isDaemon = true }.also { it.start() }
    }

    override var paused: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                //(this as java.lang.Object).notifyAll()
            }
        }

    class AudioThread(val provider: AndroidNativeSoundProvider, var freq: Int = 44100, val id: Int = -1) : Thread() {
        var props: SoundProps = DummySoundProps
        val deque = AudioSamplesDeque(2)
        @Volatile
        var running = true

        init {
            this.isDaemon = true
        }

        override fun run() {
            val bufferSamples = 4096

            val at = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                AudioTrack(
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
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    freq,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    2 * 2 * bufferSamples,
                    AudioTrack.MODE_STREAM
                )
            }
            if (at.state == AudioTrack.STATE_UNINITIALIZED) {
                System.err.println("Audio track was not initialized correctly freq=$freq, bufferSamples=$bufferSamples")
            }
            //if (at.state == AudioTrack.STATE_INITIALIZED) at.play()
            while (running) {
                try {
                    val temp = AudioSamplesInterleaved(2, bufferSamples)
                    //val tempEmpty = ShortArray(1024)
                    var paused = true
                    var lastVolume = Float.NaN
                    while (running) {
                        if (provider.paused) {
                            at.stop()
                            //at.pause()
                            //at.flush()
                            while (provider.paused && running) {
                                //(provider as java.lang.Object).wait(10_000L)
                                Thread.sleep(250L)
                            }
                            at.play()
                        }

                        val readCount = deque.read(temp)
                        if (at.state == AudioTrack.STATE_UNINITIALIZED) {
                            Thread.sleep(50L)
                            continue
                        }
                        if (readCount > 0) {
                            if (paused) {
                                //println("[KORAU] Resume $id")
                                paused = false
                                at.play()
                            }
                            //println("AUDIO CHUNK: $readCount : ${temp.data.toList()}")
                            if (at.state == AudioTrack.STATE_INITIALIZED) {
                                at.playbackRate = freq
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    at.playbackParams.speed = props.pitch.toFloat()
                                }
                                val vol = props.volume.toFloat()
                                if (lastVolume != vol) {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                        at.setVolume(vol)
                                    } else {
                                        @Suppress("DEPRECATION")
                                        at.setStereoVolume(vol, vol)
                                    }
                                }
                                lastVolume = vol
                                at.write(temp.data, 0, readCount * 2)
                            }
                        } else {
                            //at.write(tempEmpty, 0, tempEmpty.size)
                            if (!paused) {
                                //println("[KORAU] Stop $id")
                                //at.flush()
                                at.stop()
                                paused = true
                            }
                            Thread.sleep(2L)
                        }
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                } finally {
                    //println("[KORAU] Completed $id")
                    try {
                        at.stop()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }
            at.release()
        }
    }

    fun ensureAudioManager(coroutineContext: CoroutineContext) {
        if (audioManager == null) {
            val ctx = coroutineContext[AndroidCoroutineContext.Key]?.context ?: error("Can't find the Android Context on the CoroutineContext. Must call withAndroidContext first")
            audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }
    }

    override fun createPlatformAudioOutput(coroutineContext: CoroutineContext, freq: Int): PlatformAudioOutput {
        ensureAudioManager(coroutineContext)
        return AndroidPlatformAudioOutput(coroutineContext, freq, this)
    }

    class AndroidPlatformAudioOutput(coroutineContext: CoroutineContext, frequency: Int, val provider: AndroidNativeSoundProvider) : PlatformAudioOutput(coroutineContext, frequency) {
        private var started = false
        internal var thread: AudioThread? = null
        private val threadDeque get() = thread?.deque

        override val availableSamples: Int get() = threadDeque?.availableRead ?: 0

        override suspend fun add(samples: AudioSamples, offset: Int, size: Int) {
            while (thread == null) delay(10.milliseconds)
            while (threadDeque!!.availableRead >= 44100) delay(1.milliseconds)
            threadDeque!!.write(samples, offset, size)
        }

        override fun start() {
            if (started) return
            started = true
            launchImmediately(coroutineContext) {
                while (provider.threadPool.totalItemsInUse >= MAX_CHANNELS) {
                    delay(10.milliseconds)
                }
                thread = provider.threadPool.alloc()
                thread?.props = this
                thread?.freq = frequency
                threadDeque?.clear()
            }
            //provider.activeOutputs += this
        }

        override fun stop() {
            if (!started) return
            //provider.activeOutputs -= this
            started = false
            if (thread != null) {
                provider.threadPool.free(thread!!)
            }
            thread = null
        }
    }
}

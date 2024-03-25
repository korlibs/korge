package korlibs.audio.sound

import android.content.*
import android.media.*
import android.os.*
import korlibs.datastructure.pauseable.*
import korlibs.datastructure.thread.*
import korlibs.io.android.*
import kotlin.coroutines.*

actual val nativeSoundProvider: NativeSoundProvider by lazy { AndroidNativeSoundProvider() }

class AndroidNativeSoundProvider : NativeSoundProviderNew() {
    override val target: String = "android"

    private var audioManager: AudioManager? = null
    val audioSessionId: Int by lazy {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
            audioManager!!.generateAudioSessionId() else -1
    }

    override fun createNewPlatformAudioOutput(coroutineContext: CoroutineContext, channels: Int, frequency: Int, gen: (AudioSamplesInterleaved) -> Unit): NewPlatformAudioOutput {
        ensureAudioManager(coroutineContext)
        return AndroidNewPlatformAudioOutput(this, coroutineContext, channels, frequency, gen)
    }

    private val pauseable = SyncPauseable()
    override var paused: Boolean by pauseable::paused

    fun ensureAudioManager(coroutineContext: CoroutineContext) {
        if (audioManager == null) {
            val ctx = coroutineContext[AndroidCoroutineContext.Key]?.context ?: error("Can't find the Android Context on the CoroutineContext. Must call withAndroidContext first")
            audioManager = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }
    }

    class AndroidNewPlatformAudioOutput(
        val provider: AndroidNativeSoundProvider,
        coroutineContext: CoroutineContext,
        channels: Int,
        frequency: Int,
        gen: (AudioSamplesInterleaved) -> Unit
    ) : NewPlatformAudioOutput(coroutineContext, channels, frequency, gen) {
        var thread: NativeThread? = null

        override fun internalStart() {
            thread = nativeThread(isDaemon = true) { thread ->
                //val bufferSamples = 4096
                val bufferSamples = 1024

                val atChannelSize = Short.SIZE_BYTES * channels * bufferSamples
                val atChannel = if (channels >= 2) AudioFormat.CHANNEL_OUT_STEREO else AudioFormat.CHANNEL_OUT_MONO
                val atMode = AudioTrack.MODE_STREAM
                val at = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    AudioTrack(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            //.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                            .build(),
                        AudioFormat.Builder()
                            .setChannelMask(atChannel)
                            .setSampleRate(frequency)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .build(),
                        atChannelSize,
                        atMode,
                        provider.audioSessionId
                    )
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        frequency,
                        atChannel,
                        AudioFormat.ENCODING_PCM_16BIT,
                        atChannelSize,
                        atMode
                    )
                }
                if (at.state == AudioTrack.STATE_UNINITIALIZED) {
                    System.err.println("Audio track was not initialized correctly frequency=$frequency, bufferSamples=$bufferSamples")
                }

                val buffer = AudioSamplesInterleaved(channels, bufferSamples)
                at.play()

                var lastVolL = Float.NaN
                var lastVolR = Float.NaN

                try {
                    while (thread.threadSuggestRunning) {
                        provider.pauseable.checkPaused()

                        if (this.paused) {
                            at.pause()
                            Thread.sleep(20L)
                            continue
                        } else {
                            at.play()
                        }

                        when (at.state) {
                            AudioTrack.STATE_UNINITIALIZED -> {
                                Thread.sleep(20L)
                            }
                            AudioTrack.STATE_INITIALIZED -> {
                                at.playbackRate = frequency
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    at.playbackParams.speed = this.pitch.toFloat()
                                }
                                val volL = this.volumeForChannel(0).toFloat()
                                val volR = this.volumeForChannel(1).toFloat()
                                if (lastVolL != volL || lastVolR != volR) {
                                    lastVolL = volL
                                    lastVolR = volR
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                                        at.setVolume(volL)
                                    } else {
                                        at.setStereoVolume(volL, volR)
                                    }
                                }

                                genSafe(buffer)
                                at.write(buffer.data, 0, buffer.data.size)
                            }
                        }
                    }
                } finally {
                    at.flush()
                    at.stop()
                    at.release()
                }

                //val temp = AudioSamplesInterleaved(2, bufferSamples)
            }
        }

        override fun internalStop() {
            thread?.threadSuggestRunning = false
            thread = null
        }
    }
}

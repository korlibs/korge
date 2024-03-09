package korlibs.audio.sound.backend

import korlibs.audio.sound.*
import korlibs.datastructure.thread.*
import korlibs.ffi.*
import korlibs.io.lang.*
import kotlin.coroutines.*

object FFIALSANativeSoundProvider : NativeSoundProvider() {
    override fun createNewPlatformAudioOutput(coroutineContext: CoroutineContext, channels: Int, frequency: Int, gen: (AudioSamplesInterleaved) -> Unit): NewPlatformAudioOutput {
        //println("ALSANativeSoundProvider.createPlatformAudioOutput(freq=$freq)")
        return ALSAPlatformAudioOutput(this, coroutineContext, channels, frequency, gen)
    }
}

class ALSAPlatformAudioOutput(
    val soundProvider: FFIALSANativeSoundProvider,
    coroutineContext: CoroutineContext,
    channels: Int,
    frequency: Int,
    gen: (AudioSamplesInterleaved) -> Unit,
) : NewPlatformAudioOutput(coroutineContext, channels, frequency, gen) {
    //var nativeThread: Job? = null
    var nativeThread: NativeThread? = null

    override fun internalStart() {
        //nativeThread = launchImmediately(coroutineContext) {
        nativeThread = nativeThread(isDaemon = true) { thread ->
            val buffer = AudioSamplesInterleaved(channels, 1024)
            val pcm = A2.snd_pcm_open("default", A2.SND_PCM_STREAM_PLAYBACK, 0)
            if (pcm.address == 0L) {
                error("Can't initialize ALSA")
                //running = false
                //return@nativeThread
            }

            //val latency = 8 * 4096
            val latency = 32 * 4096
            A2.snd_pcm_set_params(
                pcm,
                A2.SND_PCM_FORMAT_S16_LE,
                A2.SND_PCM_ACCESS_RW_INTERLEAVED,
                channels,
                frequency,
                1,
                latency
            )
            try {
                while (thread.threadSuggestRunning) {
                    genSafe(buffer)
                    val written = A2.snd_pcm_writei(pcm, buffer.data, 0, buffer.totalSamples * channels, buffer.totalSamples)
                    //println("offset=$offset, pending=$pending, written=$written")
                    if (written == -A2.EPIPE) {
                        //println("ALSA: EPIPE error")
                        //A2.snd_pcm_prepare(pcm)
                        A2.snd_pcm_recover(pcm, written, 0)
                        continue
                        //blockingSleep(1.milliseconds)
                    } else if (written < 0) {
                        println("ALSA: OTHER error: $written")
                        //delay(1.milliseconds)
                        Thread_sleep(1L)
                        break
                    }
                }
            } finally {
                //println("!!COMPLETED : pcm=$pcm")
                A2.snd_pcm_wait(pcm, 1000)
                A2.snd_pcm_drain(pcm)
                A2.snd_pcm_close(pcm)
                //println("!!CLOSED = $pcm")
            }
        }
    }

    override fun internalStop() {
        nativeThread?.threadSuggestRunning = false
        nativeThread = null
    }
}

object A2 : FFILib("libasound.so.2"){
    fun snd_pcm_open(name: String, stream: Int, mode: Int): FFIPointer? {
        val ptrs = FFIPointerArray(1)
        if (snd_pcm_open(ptrs, name, stream ,mode) != 0) return null
        return ptrs[0]
    }

    val snd_pcm_open: (pcmPtr: FFIPointerArray, name: String?, stream: Int, mode: Int) -> Int by func()
    val snd_pcm_hw_params_any: (pcm: FFIPointer?, params: FFIPointer?) -> Int by func()
    val snd_pcm_hw_params_set_access: (pcm: FFIPointer?, params: FFIPointer?, access: Int) -> Int by func()
    val snd_pcm_hw_params_set_format: (pcm: FFIPointer?, params: FFIPointer?, format: Int) -> Int by func()
    val snd_pcm_hw_params_set_channels: (pcm: FFIPointer?, params: FFIPointer?, channels: Int) -> Int by func()
    val snd_pcm_hw_params_set_rate: (pcm: FFIPointer?, params: FFIPointer?, rate: Int, dir: Int) -> Int by func()
    val snd_pcm_hw_params: (pcm: FFIPointer?, params: FFIPointer?) -> Int by func()
    val snd_pcm_name: (pcm: FFIPointer?) -> String? by func()
    val snd_pcm_state: (pcm: FFIPointer?) -> Int by func()
    val snd_pcm_state_name: (state: Int) -> String? by func()
    val snd_pcm_hw_params_get_channels: (params: FFIPointer?, out: FFIPointer?) -> Int by func()
    val snd_pcm_hw_params_get_rate: (params: FFIPointer?, value: FFIPointer?, dir: FFIPointer?) -> Int by func()
    val snd_pcm_hw_params_get_period_size: (params: FFIPointer?, value: FFIPointer?, dir: FFIPointer?) -> Int by func()
    val snd_pcm_hw_params_get_period_time: (params: FFIPointer?, value: FFIPointer?, dir: FFIPointer?) -> Int by func()

    fun snd_pcm_writei(pcm: FFIPointer?, buffer: ShortArray, offset: Int, size: Int, nframes: Int): Int {
        //println("PCM=$pcm, buffer=$buffer, offset=$offset, size=$size")
        //if (size == 0) return 0
        return snd_pcm_writei(pcm, buffer.copyOfRange(offset, offset + size), nframes)
        //val mem = Memory((buffer.size * 2).toLong()).also { it.clear() }
        //for (n in 0 until size) mem.setShort((n * 2).toLong(), buffer[offset + n])
        ////A2.snd_pcm_wait(pcm.toCPointer(), 100)
        //return A2.snd_pcm_writei(pcm.toCPointer(), mem, nframes)
    }

    val snd_pcm_writei: (pcm: FFIPointer?, buffer: ShortArray, size: Int) -> Int by func()
    val snd_pcm_prepare: (pcm: FFIPointer?) -> Int by func()
    val snd_pcm_drain: (pcm: FFIPointer?) -> Int by func()
    val snd_pcm_drop: (pcm: FFIPointer?) -> Int by func()
    val snd_pcm_delay: (pcm: FFIPointer?, delay: FFIPointer?) -> Int by func()
    val snd_pcm_close: (pcm: FFIPointer?) -> Int by func()
    val snd_pcm_set_params: (
        pcm: FFIPointer?,
        format: Int,
        access: Int,
        channels: Int,
        rate: Int,
        softResample: Int,
        latency: Int
    ) -> Int by func()

    val snd_pcm_wait: (pcm: FFIPointer?, timeout: Int) -> Int by func()
    val snd_pcm_recover: (pcm: FFIPointer?, err: Int, silent: Int) -> Int by func()

    const val ERROR = -1

    const val EPIPE = 32	// Broken pipe
    const val EBADFD = 77	// File descriptor in bad state
    const val ESTRPIPE = 86	// Streams pipe error

    const val SND_PCM_STREAM_PLAYBACK = 0
    const val SND_PCM_STREAM_CAPTURE = 1

    const val SND_PCM_ACCESS_MMAP_INTERLEAVED = 0 // mmap access with simple interleaved channels
    const val SND_PCM_ACCESS_MMAP_NONINTERLEAVED = 1 // mmap access with simple non interleaved channels
    const val SND_PCM_ACCESS_MMAP_COMPLEX = 2 // mmap access with complex placement
    const val SND_PCM_ACCESS_RW_INTERLEAVED = 3 // snd_pcm_readi/snd_pcm_writei access
    const val SND_PCM_ACCESS_RW_NONINTERLEAVED = 4 // /snd_pcm_writen access

    const val SND_PCM_FORMAT_S16_LE = 2

    const val SND_PCM_STATE_OPEN = 0 // Open
    const val SND_PCM_STATE_SETUP = 1 // Setup installed
    const val SND_PCM_STATE_PREPARED = 2 // Ready to start
    const val SND_PCM_STATE_RUNNING = 3 // Running
    const val SND_PCM_STATE_XRUN = 4 // Stopped: underrun (playback) or overrun (capture) detected
    const val SND_PCM_STATE_DRAINING = 5 // Draining: running (playback) or stopped (capture)
    const val SND_PCM_STATE_PAUSED = 6 // Paused
    const val SND_PCM_STATE_SUSPENDED = 7 // Hardware is suspended
    const val SND_PCM_STATE_DISCONNECTED = 8 // Hardware is disconnected

    val SND_PCM_NONBLOCK: Int get() = TODO()
}

/*

object ALSAExample {
    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            val sp = ALSANativeSoundProvider()
            //val sp = JnaOpenALNativeSoundProvider()
            val job1 = launch(coroutineContext) {
                //sp.playAndWait(AudioTone.generate(10.seconds, 400.0).toStream())
                sp.playAndWait(resourcesVfs["Snowland.mp3"].readMusic().toStream())
            }
            val job2 = launch(coroutineContext) {
                //sp.playAndWait(AudioTone.generate(10.seconds, 200.0).toStream())
            }
            println("Waiting...")
            job1.join()
            job2.join()
            println("Done")
        }
    }
}

class ALSANativeSoundProvider : NativeSoundProvider() {
    override fun createPlatformAudioOutput(coroutineContext: CoroutineContext, freq: Int): PlatformAudioOutput {
        return ALSAPlatformAudioOutput(this, coroutineContext, freq)
    }
}

class ALSAPlatformAudioOutput(
    val soundProvider: ALSANativeSoundProvider,
    coroutineContext: CoroutineContext,
    frequency: Int,
) : PlatformAudioOutput(coroutineContext, frequency) {
    val channels = 2
    val cmpPtr = Memory(1024L).also { it.clear() }
    val params = Memory(1024L).also { it.clear() }
    val temp = Memory(1024L).also { it.clear() }
    var pcm: Pointer? = Pointer.NULL
    private val lock = Lock()
    val sdeque = AudioSamplesDeque(channels)
    var running = true
    var thread: Thread? = null

    init {
        start()
    }

    override suspend fun add(samples: AudioSamples, offset: Int, size: Int) {
        if (!ASound2.initialized) return super.add(samples, offset, size)

        while (running && lock { sdeque.availableRead > 4 * 1024 }) {
            delay(10.milliseconds)
        }
        lock { sdeque.write(samples, offset, size) }
    }

    override fun start() {
        sdeque.clear()
        running = true

        if (!ASound2.initialized) return

        //cmpPtr.clear()
        //cmpPtr.setLong(0L, 0L)
        //println("test")
        ASound2.snd_pcm_open(cmpPtr, "default", ASound2.SND_PCM_STREAM_PLAYBACK, 0).also {
            if (it != 0) error("Can't initialize ALSA")
        }
        pcm = cmpPtr.getPointer(0L)
        //println("pcm=$pcm")
        ASound2.snd_pcm_hw_params_any(pcm, params)
        ASound2.snd_pcm_hw_params_set_access(pcm, params, ASound2.SND_PCM_ACCESS_RW_INTERLEAVED).also {
            if (it != 0) error("Error calling snd_pcm_hw_params_set_access=$it")
        }
        ASound2.snd_pcm_hw_params_set_format(pcm, params, ASound2.SND_PCM_FORMAT_S16_LE).also {
            if (it != 0) error("Error calling snd_pcm_hw_params_set_format=$it")
        }
        ASound2.snd_pcm_hw_params_set_channels(pcm, params, channels).also {
            if (it != 0) error("Error calling snd_pcm_hw_params_set_channels=$it")
        }
        ASound2.snd_pcm_hw_params_set_rate(pcm, params, frequency, +1).also {
            if (it != 0) error("Error calling snd_pcm_hw_params_set_rate=$it")
        }
        ASound2.snd_pcm_hw_params(pcm, params).also {
            if (it != 0) error("Error calling snd_pcm_hw_params=$it")
        }

        //println(ASound2.snd_pcm_name(pcm))
        //println(ASound2.snd_pcm_state_name(ASound2.snd_pcm_state(pcm)))
        ASound2.snd_pcm_hw_params_get_channels(params, temp).also {
            if (it != 0) error("Error calling snd_pcm_hw_params_get_channels=$it")
        }
        val cchannels = temp.getInt(0L)
        ASound2.snd_pcm_hw_params_get_rate(params, temp, null).also { if (it != 0) error("Error calling snd_pcm_hw_params_get_rate=$it") }
        val crate = temp.getInt(0L)
        ASound2.snd_pcm_hw_params_get_period_size(params, temp, null).also { if (it != 0) error("Error calling snd_pcm_hw_params_get_period_size=$it") }
        val frames = temp.getInt(0L)
        //println("cchannels: $cchannels, rate=$crate, frames=$frames")
        val buff = Memory((frames * channels * 2).toLong()).also { it.clear() }
        ASound2.snd_pcm_hw_params_get_period_time(params, temp, null).also { if (it != 0) error("Error calling snd_pcm_hw_params_get_period_size=$it") }
        //val random = Random(0L)
        thread = Thread {
            val samples = AudioSamplesInterleaved(channels, frames)
            try {
                mainLoop@ while (running) {
                    while (lock { sdeque.availableRead < frames }) {
                        if (!running) break@mainLoop
                        Thread.sleep(1L)
                    }
                    val readCount = lock { sdeque.read(samples, 0, frames) }
                    //println("readCount=$readCount")
                    val panning = this.panning.toFloat()
                    //val panning = -1f
                    //val panning = +0f
                    //val panning = +1f
                    val volume = this.volume.toFloat().clamp01()
                    for (ch in 0 until channels) {
                        val pan = (if (ch == 0) -panning else +panning) + 1f
                        val npan = pan.clamp01()
                        val rscale: Float = npan * volume
                        //println("panning=$panning, volume=$volume, pan=$pan, npan=$npan, rscale=$rscale")
                        for (n in 0 until readCount) {
                            buff.setShort(
                                ((n * channels + ch) * 2).toLong(),
                                (samples[ch, n] * rscale).toInt().toShort()
                            )
                        }
                    }
                    val result = ASound2.snd_pcm_writei(pcm, buff, frames)
                    //println("result=$result")
                    if (result == -ASound2.EPIPE) {
                        ASound2.snd_pcm_prepare(pcm)
                    }
                }
            } catch (e: InterruptedException) {
                // Done
            }
        }.also {
            it.isDaemon = true
            it.start()
        }
    }

    override fun stop() {
        running = false
        thread?.interrupt()
        if (!ASound2.initialized) return

        ASound2.snd_pcm_drain(pcm)
        ASound2.snd_pcm_close(pcm)
    }
}

object AlsaTest {
    @JvmStatic fun main(args: Array<String>) {
        /*
        val data = AudioTone.generate(1.seconds, 400.0)

        var nn = 0
        while (true) {
            for (n in 0 until frames * channels) {
                val value = data[0, nn]
                buff.setShort((n * 2).toLong(), value)
                nn++
                if (nn >= data.totalSamples) nn = 0
            }
            val result = ASound2.snd_pcm_writei(pcm, buff, frames)
            println("result=$result")
            if (result == -ASound2.EPIPE) {
                ASound2.snd_pcm_prepare(pcm)
            }
        }
        */

    }
}

@Keep object ASound2 {
    var initialized = false

    @JvmStatic external fun snd_pcm_open(pcmPtr: Pointer?, name: String, stream: Int, mode: Int): Int
    @JvmStatic external fun snd_pcm_hw_params_any(pcm: Pointer?, params: Pointer): Int
    @JvmStatic external fun snd_pcm_hw_params_set_access(pcm: Pointer?, params: Pointer, access: Int): Int
    @JvmStatic external fun snd_pcm_hw_params_set_format(pcm: Pointer?, params: Pointer, format: Int): Int
    @JvmStatic external fun snd_pcm_hw_params_set_channels(pcm: Pointer?, params: Pointer, channels: Int): Int
    @JvmStatic external fun snd_pcm_hw_params_set_rate(pcm: Pointer?, params: Pointer, rate: Int, dir: Int): Int
    @JvmStatic external fun snd_pcm_hw_params(pcm: Pointer?, params: Pointer): Int
    @JvmStatic external fun snd_pcm_name(pcm: Pointer?): String
    @JvmStatic external fun snd_pcm_state(pcm: Pointer?): Int
    @JvmStatic external fun snd_pcm_state_name(state: Int): String
    @JvmStatic external fun snd_pcm_hw_params_get_channels(params: Pointer, out: Pointer): Int
    @JvmStatic external fun snd_pcm_hw_params_get_rate(params: Pointer?, value: Pointer?, dir: Pointer?): Int
    @JvmStatic external fun snd_pcm_hw_params_get_period_size(params: Pointer?, value: Pointer?, dir: Pointer?): Int
    @JvmStatic external fun snd_pcm_hw_params_get_period_time(params: Pointer?, value: Pointer?, dir: Pointer?): Int
    @JvmStatic external fun snd_pcm_writei(pcm: Pointer?, buffer: Pointer, size: Int): Int
    @JvmStatic external fun snd_pcm_prepare(pcm: Pointer?): Int
    @JvmStatic external fun snd_pcm_drain(pcm: Pointer?): Int
    @JvmStatic external fun snd_pcm_close(pcm: Pointer?): Int

    const val EPIPE = 32	// Broken pipe
    const val EBADFD = 77	// File descriptor in bad state
    const val ESTRPIPE = 86	// Streams pipe error

    const val SND_PCM_STREAM_PLAYBACK = 0
    const val SND_PCM_STREAM_CAPTURE = 1

    const val SND_PCM_ACCESS_MMAP_INTERLEAVED = 0 // mmap access with simple interleaved channels
    const val SND_PCM_ACCESS_MMAP_NONINTERLEAVED = 1 // mmap access with simple non interleaved channels
    const val SND_PCM_ACCESS_MMAP_COMPLEX = 2 // mmap access with complex placement
    const val SND_PCM_ACCESS_RW_INTERLEAVED = 3 // snd_pcm_readi/snd_pcm_writei access
    const val SND_PCM_ACCESS_RW_NONINTERLEAVED = 4 // /snd_pcm_writen access

    const val SND_PCM_FORMAT_S16_LE = 2

    const val SND_PCM_STATE_OPEN = 0 // Open
    const val SND_PCM_STATE_SETUP = 1 // Setup installed
    const val SND_PCM_STATE_PREPARED = 2 // Ready to start
    const val SND_PCM_STATE_RUNNING = 3 // Running
    const val SND_PCM_STATE_XRUN = 4 // Stopped: underrun (playback) or overrun (capture) detected
    const val SND_PCM_STATE_DRAINING = 5 // Draining: running (playback) or stopped (capture)
    const val SND_PCM_STATE_PAUSED = 6 // Paused
    const val SND_PCM_STATE_SUSPENDED = 7 // Hardware is suspended
    const val SND_PCM_STATE_DISCONNECTED = 8 // Hardware is disconnected

    init {
        try {
            Native.register("libasound.so.2")
            initialized = true
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }
}

/*
➜  korge git:(main) ✗ cat ~/alsatest.c
/*
 * Simple sound playback using ALSA API and libasound.
 *
 * Compile:
 * $ cc -o play sound_playback.c -lasound
 *
 * Usage:
 * $ ./play <sample_rate> <channels> <seconds> < <file>
 *
 * Examples:
 * $ ./play 44100 2 5 < /dev/urandom
 * $ ./play 22050 1 8 < /path/to/file.wav
 *
 * Copyright (C) 2009 Alessandro Ghedini <al3xbio@gmail.com>
 * --------------------------------------------------------------
 * "THE BEER-WARE LICENSE" (Revision 42):
 * Alessandro Ghedini wrote this file. As long as you retain this
 * notice you can do whatever you want with this stuff. If we
 * meet some day, and you think this stuff is worth it, you can
 * buy me a beer in return.
 * --------------------------------------------------------------
 */

#include <alsa/asoundlib.h>
#include <stdio.h>

#define PCM_DEVICE "default"

int main(int argc, char **argv) {
        unsigned int pcm, tmp, dir;
        int rate, channels, seconds;
        snd_pcm_t *pcm_handle;
        snd_pcm_hw_params_t *params;
        snd_pcm_uframes_t frames;
        char *buff;
        int buff_size, loops;

        if (argc < 4) {
                printf("Usage: %s <sample_rate> <channels> <seconds>\n",
                                                                argv[0]);
                return -1;
        }

        rate     = atoi(argv[1]);
        channels = atoi(argv[2]);
        seconds  = atoi(argv[3]);

        /* Open the PCM device in playback mode */
        if (pcm = snd_pcm_open(&pcm_handle, PCM_DEVICE,
                                        SND_PCM_STREAM_PLAYBACK, 0) < 0)
                printf("ERROR: Can't open \"%s\" PCM device. %s\n",
                                        PCM_DEVICE, snd_strerror(pcm));

        /* Allocate parameters object and fill it with default values*/
        snd_pcm_hw_params_alloca(&params);

        snd_pcm_hw_params_any(pcm_handle, params);

        /* Set parameters */
        if (pcm = snd_pcm_hw_params_set_access(pcm_handle, params,
                                        SND_PCM_ACCESS_RW_INTERLEAVED) < 0)
                printf("ERROR: Can't set interleaved mode. %s\n", snd_strerror(pcm));

        if (pcm = snd_pcm_hw_params_set_format(pcm_handle, params,
                                                SND_PCM_FORMAT_S16_LE) < 0)
                printf("ERROR: Can't set format. %s\n", snd_strerror(pcm));

        if (pcm = snd_pcm_hw_params_set_channels(pcm_handle, params, channels) < 0)
                printf("ERROR: Can't set channels number. %s\n", snd_strerror(pcm));

        if (pcm = snd_pcm_hw_params_set_rate_near(pcm_handle, params, &rate, 0) < 0)
                printf("ERROR: Can't set rate. %s\n", snd_strerror(pcm));

        /* Write parameters */
        if (pcm = snd_pcm_hw_params(pcm_handle, params) < 0)
                printf("ERROR: Can't set harware parameters. %s\n", snd_strerror(pcm));

        /* Resume information */
        printf("PCM name: '%s'\n", snd_pcm_name(pcm_handle));

        printf("PCM state: %s\n", snd_pcm_state_name(snd_pcm_state(pcm_handle)));

        snd_pcm_hw_params_get_channels(params, &tmp);
        printf("channels: %i ", tmp);

        if (tmp == 1)
                printf("(mono)\n");
        else if (tmp == 2)
                printf("(stereo)\n");

        snd_pcm_hw_params_get_rate(params, &tmp, 0);
        printf("rate: %d bps\n", tmp);

        printf("seconds: %d\n", seconds);

        /* Allocate buffer to hold single period */
        snd_pcm_hw_params_get_period_size(params, &frames, 0);

        buff_size = frames * channels * 2 /* 2 -> sample size */;
        buff = (char *) malloc(buff_size);

        snd_pcm_hw_params_get_period_time(params, &tmp, NULL);

        for (loops = (seconds * 1000000) / tmp; loops > 0; loops--) {

                if (pcm = read(0, buff, buff_size) == 0) {
                        printf("Early end of file.\n");
                        return 0;
                }

                if (pcm = snd_pcm_writei(pcm_handle, buff, frames) == -EPIPE) {
                        printf("XRUN.\n");
                        snd_pcm_prepare(pcm_handle);
                } else if (pcm < 0) {
                        printf("ERROR. Can't write to PCM device. %s\n", snd_strerror(pcm));
                }

        }

        snd_pcm_drain(pcm_handle);
        snd_pcm_close(pcm_handle);
        free(buff);

        return 0;
}%
 */

 */

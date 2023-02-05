package com.soywiz.korau.sound.impl.alsa

import com.soywiz.klock.*
import com.soywiz.korau.sound.*
import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import java.util.Random

object AlsaTest {
    @JvmStatic fun main(args: Array<String>) {
        val cmpPtr = Memory(1024L).also { it.clear() }
        val params = Memory(1024L).also { it.clear() }
        val temp = Memory(1024L).also { it.clear() }

        val channels = 2
        val rate = 44100

        //cmpPtr.clear()
        //cmpPtr.setLong(0L, 0L)
        println("test")
        ASound2.snd_pcm_open(cmpPtr, "default", ASound2.SND_PCM_STREAM_PLAYBACK, 0).also {
            if (it != 0) error("Can't initialize ALSA")
        }
        val pcm = cmpPtr.getPointer(0L)
        println("pcm=$pcm")
        println(ASound2.snd_pcm_hw_params_any(pcm, params))
        ASound2.snd_pcm_hw_params_set_access(pcm, params, ASound2.SND_PCM_ACCESS_RW_INTERLEAVED).also {
            if (it != 0) error("Error calling snd_pcm_hw_params_set_access=$it")
        }
        ASound2.snd_pcm_hw_params_set_format(pcm, params, ASound2.SND_PCM_FORMAT_S16_LE).also {
            if (it != 0) error("Error calling snd_pcm_hw_params_set_format=$it")
        }
        ASound2.snd_pcm_hw_params_set_channels(pcm, params, channels).also {
            if (it != 0) error("Error calling snd_pcm_hw_params_set_channels=$it")
        }
        ASound2.snd_pcm_hw_params_set_rate(pcm, params, rate, +1).also {
            if (it != 0) error("Error calling snd_pcm_hw_params_set_rate=$it")
        }
        ASound2.snd_pcm_hw_params(pcm, params).also {
            if (it != 0) error("Error calling snd_pcm_hw_params=$it")
        }

        println(ASound2.snd_pcm_name(pcm))
        println(ASound2.snd_pcm_state_name(ASound2.snd_pcm_state(pcm)))
        ASound2.snd_pcm_hw_params_get_channels(params, temp).also {
            if (it != 0) error("Error calling snd_pcm_hw_params_get_channels=$it")
        }
        val cchannels = temp.getInt(0L)
        ASound2.snd_pcm_hw_params_get_rate(params, temp, null).also { if (it != 0) error("Error calling snd_pcm_hw_params_get_rate=$it") }
        val crate = temp.getInt(0L)
        ASound2.snd_pcm_hw_params_get_period_size(params, temp, null).also { if (it != 0) error("Error calling snd_pcm_hw_params_get_period_size=$it") }
        val frames = temp.getInt(0L)
        println("cchannels: $cchannels, rate=$crate, frames=$frames")
        val buff = Memory((frames * channels * 2).toLong()).also { it.clear() }
        ASound2.snd_pcm_hw_params_get_period_time(params, temp, null).also { if (it != 0) error("Error calling snd_pcm_hw_params_get_period_size=$it") }
        //val random = Random(0L)

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

        ASound2.snd_pcm_drain(pcm)
        ASound2.snd_pcm_close(pcm)
    }
}

object ASound2 {
    @JvmStatic external fun snd_pcm_open(pcmp: Pointer, name: String, stream: Int, mode: Int): Int
    @JvmStatic external fun snd_pcm_hw_params_any(pcmp: Pointer, params: Pointer): Int
    @JvmStatic external fun snd_pcm_hw_params_set_access(pcmp: Pointer, params: Pointer, access: Int): Int
    @JvmStatic external fun snd_pcm_hw_params_set_format(pcmp: Pointer, params: Pointer, format: Int): Int
    @JvmStatic external fun snd_pcm_hw_params_set_channels(pcmp: Pointer, params: Pointer, channels: Int): Int
    @JvmStatic external fun snd_pcm_hw_params_set_rate(pcmp: Pointer, params: Pointer, rate: Int, dir: Int): Int
    @JvmStatic external fun snd_pcm_hw_params(pcmp: Pointer, params: Pointer): Int
    @JvmStatic external fun snd_pcm_name(pcmp: Pointer): String
    @JvmStatic external fun snd_pcm_state(pcm: Pointer): Int
    @JvmStatic external fun snd_pcm_state_name(state: Int): String
    @JvmStatic external fun snd_pcm_hw_params_get_channels(params: Pointer, out: Pointer): Int
    @JvmStatic external fun snd_pcm_hw_params_get_rate(params: Pointer?, value: Pointer?, dir: Pointer?): Int
    @JvmStatic external fun snd_pcm_hw_params_get_period_size(params: Pointer?, value: Pointer?, dir: Pointer?): Int
    @JvmStatic external fun snd_pcm_hw_params_get_period_time(params: Pointer?, value: Pointer?, dir: Pointer?): Int
    @JvmStatic external fun snd_pcm_writei(pcm: Pointer, buffer: Pointer, size: Int): Int
    @JvmStatic external fun snd_pcm_prepare(pcm: Pointer): Int
    @JvmStatic external fun snd_pcm_drain(pcm: Pointer): Int
    @JvmStatic external fun snd_pcm_close(pcm: Pointer): Int

    const val EPIPE = 32	/* Broken pipe */
    const val EBADFD = 77	/* File descriptor in bad state */
    const val ESTRPIPE = 86	/* Streams pipe error */


    const val SND_PCM_STREAM_PLAYBACK = 0
    const val SND_PCM_STREAM_CAPTURE = 1


    /** mmap access with simple interleaved channels */
    const val SND_PCM_ACCESS_MMAP_INTERLEAVED = 0
    /** mmap access with simple non interleaved channels */
    const val SND_PCM_ACCESS_MMAP_NONINTERLEAVED = 1
    /** mmap access with complex placement */
    const val SND_PCM_ACCESS_MMAP_COMPLEX = 2
    /** snd_pcm_readi/snd_pcm_writei access */
    const val SND_PCM_ACCESS_RW_INTERLEAVED = 3
    /** snd_pcm_readn/snd_pcm_writen access */
    const val SND_PCM_ACCESS_RW_NONINTERLEAVED = 4

    const val SND_PCM_FORMAT_S16_LE = 2

    /** Open */
    const val SND_PCM_STATE_OPEN = 0
    /** Setup installed */
    const val SND_PCM_STATE_SETUP = 1
    /** Ready to start */
    const val SND_PCM_STATE_PREPARED = 2
    /** Running */
    const val SND_PCM_STATE_RUNNING = 3
    /** Stopped: underrun (playback) or overrun (capture) detected */
    const val SND_PCM_STATE_XRUN = 4
    /** Draining: running (playback) or stopped (capture) */
    const val SND_PCM_STATE_DRAINING = 5
    /** Paused */
    const val SND_PCM_STATE_PAUSED = 6
    /** Hardware is suspended */
    const val SND_PCM_STATE_SUSPENDED = 7
    /** Hardware is disconnected */
    const val SND_PCM_STATE_DISCONNECTED = 8


    init {
        Native.register("libasound.so.2")
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

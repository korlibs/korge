package com.soywiz.korau.format.mp3

import com.soywiz.korau.format.AudioFormat
import com.soywiz.korau.sound.NativeMp3DecoderAudioFormat

actual val FastMP3Decoder: AudioFormat get() = NativeMp3DecoderAudioFormat

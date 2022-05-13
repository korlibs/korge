package com.soywiz.korau.sound

import com.soywiz.korau.format.AudioFormat

@kotlin.native.concurrent.ThreadLocal
expect val knNativeAudioFormats: List<AudioFormat>

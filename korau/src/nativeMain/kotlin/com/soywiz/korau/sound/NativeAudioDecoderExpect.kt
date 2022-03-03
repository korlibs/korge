package com.soywiz.korau.sound

import com.soywiz.korau.format.*

@kotlin.native.concurrent.ThreadLocal
expect val knNativeAudioFormats: List<AudioFormat>

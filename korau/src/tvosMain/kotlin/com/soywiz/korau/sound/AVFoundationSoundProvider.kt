package com.soywiz.korau.sound

import com.soywiz.korau.format.*
import kotlin.coroutines.*

actual val nativeSoundProvider: NativeSoundProvider get() = DummyNativeSoundProvider

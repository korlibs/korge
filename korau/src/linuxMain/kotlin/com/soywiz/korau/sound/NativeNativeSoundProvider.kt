package com.soywiz.korau.sound

import com.soywiz.korau.sound.backends.*

actual val nativeSoundProvider: NativeSoundProvider by lazy {
    try {
        (null as? NativeSoundProvider?)
            ?: alsaNativeSoundProvider
            ?: openalNativeSoundProvider
            ?: DummyNativeSoundProvider
    } catch (e: Throwable) {
        e.printStackTrace()
        DummyNativeSoundProvider
    }
}

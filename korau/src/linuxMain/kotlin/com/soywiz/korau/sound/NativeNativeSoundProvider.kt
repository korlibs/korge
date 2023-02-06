package com.soywiz.korau.sound

import com.soywiz.korau.sound.backends.*

actual val nativeSoundProvider: NativeSoundProvider by lazy {
    (null as? NativeSoundProvider?)
        ?: alsaNativeSoundProvider
        ?: openalNativeSoundProvider
        ?: DummyNativeSoundProvider
}

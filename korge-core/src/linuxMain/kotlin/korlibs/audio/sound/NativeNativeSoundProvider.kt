package korlibs.audio.sound

import korlibs.audio.sound.backends.*

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

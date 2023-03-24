package korlibs.audio.sound

import korlibs.audio.format.*
import kotlin.coroutines.*

actual val nativeSoundProvider: NativeSoundProvider get() = DummyNativeSoundProvider
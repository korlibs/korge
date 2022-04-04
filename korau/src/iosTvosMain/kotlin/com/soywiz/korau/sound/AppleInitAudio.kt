package com.soywiz.korau.sound

import kotlinx.cinterop.*
import platform.AVFAudio.*
import platform.Foundation.*

fun appleInitAudio() {
    memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>().ptr
        AVAudioSession.sharedInstance().setCategory(AVAudioSessionCategoryPlayback, error)
        AVAudioSession.sharedInstance().setActive(true, error)
    }
}

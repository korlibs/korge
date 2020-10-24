package com.soywiz.korau.sound

interface AudioStreamable {
    suspend fun toStream(): AudioStream
}

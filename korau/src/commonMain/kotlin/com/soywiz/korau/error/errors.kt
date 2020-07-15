package com.soywiz.korau.error

class SeekingNotSupported(message: String = "seekingNotSupported") : Exception(message)
fun seekingNotSupported(message: String = "seekingNotSupported"): Nothing = throw SeekingNotSupported(message)

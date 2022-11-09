package com.soywiz.korge.view.animation

interface PlayableWithName {
    val animationName: String? get() = null
    fun play(name: String?): Unit = Unit
    fun stop(): Unit = Unit
}

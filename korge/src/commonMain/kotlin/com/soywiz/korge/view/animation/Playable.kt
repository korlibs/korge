package com.soywiz.korge.view.animation

interface Playable {
    fun play(): Unit
    fun stop(): Unit
    fun rewind(): Unit = Unit
}

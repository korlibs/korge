package com.soywiz.korau.sound.compat

import com.soywiz.korau.sound.PlaybackParameters
import com.soywiz.korau.sound.Sound
import com.soywiz.korau.sound.SoundChannel
import kotlin.coroutines.CoroutineContext

@Deprecated("Use play with a coroutineContext or in a suspend function")
fun Sound.play(params: PlaybackParameters = PlaybackParameters.DEFAULT, coroutineContext: CoroutineContext = defaultCoroutineContext, noSuspend: Boolean): SoundChannel = play(coroutineContext, params)

@Deprecated("Use play with a coroutineContext or in a suspend function")
fun Sound.playOld(params: PlaybackParameters = PlaybackParameters.DEFAULT, coroutineContext: CoroutineContext = defaultCoroutineContext): SoundChannel = play(coroutineContext, params)

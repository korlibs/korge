package com.soywiz.korui

import korlibs.io.*
import kotlinx.coroutines.*

expect val KoruiDispatcher: CoroutineDispatcher

open class KoruiContext

fun Korui(entry: suspend (KoruiContext) -> Unit) = Korio {
    KoruiWrap { entry(it) }
}

internal expect suspend fun KoruiWrap(entry: suspend (KoruiContext) -> Unit)
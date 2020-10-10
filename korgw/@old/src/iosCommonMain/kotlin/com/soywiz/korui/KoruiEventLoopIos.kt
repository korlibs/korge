package com.soywiz.korui

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual val KoruiDispatcher: CoroutineDispatcher get() = Dispatchers.Main

internal actual suspend fun KoruiWrap(entry: suspend (KoruiContext) -> Unit) {
    entry(KoruiContext())
}

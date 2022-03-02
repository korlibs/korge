package com.soywiz.korio.async

import kotlinx.coroutines.*
import platform.posix.*

actual fun asyncEntryPoint(callback: suspend () -> Unit) = runBlocking {
    setlocale(LC_ALL, ".UTF-8")
    callback()
}

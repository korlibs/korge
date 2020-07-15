package com.soywiz.korio.async

import kotlin.coroutines.*
import kotlinx.coroutines.*

actual fun asyncEntryPoint(callback: suspend () -> Unit) = runBlocking { callback() }

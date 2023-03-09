package com.soywiz.korio.async

import kotlinx.coroutines.*

expect val Dispatchers.CIO: CoroutineDispatcher
expect val Dispatchers.ResourceDecoder: CoroutineDispatcher

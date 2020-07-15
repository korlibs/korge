package com.soywiz.korio.lang

import com.soywiz.korio.*

expect val currentThreadId: Long

expect fun Thread_sleep(time: Long): Unit

package com.soywiz.korio.lang

expect val currentThreadId: Long

expect fun Thread_sleep(time: Long): Unit

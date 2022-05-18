package com.soywiz.korio.lang

import kotlinx.cinterop.convert

actual fun Thread_sleep(time: Long) {
	platform.posix.usleep((time * 1000L).convert())
}

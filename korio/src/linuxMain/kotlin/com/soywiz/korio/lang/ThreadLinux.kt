package com.soywiz.korio.lang

import kotlinx.cinterop.convert
import platform.linux.__NR_gettid
import platform.posix.syscall

actual val currentThreadId: Long get() = syscall(__NR_gettid.convert()).toLong()

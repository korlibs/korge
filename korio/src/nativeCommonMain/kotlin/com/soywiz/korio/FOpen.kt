package com.soywiz.korio

import kotlinx.cinterop.*

expect fun posixFopen(filename: String, mode: String): CPointer<platform.posix.FILE>?

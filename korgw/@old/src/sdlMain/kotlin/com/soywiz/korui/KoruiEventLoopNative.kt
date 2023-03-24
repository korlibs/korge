package com.soywiz.korui

import korlibs.io.async.*
import com.soywiz.korui.light.*
import korlibs.io.*

actual object KoruiEventLoop {
	actual fun create(): EventLoop = BaseEventLoopNative()
}
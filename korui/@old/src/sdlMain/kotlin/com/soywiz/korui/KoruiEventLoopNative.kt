package com.soywiz.korui

import com.soywiz.korio.async.*
import com.soywiz.korui.light.*
import com.soywiz.korio.*

actual object KoruiEventLoop {
	actual fun create(): EventLoop = BaseEventLoopNative()
}
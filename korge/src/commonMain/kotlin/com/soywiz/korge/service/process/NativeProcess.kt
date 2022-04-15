package com.soywiz.korge.service.process

import com.soywiz.korge.view.*
import com.soywiz.korgw.*
import com.soywiz.korio.net.*

class NativeProcess(views: Views) : NativeProcessBase(views) {
}

open class NativeProcessBase(val views: Views) : DialogInterfaceProvider by views.gameWindow {
}

package com.soywiz.korge.service.process

import com.soywiz.korge.view.*
import com.soywiz.korio.net.*

class NativeProcess(views: Views) : NativeProcessBase(views) {
}

open class NativeProcessBase(val views: Views) {
    open suspend fun alert(message: String) = views.gameWindow.alert(message)
    open suspend fun confirm(message: String): Boolean = views.gameWindow.confirm(message)
    open suspend fun openFileDialog(filter: String? = null, write: Boolean = false, multi: Boolean = false) = views.gameWindow.openFileDialog(filter, write, multi)
    open suspend fun browse(url: URL) = views.gameWindow.browse(url)
    open suspend fun close() = views.gameWindow.close()
}

package com.soywiz.kds.thread

expect class NativeThread(code: () -> Unit) {
    var isDaemon: Boolean
    fun start(): Unit
    fun interrupt(): Unit
}

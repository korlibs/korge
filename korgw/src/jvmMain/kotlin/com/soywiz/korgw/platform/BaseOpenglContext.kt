package com.soywiz.korgw.platform

import com.soywiz.korio.lang.*

interface BaseOpenglContext : Disposable {
    val scaleFactor: Double get() = 1.0
    fun useContext(obj: Any?, action: Runnable) {
        makeCurrent()
        try {
            action.run()
        } finally {
            swapBuffers()
            releaseCurrent()
        }
    }

    fun makeCurrent()
    fun releaseCurrent() {
    }
    fun swapBuffers() {
    }
    fun supportsSwapInterval(): Boolean = false
    fun swapInterval(value: Int) {
    }

    override fun dispose() = Unit
}

object DummyOpenglContext : BaseOpenglContext {
    override fun makeCurrent() {
    }

    override fun swapBuffers() {
    }
}

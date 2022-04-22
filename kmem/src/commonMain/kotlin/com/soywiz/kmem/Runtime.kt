package com.soywiz.kmem

import com.soywiz.kmem.internal.currentRuntime

enum class Runtime {
    JS, JVM, ANDROID, NATIVE;

    val isJs: Boolean get() = this == JS
    val isJvm: Boolean get() = this == JVM
    val isAndroid: Boolean get() = this == ANDROID
    val isNative: Boolean get() = this == NATIVE
    val isJvmOrAndroid: Boolean get() = isJvm || isAndroid

    companion object {
        val CURRENT: Runtime get() = currentRuntime
    }
}

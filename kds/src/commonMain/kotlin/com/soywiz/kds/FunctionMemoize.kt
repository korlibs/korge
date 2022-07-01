package com.soywiz.kds

fun <T : Any> (() -> T).memoize(): (() -> T) {
    val func = this
    var cached: T? = null
    return {
        if (cached == null) cached = func()
        cached!!
    }
}

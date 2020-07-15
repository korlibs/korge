package com.soywiz.korev.internal

internal inline fun <T> List<T>.fastForEach(callback: (T) -> Unit) {
    var n = 0
    while (n < size) {
        callback(this[n++])
    }
}

